package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.commands.Root
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.service.PermissionService
import de.bigboot.ggtools.fang.utils.formatCommandHelp
import de.bigboot.ggtools.fang.utils.parseArgs
import de.bigboot.ggtools.fang.utils.print
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Status
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.discordjson.json.ActivityUpdateRequest
import discord4j.discordjson.json.gateway.StatusUpdate
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.tinylog.Logger
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max

class Fang(private val client: GatewayDiscordClient) : KoinComponent {
    private val botId: Snowflake
    private val permissionService: PermissionService by inject()
    private val matchService: MatchService by inject()

    private val updateStatusTimer = Timer(true)

    private var commands = CommandGroupBuilder("", "").apply(Root().build).build()

    init {
        updateStatusTimer.schedule(object : TimerTask() {
            override fun run() {
                updateStatus()
            }
        }, 0, Config.STATUSUPDATE_POLL_RATE)

        botId = runBlocking {
            client.selfId.awaitSingle()
        }

        registerEventListeners()
    }

    private fun registerEventListeners() {
        client.eventDispatcher.on(MessageCreateEvent::class.java)
            .filter { it.message.content.startsWith(Config.PREFIX) }
            .flatMap { event ->
                handleCommandEvent(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        Logger.warn(error)
                        Mono.empty()
                    }
            }
            .subscribe()

        client.eventDispatcher.on(VoiceStateUpdateEvent::class.java)
            .filter { !it.old.isPresent }
            .filterWhen {
                mono {
                    it.current.channel.awaitFirstOrNull()?.name == "Match Hub"
                }
            }
            .flatMap { event ->
                handleMatchHubJoined(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        Logger.warn(error)
                        Mono.empty()
                    }
            }
            .subscribe()

        client.eventDispatcher.on(ReactionAddEvent::class.java)
            .filter { it.emoji == EMOJI_MATCH_FINISHED }
            .filterWhen {
                mono {
                    val message = it.message.awaitSingle()
                    val userId = Snowflake.of(message.userData.id())
                    userId == botId && message.getReactors(EMOJI_MATCH_FINISHED)
                        .any { it.id == botId }
                        .awaitSingle()
                }
            }
            .flatMap { event ->
                handleReactMatchFinished(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        Logger.warn(error)
                        Mono.empty()
                    }
            }
            .subscribe()

        client.eventDispatcher.on(PresenceUpdateEvent::class.java)
            .filter { it.current.status == Status.OFFLINE && matchService.isPlayerQueued(it.userId.asLong()) }
            .doOnNext { matchService.leave(it.userId.asLong()) }
            .subscribe()

        client.eventDispatcher.on(GuildCreateEvent::class.java)
            .flatMap { event ->
                handleGuildCreateEvent(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        Logger.warn(error)
                        Mono.empty()
                    }
            }
            .subscribe()
    }

