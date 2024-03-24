package de.bigboot.ggtools.fang.commands

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Admin
import de.bigboot.ggtools.fang.commands.queue.Queue
import de.bigboot.ggtools.fang.commands.server.Server
import de.bigboot.ggtools.fang.commands.match.Match
import de.bigboot.ggtools.fang.utils.createEmbedCompat
import de.bigboot.ggtools.fang.utils.formatCommandHelp
import de.bigboot.ggtools.fang.utils.formatCommandTree
import kotlinx.coroutines.reactive.awaitFirst

class Root : CommandGroupSpec("", "") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Admin())
        group(Queue())
        group(Server())
        group(Match())

        command("help", "show this help") {
            onCall {
                channel().createEmbedCompat {
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
                }.awaitFirst()
            }
        }

        command("commands", "Show all available commands") {
            onCall {
                channel().createEmbedCompat {
                    title("Commands")
                    description("```\n${formatCommandTree(commands.values)}\n```")
                }.awaitFirst()
            }
        }
    }
}
