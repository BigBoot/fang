package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.service.AutostartService
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.command.ApplicationCommandOption;
import discord4j.core.spec.*
import org.koin.core.component.inject
import org.koin.core.component.KoinComponent

data class Argument(
    val name: String,
    val description: String,
    val optional: Boolean,
    val verifier: ((String) -> Boolean)? = null,
)

interface Commands {
    val commands: Map<String, Command>
}

abstract class CommandGroupSpec(val name: String, val description: String) : KoinComponent {
    abstract val build: CommandGroupBuilder.() -> Unit
}

sealed class Command(val name: String, val description: String, var slashCommand: ImmutableApplicationCommandOptionData.Builder) {
    var parent: Command? = null
        internal set

    class Invokable(
        name: String,
        description: String,
        val args: Array<Argument>,
        val handler: suspend CommandContext.() -> MessageCreateSpec.Builder.() -> Unit
    ) :
        Command(name, description, run {
            var command = ApplicationCommandOptionData.builder()
                                           .name(name)
                                           .description(description)
                                           .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());
            args.forEach {
                command = command.addOption(ApplicationCommandOptionData.builder()
                                                .name(it.name)
                                                .description(it.description)
                                                .required(!it.optional)
                                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                                .build());
            }
            command
        })


    class Group(name: String, description: String, override val commands: Map<String, Command>) :
        Command(name, description, ApplicationCommandOptionData.builder()
                                       .name(name)
                                       .description(description)
                                       .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())), Commands {

        operator fun plus(other: Command): Group {
            return Group(
                name,
                description,
                commands + mapOf(other.name to other)
            ).apply {
                parent = this@Group.parent
            }
        }

        override val namespace get() = "${super.namespace}."
    }

    val chain: List<Command> get() {
        val result = ArrayList<Command>()

        var cur: Command? = this
        while (cur != null && cur.name.isNotEmpty()) {
            result.add(cur)
            cur = cur.parent
        }

        return result.reversed()
    }

    val fullname: String get() = chain.joinToString(" ") { it.name }
    open val namespace: String get() = chain.joinToString(".") { it.name }
}

class CommandBuilder(private val name: String, private val description: String) {
    private var handler: suspend CommandContext.() -> MessageCreateSpec.Builder.() -> Unit = {{}}
    private var args = ArrayList<Argument>()

    fun arg(name: String, description: String = "", optional: Boolean = false, verify: ((String) -> Boolean)? = null) {
        this.args.add(Argument(name, description, optional, verify))
    }

    fun onCall(handler: suspend CommandContext.() -> MessageCreateSpec.Builder.() -> Unit) {
        this.handler = handler
    }

    fun build(): Command.Invokable =
        Command.Invokable(
            name = name,
            description = description,
            handler = handler,
            args = this@CommandBuilder.args
                .sortedBy {
                    if (it.optional) {
                        1
                    } else {
                        0
                    }
                }
                .toTypedArray()
        )

    fun args(): Array<Argument> =
        this@CommandBuilder.args
            .sortedBy {
                if (it.optional) {
                    1
                } else {
                    0
                }
            }
            .toTypedArray()
}

class CommandGroupBuilder(private val name: String, private val description: String) : AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val commands = HashMap<String, Command>()

    fun command(name: String, description: String = "", builder: CommandBuilder.() -> Unit = {}) {
        commands[name] = CommandBuilder(name, description).apply(builder).build();
    }

    fun group(spec: CommandGroupSpec) {
        commands[spec.name] = CommandGroupBuilder(
            spec.name,
            spec.description
        ).apply(spec.build).build()
    }

    fun build(): Command.Group = Command.Group(
        name = name,
        description = description,
        commands = commands
    ).apply {
        commands.values.forEach { it.parent = this };

        var command = ApplicationCommandRequest.builder()
            .name(name)
            .description(description)
            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());
        commands.values.forEach {
            command = command.addOption(it.slashCommand.build());
        }

        val applicationId = client.getRestClient().getApplicationId().block();

        commands.values.forEach {
            client.getRestClient().getApplicationService()
                .createGlobalApplicationCommand(applicationId, command.build())
                .subscribe();
        }
    }
}