    private fun handleGuildCreateEvent(event: GuildCreateEvent) = mono {
        Logger.info { "Joined Guild: ${event.guild.name}" }

        val queueChannel = event.guild.channels
            .filter { it.name == "match-queue" }
            .awaitFirstOrNull() as? MessageChannel

        if (queueChannel != null) {
            Logger.info { "Found queue channel! Starting cleanup!" }

            if (queueChannel.lastMessageId.isPresent) {
                queueChannel
                    .getMessagesBefore(queueChannel.lastMessageId.get())
                    .doOnNext { it.delete().subscribe() }
                    .subscribe()

                queueChannel.lastMessage.doOnNext { it.delete().subscribe() }.subscribe()
            }

            val queueMsg = queueChannel.createMessage {
                it.setEmbed {}
            }.awaitSingle()

            queueMsg.addReaction(EMOJI_JOIN_QUEUE).awaitFirstOrNull()
            queueMsg.addReaction(EMOJI_LEAVE_QUEUE).awaitFirstOrNull()

            queueChannel.createMessage {
                @Suppress("MagicNumber")
                it.setContent("-".repeat(32))
            }.awaitSingle()

            while (true) {
                queueMsg.getReactors(EMOJI_JOIN_QUEUE)
                    .filter { it.id != botId }
                    .collectList()
                    .awaitSingle()
                    .forEach { user ->
                        matchService.join(user.id.asLong())
                        queueMsg.removeReaction(EMOJI_JOIN_QUEUE, user.id).awaitFirstOrNull()
                    }

                queueMsg.getReactors(EMOJI_LEAVE_QUEUE)
                    .filter { it.id != botId }
                    .collectList()
                    .awaitSingle()
                    .forEach { user ->
                        matchService.leave(user.id.asLong())
                        queueMsg.removeReaction(EMOJI_LEAVE_QUEUE, user.id).awaitFirstOrNull()
                    }

                queueMsg.edit { msg ->
                    msg.setEmbed { embed ->
                        embed.setTitle("Players waiting in queue")
                        val players = when {
                            matchService.getNumPlayers() == 0 -> "No one in queue ${EMOJI_QUEUE_EMPTY.print()}."
                            else -> matchService.getPlayers().joinToString("\n") { it -> "<@$it>" }
                        }

                        embed.setDescription("""
                        | $players
                        | 
                        | Use ${EMOJI_JOIN_QUEUE.print()} to join the queue.
                        | Use ${EMOJI_LEAVE_QUEUE.print()} to leave the queue.   
                        """.trimMargin())
                    }
                }.awaitSingle()

                if (matchService.canPop()) {
                    handleQueuePop(queueChannel)
                }

                @Suppress("MagicNumber")
                delay(500)
            }
        }
    }

    private fun handleCommandEvent(event: MessageCreateEvent) = mono {
        val msg = event.message
        val text = msg.content.toLowerCase()

        val args = parseArgs(text.substring(Config.PREFIX.length)).iterator()

        val sudo = msg.content.startsWith("${Config.PREFIX}sudo") &&
                client.applicationInfo.awaitSingle().ownerId == Snowflake.of(msg.userData.id())

        if (sudo && args.hasNext()) {
            args.next()
        }

        var command: Command? = commands
        while (args.hasNext() && command is Command.Group) {
            command = command.commands[args.next()]
        }

        if (command == null) {
            msg.channel.awaitSingle().createEmbed { embed ->
                embed.setDescription("Unknown command: $text")
            }.awaitFirst()

            return@mono
        }

        val namespace = command.namespace
        val hasPermission = sudo || permissionService
            .getGroupsByUser(Snowflake.of(msg.userData.id()).asLong())
            .flatMap { it.second }
            .toSet()
            .any { permission ->
                permission
                    .replace(".", "\\.")
                    .replace("*", ".*")
                    .toRegex()
                    .matches(namespace)
            }

        if (!hasPermission) {
            msg.channel.awaitSingle().createEmbed { embed ->
                embed.setDescription("You do not have permissions to use this command")
            }.awaitFirst()

            return@mono
        }

        when (command) {
            is Command.Invokable -> {
                val commandArgs = createCommandArguments(command, args.asSequence().toList())
                if (commandArgs != null) {
                    command.handler(CommandContext(commandArgs, msg))
                } else {
                    printCommandHelp(msg.channel.awaitSingle(), command)
                }
            }

            is Command.Group -> {
                printCommandHelp(msg.channel.awaitSingle(), command)
            }
        }
    }

    private fun createCommandArguments(command: Command.Invokable, args: Collection<String>): CommandContext.Arguments? {
        val argRange = command.args.filter { !it.optional }.size..command.args.size
        if (args.size in argRange) {
            return CommandContext.Arguments(command.args.map { it.name }.zip(args).toMap())
        }
        return null
    }

