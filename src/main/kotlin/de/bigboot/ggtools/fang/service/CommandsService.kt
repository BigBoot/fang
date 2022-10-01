package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.*
import de.bigboot.ggtools.fang.commands.Root
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Locale

class CommandsService : AutostartService, KoinComponent {
    private val client: GatewayDiscordClient by inject()
    private val permissionService: PermissionService by inject()

    private var commands = CommandGroupBuilder("", "").apply(Root().build).build()

    init {
        client.eventDispatcher.on<MessageCreateEvent>()
            .filter { it.message.content.startsWith(Config.bot.prefix) }
            .onEachSafe(this::handleCommandEvent)
            .launch()
    }

    private suspend fun handleCommandEvent(event: MessageCreateEvent) {
        val msg = event.message
        val text = msg.content

        val args = parseArgs(text.substring(Config.bot.prefix.length)).iterator()

        val sudo = text.startsWith("${Config.bot.prefix}sudo") &&
                client.applicationInfo.awaitSingle().ownerId == Snowflake.of(msg.userData.id())

        if (sudo && args.hasNext()) {
            args.next()
        }

        var command: Command? = commands
        while (args.hasNext() && command is Command.Group) {
            command = command.commands[args.next().lowercase(Locale.getDefault())]
        }

        if (command == null) {
            msg.channel.awaitSingle().createEmbedCompat {
                description("Unknown command: $text")
            }.awaitSingle()

            return
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
            msg.channel.awaitSingle().createEmbedCompat {
                description("You do not have permissions to use this command")
            }.awaitSingle()

            return
        }

        when (command) {
            is Command.Invokable -> {
                val argList = args.asSequence().toList()
                val commandArgs = createCommandArguments(command, argList)
                if (commandArgs != null) {
                    val invalidArgs = verfiyCommandArguments(command, argList)
                    if (invalidArgs.isEmpty()) {
                        command.handler(CommandContext(commandArgs, msg))
                    } else {
                        printInvalidArguments(msg.channel.awaitSingle(), command, invalidArgs)
                    }
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
            return CommandContext.Arguments(
                command.args.map { it.name }.zip(args).toMap()
            )
        }
        return null
    }

    private fun verfiyCommandArguments(command: Command.Invokable, args: Collection<String>): Collection<Pair<Argument, String>> {
        return command.args
            .zip(args)
            .filter { it.first.verifier?.invoke(it.second) == false }
    }

    private suspend fun printInvalidArguments(
        channel: MessageChannel,
        command: Command.Invokable,
        args: Collection<Pair<Argument, String>>
    ) {
        channel.createEmbedCompat {
            title("Usage: ${formatCommandHelp(command.fullname, command)}")
            description(args.joinToString("\n") { "Invalid value for argument ${it.first.name}: ${it.second}\nExpected: ${it.first.description}" })
        }.awaitSingle()
    }

    private suspend fun printCommandHelp(channel: MessageChannel, command: Command) {
        channel.createEmbedCompat {
            title("Usage: ${formatCommandHelp(command.fullname, command)}")
            description(command.description)

            when (command) {
                is Command.Invokable -> {
                    addField(
                        "arguments",
                        command.args.joinToString("\n\n") { "*${it.name}*\n${it.description}" },
                        false
                    )
                }
                is Command.Group -> {
                    addField(
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
        }.awaitSingle()
    }
}
