package de.bigboot.ggtools.fang.commands

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Admin
import de.bigboot.ggtools.fang.commands.queue.Queue
import de.bigboot.ggtools.fang.commands.server.Server
import de.bigboot.ggtools.fang.utils.*
import kotlinx.coroutines.reactive.awaitFirst

class Root : CommandGroupSpec("", "") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Admin())
        group(Queue())
        group(Server())

        command("help", "show this help") {
            onCall {
                {
                    addEmbedCompat {
                        title("Help")
                        addField(
                            "commands",
                            commands.values.joinToString("\n\n") {
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

        command("commands", "Show all available commands") {
            onCall {
                {
                    addEmbedCompat {
                        title("Commands")
                        description("```\n${formatCommandTree(commands.values)}\n```")
                    }
                }
            }
        }
    }
}