    private suspend fun handleQueuePop(channel: MessageChannel) {
        val pop = matchService.pop()
        val players = pop.players
        val canDeny = pop.request != null
        val requiredPlayers = pop.request?.minPlayers ?: Config.REQUIRED_PLAYERS

        val message = channel.createMessage {
            it.setContent(players.joinToString(" ") { player -> "<@$player>" })
        }.awaitSingle()

        message.addReaction(EMOJI_ACCEPT).awaitFirstOrNull()

        if (canDeny) {
            message.addReaction(EMOJI_DENY).awaitFirstOrNull()
        }

        val content = when {
            pop.request != null -> "A ${pop.request.minPlayers} player match has been requested by <@${pop.request.player}>." +
                    "Please react with a ${EMOJI_ACCEPT.print()} to accept the match. If you want to deny the match please react with a ${EMOJI_DENY.print()}. " +
                    "You have ${Config.ACCEPT_TIMEOUT} seconds to accept or deny, otherwise you will be removed from the queue."

            else -> "A match is ready, please react with a ${EMOJI_ACCEPT.print()}} to accept the match. " +
                    "You have ${Config.ACCEPT_TIMEOUT} seconds to accept, otherwise you will be removed from the queue."
        }

        val (missing, accepted, denied) = waitForQueuePopResponses(message, pop, content)

        if (accepted.count() >= requiredPlayers) {
            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match ready!")
                    embed.setDescription("Everybody get ready, you've got a match.\nHave fun!\n\nPlease react with a ${EMOJI_MATCH_FINISHED.print()} after the match is finished to get added back to the queue.")
                    embed.addField("Players", players.joinToString(" ") { "<@$it>" }, true)
                }
            }.awaitSingle()

