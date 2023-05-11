package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.*
import de.bigboot.ggtools.fang.commands.Root
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.command.ApplicationCommandOption;
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.*
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

        client.eventDispatcher.on<ChatInputInteractionEvent>()
            .onEachSafe(this::handleSlashEvent)
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

        var send: (MessageCreateSpec.Builder.() -> Unit)? = null;

        when (command) {
            is Command.Invokable -> {
                val argList = args.asSequence().toList()
                val commandArgs = createCommandArguments(command, argList)
                if (commandArgs != null) {
                    val invalidArgs = verfiyCommandArguments(command, argList)
                    if (invalidArgs.isEmpty()) {
                        send = command.handler(CommandContext(commandArgs, msg.channel.awaitSingle(), msg.guild.awaitSingle(), msg.authorAsMember.awaitSingle()))
                    } else {
                        printInvalidArguments(msg.channel.awaitSingle(), command, invalidArgs)
                    }
                } else {
                    send = printCommandHelp(command)
                }
            }

            is Command.Group -> {
                send = printCommandHelp(command)
            }
        }

        send.let{
            msg.channel.awaitSingle().createMessage(MessageCreateSpec.builder().apply(it!!).build()).awaitSingle();
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

    private suspend fun printCommandHelp(command: Command): MessageCreateSpec.Builder.() -> Unit {
        return {
            addEmbedCompat {
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
            }
        }
    }

    private suspend fun handleSlashEvent(event: ChatInputInteractionEvent) {
        event.deferReply().awaitSafe()

        val interaction = event.interaction;

        var args = event.getOptions();

        var command: Command = commands.commands[event.getCommandName()] ?: return;
        while (args.size > 0 && command is Command.Group) {
            val next = args.first();
            args = next.getOptions();
            command = command.commands[next.name] ?: return;
        }

        val namespace = command.namespace
        val hasPermission = permissionService
            .getGroupsByUser(interaction.user.id.asLong())
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
            event.editReplyCompat {
                addEmbedCompat {
                    description("You do not have permissions to use this command")
                }
            }.await()
            return
        }

        var returnCommand: MessageCreateSpec.Builder.() -> Unit?;

        when (command) {

            is Command.Invokable -> {
                val args = args.map { it.value.get().asString() }.toList();
                val commandArgs = createCommandArguments(command, args)!!;
                returnCommand = command.handler(CommandContext(commandArgs, interaction.channel.awaitSingle(), interaction.guild.awaitSingle(), interaction.user))
            }

            is Command.Group -> {
                returnCommand = printCommandHelp(command);
            }
        }

        returnCommand.let{
            val command = MessageCreateSpec.builder().apply(it).build()

            var interaction = InteractionReplyEditSpec.builder();

            if (!command.content().isAbsent()) {
                command.content().get().let{interaction = interaction.content(it)}
            }

            if (!command.embeds().isAbsent()) {
                command.embeds().get().let{it.forEach { interaction = interaction.addEmbed(it)}}
            }

            event.editReply(interaction.build()).awaitSingle()
        }
    }
}
