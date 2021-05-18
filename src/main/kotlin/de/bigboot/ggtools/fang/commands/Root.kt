package de.bigboot.ggtools.fang.commands

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Admin
import de.bigboot.ggtools.fang.commands.queue.Queue
import de.bigboot.ggtools.fang.commands.server.Server
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.utils.formatCommandHelp
import de.bigboot.ggtools.fang.utils.formatCommandTree
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitFirst
import org.koin.core.inject

class Root : CommandGroupSpec("", "") {
    override val build: CommandGroupBuilder.() -> Unit = {
        val matchService by inject<MatchService>()

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

        command("skill", "Shows your skill") {
            onCall {
                val user = Snowflake.of(message.userData.id()).asLong();
                channel().createEmbed { embed ->
                    embed.setTitle("Your current skill level is: ${matchService.getPlayerSkill(user)}")
                }.awaitFirst()
            }
        }
    }
}
