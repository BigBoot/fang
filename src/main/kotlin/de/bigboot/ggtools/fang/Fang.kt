package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.commands.Commands
import de.bigboot.ggtools.fang.utils.parseArgs
import de.bigboot.ggtools.fang.utils.print
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Status
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.discordjson.json.ActivityUpdateRequest
import discord4j.discordjson.json.gateway.StatusUpdate
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.jetbrains.exposed.sql.Database
import reactor.core.publisher.Mono
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max

class Fang(private val client: GatewayDiscordClient) {
    private val database = Database.connect(
        url = Config.DB_URL,
        driver = Config.DB_DRIVER,
        user = Config.DB_USER,
        password = Config.DB_PASS
    )
    private val gcpManager = ServerManager(database)
    private val permissionManager = PermissionManager(database)
    private val matchManager = MatchManager(database)

    private val updateStatusTimer = Timer(true)

    private var commands = CommandGroupBuilder("", "").apply(Commands().build).build()

    init {
        commands += Command.Invokable(
            "help",
            "show this help",
            emptyArray()
        ) {
            channel().createEmbed { embed ->
                embed.setTitle("Help")
                embed.addField(
                    "commands",
                    this@Fang.commands.commands.values.joinToString("\n\n") {
                        "${formatCommandHelp(
                            it.name,
                            it
                        )}\n${it.description}"
                    },
                    false
                )
            }.awaitFirst()
        }


        commands += Command.Invokable(
            "commands",
            "Show all available commands",
            emptyArray()
        ) {
            channel().createEmbed { embed ->
                embed.setTitle("Commands")
                embed.setDescription("```\n${printCommandTree()}\n```")

            }.awaitFirst()
        }

        updateStatusTimer.schedule(object : TimerTask() {
            override fun run() {
                updateStatus()
            }
        }, 0, 2000)

        client.eventDispatcher.on(MessageCreateEvent::class.java)
            .filter { it.message.content.startsWith(Config.PREFIX) }
            .flatMap { event ->
                handleCommandEvent(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        error.printStackTrace()
                        Mono.empty()
                    }
            }
            .subscribe()

        client.eventDispatcher.on(VoiceStateUpdateEvent::class.java)
            .filter { it.old.isEmpty }
            .filterWhen {
                mono {
                    it.current.channel.awaitFirstOrNull()?.name == "Match Hub"
                }
            }
            .flatMap { event ->
                handleMatchHubJoined(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        error.printStackTrace()
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
                    val botId = client.selfId.awaitSingle()
                    userId == botId && message.getReactors(EMOJI_MATCH_FINISHED)
                        .any { it.id == botId }
                        .awaitSingle()
                }
            }
            .flatMap { event ->
                handleReactMatchFinished(event)
                    .onErrorResume { error ->
                        // log and then discard the error to keep the sequence alive
                        error.printStackTrace()
                        Mono.empty()
                    }
            }
            .subscribe()

        client.eventDispatcher.on(PresenceUpdateEvent::class.java)
            .filter { it.current.status == Status.OFFLINE && matchManager.isPlayerQueued(it.userId.asLong()) }
            .doOnNext { matchManager.leave(it.userId.asLong()) }
            .subscribe()
    }

    private fun buildCommandTree(command: Command, tree: StringBuilder, prefix: String, last: Boolean): StringBuilder {
        val branch = when (last) {
            true -> "└── "
            false -> "├── "
        }

        tree.appendln("$prefix$branch${command.name}")

        if (command is Command.Group) {
            command.commands.values.forEachIndexed { i, subcommand ->
                val sublast = command.commands.size - 1 == i

                val subprefix = prefix + when {
                    last -> "    "
                    else -> "│   "
                }
                buildCommandTree(subcommand, tree, subprefix, sublast)
            }

            if (!last) {
                tree.appendln("$prefix│   ")
            }
        }

        return tree
    }

    private fun printCommandTree(): String {
        val tree = StringBuilder()
        commands.commands.values.forEachIndexed { i, subcommand ->
            val last = commands.commands.size - 1 == i
            buildCommandTree(subcommand, tree, "", last)
        }

        return tree.toString()
    }


    private fun handleCommandEvent(event: MessageCreateEvent) = mono {
        val msg = event.message

        val args = parseArgs(msg.content.substring(Config.PREFIX.length)).iterator()

        val sudo = msg.content.startsWith("${Config.PREFIX}sudo")
                && client.applicationInfo.awaitSingle().ownerId == Snowflake.of(msg.userData.id())

        if (sudo && args.hasNext()) {
            args.next()
        }

        var command: Command? = commands
        while (args.hasNext() && command is Command.Group) {
            command = command.commands[args.next()]
        }

        if (command == null) {
            msg.channel.awaitSingle().createEmbed { embed ->
                embed.setDescription("Unknown command: ${msg.content}")
            }.awaitFirst()

            return@mono
        }

        val namespace = command.namespace
        val hasPermission = sudo || permissionManager
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
                val argRange = command.args.filter { !it.optional }.size..command.args.size
                val commandArgs = args.asSequence().toList()
                if (commandArgs.size in argRange) {
                    val argMap =
                        CommandContext.Arguments(command.args.map { it.name }
                            .zip(commandArgs).toMap())
                    val ctx = CommandContext(
                        args = argMap,
                        message = msg,
                        serverManager = gcpManager,
                        matchManager = matchManager,
                        permissionManager = permissionManager
                    )
                    command.handler(ctx)
                } else {
                    printCommandHelp(msg.channel.awaitSingle(), command)
                }
            }

            is Command.Group -> {
                printCommandHelp(msg.channel.awaitSingle(), command)
            }
        }

        if (matchManager.canPop()) {
            handleQueuePop(msg.channel.awaitSingle())
        }
    }

    private suspend fun handleQueuePop(channel: MessageChannel) {
        val pop = matchManager.pop()
        val players = pop.players
        val missing = HashSet(players)
        val accepted = HashSet<Long>()
        val denied = HashSet<Long>()
        val canDeny = pop.request != null
        val requiredPlayers = pop.request?.minPlayers ?: 10

        val endTime = System.currentTimeMillis() + (Config.ACCEPT_TIMEOUT * 1000L)

        val message = channel.createMessage {
            it.setContent(players.joinToString(" ") { player -> "<@$player>" })
        }.awaitSingle()

        message.addReaction(EMOJI_ACCEPT).awaitFirstOrNull()

        if(canDeny) {
            message.addReaction(EMOJI_DENY).awaitFirstOrNull()
        }

        val content = when {
            pop.request != null -> "A ${pop.request.minPlayers} player match has been requested by <@${pop.request.player}>." +
                    "Please react with a ${EMOJI_ACCEPT.print()} to accept the match. If you want to deny the match please react with a ${EMOJI_DENY.print()}. " +
                    "You have ${Config.ACCEPT_TIMEOUT} seconds to accept or deny, otherwise you will be removed from the queue."

            else -> "A match is ready, please react with a ${EMOJI_ACCEPT.print()}} to accept the match. " +
                    "You have ${Config.ACCEPT_TIMEOUT} seconds to accept, otherwise you will be removed from the queue."
        }



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
                    embed.setDescription(content)
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

        if (accepted.count() >= requiredPlayers) {
            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match ready!")
                    embed.setDescription("Everybody get ready, you've got a match.\nHave fun!\n\nPlease react with a ${EMOJI_MATCH_FINISHED.print()} after the match is finished to get added back to the queue.")
                    embed.addField("Players", players.joinToString(" ") { "<@$it>" }, true)
                }
            }.awaitSingle()

            message.addReaction(EMOJI_MATCH_FINISHED).awaitFirstOrNull()
        } else {
            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match Cancelled!")
                    embed.setDescription("Not enough players accepted the match")
                }
            }.awaitSingle()

            for (player in accepted) {
                matchManager.join(player)
            }
        }

        for (player in denied) {
            matchManager.join(player)
        }

        for (player in missing) {
            matchManager.leave(player)
        }
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
                matchManager.join(player)
            }

            message.edit {
                it.setEmbed { embed ->
                    embed.setTitle("Match finished!")
                    embed.setDescription("Players have rejoined the queue. Let's have another one!")
                }
            }.awaitSingle()

            message.removeSelfReaction(EMOJI_MATCH_FINISHED).awaitSingle()
        }
    }

    private fun handleMatchHubJoined(event: VoiceStateUpdateEvent) = mono {
        val userId = event.current.userId.asLong()

        if (!matchManager.isPlayerQueued(userId)) {
            val msgChannel = event
                .current
                .guild
                .awaitSingle()
                .channels
                .filter { it.name == "match-hub" }
                .awaitFirstOrNull()


            if (msgChannel is MessageChannel) {
                msgChannel.createMessage {
                    it.setContent("Hey <@${userId}>. I've noticed you joined the Match Hub voice channel, please remember to also join the queue with `~queue join`")
                }.awaitSingle()
            }
        }
    }

    private fun formatArg(arg: Argument) = if (arg.optional) {
        "[${arg.name}]"
    } else {
        "<${arg.name}>"
    }

    private fun formatCommandHelp(name: String, command: Command): String {
        return when (command) {
            is Command.Invokable -> "$name ${command.args.joinToString(" ") { formatArg(it) }}"
            is Command.Group -> "$name <subcommand>"
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
        val playersInQueue = matchManager.getNumPlayers()

        val status = when {
            playersInQueue > 0 -> "at ${matchManager.getNumPlayers()} players in queue"
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

        val EMOJI_ACCEPT = ReactionEmoji.custom(Snowflake.of("630950806450995201"), "ReadyScrollEmote", true)
        val EMOJI_DENY = ReactionEmoji.unicode("\uD83D\uDC4E")
        val EMOJI_MATCH_FINISHED = ReactionEmoji.custom(Snowflake.of("632026748946481162"), "GG", false)

//        val EMOJI_ACCEPT = ReactionEmoji.unicode("\uD83D\uDC4D")
//        val EMOJI_MATCH_FINISHED = ReactionEmoji.unicode("\uD83C\uDFC1")
    }
}