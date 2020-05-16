package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.commands.Root
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.service.PermissionService
import de.bigboot.ggtools.fang.utils.asReaction
import de.bigboot.ggtools.fang.utils.formatCommandHelp
import de.bigboot.ggtools.fang.utils.milliSecondsToTimespan
import de.bigboot.ggtools.fang.utils.parseArgs
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Status
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
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
        }, 0, Config.bot.statusupdate_poll_rate)

        botId = runBlocking {
            client.selfId.awaitSingle()
        }

        registerEventListeners()
    }

    private fun registerEventListeners() {
        client.eventDispatcher.on(MessageCreateEvent::class.java)
            .filter { it.message.content.startsWith(Config.bot.prefix) }
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
            .filter { it.emoji == Config.emojis.match_finished.asReaction() }
            .filterWhen {
                mono {
                    val message = it.message.awaitSingle()
                    val userId = Snowflake.of(message.userData.id())
                    userId == botId && message.getReactors(Config.emojis.match_finished.asReaction())
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

            queueMsg.addReaction(Config.emojis.join_queue.asReaction()).awaitFirstOrNull()
            queueMsg.addReaction(Config.emojis.leave_queue.asReaction()).awaitFirstOrNull()

            queueChannel.createMessage {
                @Suppress("MagicNumber")
                it.setContent("-".repeat(32))
            }.awaitSingle()

            CoroutineScope(Dispatchers.Default).launch {
                queueMessageUpdateLoop(queueChannel.id, queueMsg.id)
            }
        }
    }

    private suspend fun queueMessageUpdateLoop(channelId: Snowflake, msgId: Snowflake) {
        while (true) {
            try {
                val channel = client.getChannelById(channelId).awaitSingle() as MessageChannel
                val msg = client.getMessageById(channelId, msgId).awaitSingle()

                msg.getReactors(Config.emojis.join_queue.asReaction())
                    .filter { it.id != botId }
                    .collectList()
                    .awaitSingle()
                    .forEach { user ->
                        matchService.join(user.id.asLong())
                        msg.removeReaction(Config.emojis.join_queue.asReaction(), user.id).awaitFirstOrNull()
                    }

                msg.getReactors(Config.emojis.leave_queue.asReaction())
                    .filter { it.id != botId }
                    .collectList()
                    .awaitSingle()
                    .forEach { user ->
                        matchService.leave(user.id.asLong())
                        msg.removeReaction(Config.emojis.leave_queue.asReaction(), user.id).awaitFirstOrNull()
                    }

                msg.edit { edit ->
                    edit.setEmbed { embed ->
                        embed.setTitle("${matchService.getNumPlayers()} players waiting in queue")
                        embed.setDescription("""
                        | ${matchService.printQueue()}
                        | 
                        | Use ${Config.emojis.join_queue} to join the queue.
                        | Use ${Config.emojis.leave_queue} to leave the queue.   
                        """.trimMargin())
                    }
                }.awaitSingle()

                if (matchService.canPop()) {
                    handleQueuePop(matchService.pop(), channel)
                }

                @Suppress("MagicNumber")
                delay(500)
            } catch (ignore: Exception) {
            }
        }
    }

    private fun handleCommandEvent(event: MessageCreateEvent) = mono {
        val msg = event.message
        val text = msg.content.toLowerCase()

        val args = parseArgs(text.substring(Config.bot.prefix.length)).iterator()

        val sudo = msg.content.startsWith("${Config.bot.prefix}sudo") &&
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

    private suspend fun handleQueuePop(pop: MatchService.Pop, channel: MessageChannel) {
        val players = pop.players + pop.previousPlayers
        val canDeny = pop.request != null
        val requiredPlayers = pop.request?.minPlayers ?: Config.bot.required_players

        val message = channel.createMessage {
            it.setContent(players.joinToString(" ") { player -> "<@$player>" })
        }.awaitSingle()

        message.addReaction(Config.emojis.accept.asReaction()).awaitFirstOrNull()

        if (canDeny) {
            message.addReaction(Config.emojis.deny.asReaction()).awaitFirstOrNull()
        }

        val content = when {
            pop.request != null -> "A ${pop.request.minPlayers} player match has been requested by <@${pop.request.player}>." +
                    "Please react with a ${Config.emojis.accept} to accept the match. If you want to deny the match please react with a ${Config.emojis.deny}. " +
                    "You have ${Config.bot.accept_timeout} seconds to accept or deny, otherwise you will be removed from the queue."

            else -> "A match is ready, please react with a ${Config.emojis.accept} to accept the match. " +
                    "You have ${Config.bot.accept_timeout} seconds to accept, otherwise you will be removed from the queue."
        }

        val (missing, accepted, denied) = waitForQueuePopResponses(message, pop, content)

        when {
            accepted.count() >= requiredPlayers -> {
                message.edit {
                    it.setEmbed { embed ->
                        embed.setTitle("Match ready!")
                        embed.setDescription("Everybody get ready, you've got a match.\nHave fun!\n\nPlease react with a ${Config.emojis.match_finished} after the match is finished to get added back to the queue.")
                        embed.addField("Players", players.joinToString(" ") { "<@$it>" }, true)
                    }
                }.awaitSingle()

                message.removeAllReactions().awaitFirstOrNull()

                val msgId = message.id
                val channelId = message.channelId
                CoroutineScope(Dispatchers.Default).launch {
                    delay(2 * 60 * 1000)
                    client.getMessageById(channelId, msgId).awaitFirstOrNull()
                        ?.addReaction(Config.emojis.match_finished.asReaction())
                        ?.awaitFirstOrNull()
                }

            }
            matchService.getNumPlayers() + accepted.size >= requiredPlayers -> {
                val repop = matchService.pop(accepted)
                message.delete().awaitFirstOrNull()
                handleQueuePop(repop, channel)
            }
            else -> {
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
        val endTime = System.currentTimeMillis() + (Config.bot.accept_timeout * 1000L)
        val missing = HashSet(pop.players)
        val accepted = HashSet(pop.previousPlayers)
        val denied = HashSet<Long>()
        val requiredPlayers = pop.request?.minPlayers ?: Config.bot.required_players

        while (true) {
            message.getReactors(Config.emojis.accept.asReaction())
                .filter { missing.contains(it.id.asLong()) }
                .collectList()
                .awaitSingle()
                .onEach { accepted.add(it.id.asLong()) }
                .onEach { missing.remove(it.id.asLong()) }

            message.getReactors(Config.emojis.deny.asReaction())
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
    }
}
