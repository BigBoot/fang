package de.bigboot.ggtools.fang.commands

import de.bigboot.ggtools.fang.Command
import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Admin
import de.bigboot.ggtools.fang.commands.queue.Queue
import de.bigboot.ggtools.fang.commands.server.Server
import de.bigboot.ggtools.fang.utils.formatCommandHelp
import de.bigboot.ggtools.fang.utils.formatCommandTree
import kotlinx.coroutines.reactive.awaitFirst

class Root : CommandGroupSpec("", "") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Admin())
        group(Queue())
        group(Server())

        command("help", "show this help") {
            onCall {
                channel().createEmbed { embed ->
                    embed.setTitle("Help")
                    embed.addField(
                        "commands",
                        commands.values.joinToString("\n\n") {
                            "${formatCommandHelp(
                                it.name,
                                it
                            )}\n${it.description}"
                        },
                        false
                    )
                }.awaitFirst()
            }
        }

        command("commands", "Show all available commands") {
            onCall {
                channel().createEmbed { embed ->
                    embed.setTitle("Commands")
                    embed.setDescription("```\n${formatCommandTree(commands.values)}\n```")
                }.awaitFirst()
            }
        }
    }
}

