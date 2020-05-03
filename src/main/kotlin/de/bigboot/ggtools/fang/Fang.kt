package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.commands.Commands
import de.bigboot.ggtools.fang.utils.parseArgs
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Status
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
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
    private val matchManager = MatchManager()

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

        updateStatusTimer.schedule(object: TimerTask() {
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
    }

    private fun buildCommandTree(command: Command, tree: StringBuilder, prefix: String, last: Boolean): StringBuilder {
        val branch = when (last) {
            true -> "└── "
            false -> "├── "
        }

        tree.appendln("$prefix$branch${command.name}")

        if(command is Command.Group) {
            command.commands.values.forEachIndexed { i, subcommand ->
                val sublast = command.commands.size-1 == i

                val subprefix = prefix + when {
                    last -> "    "
                    else -> "│   "
                }
                buildCommandTree(subcommand, tree, subprefix, sublast)
            }

            if(!last) {
                tree.appendln("$prefix│   ")
            }
        }

        return tree
    }

    private fun printCommandTree(): String {
        val tree = StringBuilder()
        commands.commands.values.forEachIndexed { i, subcommand ->
            val last = commands.commands.size-1 == i
            buildCommandTree(subcommand, tree, "", last)
        }

        return tree.toString()
    }


    private fun handleCommandEvent(event: MessageCreateEvent) = mono {
        val msg = event.message

        val args = parseArgs(msg.content.substring(Config.PREFIX.length)).iterator()

        val sudo = msg.content.startsWith("${Config.PREFIX}sudo")
                && client.applicationInfo.awaitSingle().ownerId == Snowflake.of(msg.userData.id())

        if(sudo && args.hasNext()) {
            args.next()
        }

        var command: Command? = commands
        while(args.hasNext() && command is Command.Group) {
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

        when(command) {
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

        if(matchManager.canPop()) {
            val players = matchManager.pop()
            val missingPlayers = ArrayList(players)

            val endTime = System.currentTimeMillis() + (Config.ACCEPT_TIMEOUT * 1000L)

            val pop = msg.channel.awaitSingle().createMessage {
                it.setContent(players.joinToString(" ") { player -> "<@$player>" })
            }.awaitSingle()

            pop.addReaction(ReactionEmoji.unicode("\uD83D\uDC4D")).awaitFirstOrNull()

            while (true) {
                if(System.currentTimeMillis() >= endTime || missingPlayers.isEmpty()) {
                    break
                }

                pop.getReactors(ReactionEmoji.unicode("\uD83D\uDC4D")).collectList().awaitSingle().forEach {
                    missingPlayers.remove(it.id.asString())
                }

                pop.edit {
                    it.setEmbed { embed ->
                        embed.setTitle("Match found!")
                        embed.setDescription("A match is ready, please react with a \uD83D\uDC4D to accept the match. You have 60 seconds to accept, otherwise you will be removed from the queue and everybody else will be put back into the queue.")
                        embed.addField("Time remaining: ", "${max(0, (endTime-System.currentTimeMillis())/1000)}", true)
                        if(missingPlayers.isNotEmpty()) {
                            embed.addField("Missing players: ", missingPlayers.joinToString(" ") { player -> "<@$player>" }, true)
                        }
                    }
                }.awaitSingle()

                delay(1000)
            }

            if (missingPlayers.isEmpty()) {
                pop.edit {
                    it.setEmbed { embed ->
                        embed.setTitle("Match ready!")
                        embed.setDescription("Everybody get ready, you've got a match.\nHave fun!")
                        embed.addField("Players: ", players.joinToString(" ") { "<@$it>" }, true)
                    }
                }.awaitSingle()
            }
            else {
                pop.edit {
                    it.setEmbed { embed ->
                        embed.setTitle("Match Cancelled!")
                        embed.setDescription("Not all players accepted the match")
                    }
                }.awaitSingle()

                pop.removeAllReactions()

                for(player in players - missingPlayers) {
                    matchManager.join(player)
                }
            }
        }
    }

    private fun formatArg(arg: Argument) = if (arg.optional) { "[${arg.name}]" } else { "<${arg.name}>" }

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
                        command.commands.values.joinToString("\n\n") { "${formatCommandHelp(it.name, it)}\n${it.description}" },
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
            else -> "at ${LOOKING_AT[((System.currentTimeMillis() / (60 * 1000) ) % LOOKING_AT.size).toInt()]}"
        }

        client.updatePresence(StatusUpdate.builder()
            .status(Status.ONLINE.value)
            .game(ActivityUpdateRequest.builder()
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