            message.removeAllReactions().awaitFirstOrNull()
            message.addReaction(EMOJI_MATCH_FINISHED).awaitFirstOrNull()
        } else {
            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match Cancelled!")
                    embed.setDescription("Not enough players accepted the match")
                }
            }.awaitSingle()

            for (player in accepted) {
                matchService.join(player)
            }
        }

        for (player in denied) {
            matchService.join(player)
        }

        for (player in missing) {
            matchService.leave(player)
        }

        val channelId = channel.id
        val messageId = message.id
        CoroutineScope(Dispatchers.Default).launch {
            delay(2 * 60 * 60 * 1000L)
            client.getMessageById(channelId, messageId)
                .awaitFirstOrNull()
                ?.delete()
                ?.awaitFirstOrNull()
        }
    }

    private data class PopResonse(
        val missing: Collection<Long>,
        val accepted: MutableCollection<Long>,
        val denied: MutableCollection<Long>
    )

    private suspend fun waitForQueuePopResponses(message: Message, pop: MatchService.Pop, messageContent: String): PopResonse {
        val endTime = System.currentTimeMillis() + (Config.ACCEPT_TIMEOUT * 1000L)
        val missing = HashSet(pop.players)
        val accepted = HashSet<Long>()
        val denied = HashSet<Long>()
        val requiredPlayers = pop.request?.minPlayers ?: Config.REQUIRED_PLAYERS

        while (true) {
            message.getReactors(EMOJI_ACCEPT)
                .filter { missing.contains(it.id.asLong()) }
                .collectList()
                .awaitSingle()
                .onEach { accepted.add(it.id.asLong()) }
                .onEach { missing.remove(it.id.asLong()) }

            message.getReactors(EMOJI_DENY)
                .filter { missing.contains(it.id.asLong()) }
                .collectList()
                .awaitSingle()
                .onEach { denied.add(it.id.asLong()) }
                .onEach { missing.remove(it.id.asLong()) }

            if (System.currentTimeMillis() >= endTime || accepted.count() >= requiredPlayers || missing.isEmpty()) {
                break
            }

            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match found!")
                    embed.setDescription(messageContent)
                    embed.addField("Time remaining: ", "${max(0, (endTime - System.currentTimeMillis()) / 1000)}", true)
                    if (missing.isNotEmpty()) {
                        embed.addField(
                            "Missing players: ",
                            missing.joinToString(" ") { player -> "<@$player>" },
                            true
                        )
                    }
                }
            }.awaitSingle()

            delay(1000)
        }

        return PopResonse(missing, accepted, denied)
    }

    private fun handleReactMatchFinished(event: ReactionAddEvent) = mono {
        val message = event.message.awaitSingle()

        val players = message
            .embeds
            .mapNotNull { embed ->
                embed
                    .fields
                    .find { it.name == "Players" }
                    ?.value
                    ?.split(" ")
                    ?.map { it.substringAfter("<@").substringBefore(">") }
                    ?.mapNotNull { it.toLongOrNull() }
            }
            .flatten()

        if (players.contains(event.userId.asLong())) {
            for (player in players) {
                matchService.join(player)
            }

            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match finished!")
                    embed.setDescription("Players have rejoined the queue. Let's have another one!")
                }
            }.awaitSingle()

            message.removeAllReactions().awaitFirstOrNull()
        }
    }

    private fun handleMatchHubJoined(event: VoiceStateUpdateEvent) = mono {
        val userId = event.current.userId.asLong()

        if (!matchService.isPlayerQueued(userId)) {
            val msgChannel = event
                .current
                .guild
                .awaitSingle()
                .channels
                .filter { it.name == "match-queue" }
                .awaitFirstOrNull()

            matchService.join(userId)

            if (msgChannel is MessageChannel) {
                val msg = msgChannel.createMessage {
                    it.setContent("Hey <@$userId>. I've noticed you joined the Match Hub voice channel, so I've added you to the queue. Don't forget to leave the queue when you're not available anymore.")
                }.awaitSingle()

                delay(60 * 1000)

                msg.delete().awaitFirstOrNull()
            }
        }
    }

    private suspend fun printCommandHelp(channel: MessageChannel, command: Command) {
        channel.createEmbed { embed ->
            embed.setTitle("Usage: ${formatCommandHelp(command.fullname, command)}")
            embed.setDescription(command.description)

            when (command) {
                is Command.Invokable -> {
                    embed.addField(
                        "arguments",
                        command.args.joinToString("\n\n") { "*${it.name}*\n${it.description}" },
                        false
                    )
                }
                is Command.Group -> {
                    embed.addField(
                        "subcommands",
                        command.commands.values.joinToString("\n\n") {
                            "${formatCommandHelp(
                                it.name,
                                it
                            )}\n${it.description}"
                        },
                        false
                    )
                }
            }
        }.awaitFirst()
    }

    private fun updateStatus() {
        val playersInQueue = matchService.getNumPlayers()

        val status = when {
            playersInQueue > 0 -> "at ${matchService.getNumPlayers()} players in queue"
            else -> "at ${LOOKING_AT[((System.currentTimeMillis() / (60 * 1000)) % LOOKING_AT.size).toInt()]}"
        }

        client.updatePresence(
            StatusUpdate.builder()
                .status(Status.ONLINE.value)
                .game(
                    ActivityUpdateRequest.builder()
                        .name(status)
                        .type(Activity.Type.WATCHING.value)
                        .build()
                )
                .afk(false)
                .since(Optional.empty())
                .build()
        ).block()
    }

    suspend fun run() {
        client.onDisconnect().awaitSingle()
    }

    companion object {
        val LOOKING_AT = listOf(
            "Imanis tights",
            "Vodens tail",
            "Pakkos toys"
        )

//        val EMOJI_ACCEPT = ReactionEmoji.custom(Snowflake.of("630950806430023701"), "ReadyScrollEmote", true)
//        val EMOJI_DENY = ReactionEmoji.unicode("\uD83D\uDC4E")
//        val EMOJI_MATCH_FINISHED = ReactionEmoji.custom(Snowflake.of("632026748946481162"), "GG", false)
//        val EMOJI_QUEUE_EMPTY = ReactionEmoji.custom(Snowflake.of("631489587092520980"), "FeelsWuMan", false)
//        val EMOJI_JOIN_QUEUE = ReactionEmoji.custom(Snowflake.of("630952770694021130"), "GiganticHeart", false)
//        val EMOJI_LEAVE_QUEUE = ReactionEmoji.custom(Snowflake.of("631660093108256768"), "GiganticBrokenHeart", false)

        val EMOJI_ACCEPT = ReactionEmoji.unicode("\uD83D\uDC4D")
        val EMOJI_DENY = ReactionEmoji.unicode("\uD83D\uDC4E")
        val EMOJI_MATCH_FINISHED = ReactionEmoji.unicode("\uD83C\uDFC1")
        val EMOJI_QUEUE_EMPTY = ReactionEmoji.unicode("\uD83D\uDE22")
        val EMOJI_JOIN_QUEUE = ReactionEmoji.unicode("\uD83D\uDC4D")
        val EMOJI_LEAVE_QUEUE = ReactionEmoji.unicode("\uD83D\uDC4E")
    }
}
