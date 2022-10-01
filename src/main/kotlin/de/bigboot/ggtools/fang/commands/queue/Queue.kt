package de.bigboot.ggtools.fang.commands.queue

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.utils.addEmbedCompat
import de.bigboot.ggtools.fang.utils.createEmbedCompat
import de.bigboot.ggtools.fang.utils.createMessageCompat
import de.bigboot.ggtools.fang.utils.findMember
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Queue : CommandGroupSpec("queue", "Commands for matchmaking") {
    private val matchService by inject<MatchService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("list", "show all available queues") {
            onCall {
                channel().createMessageCompat {
                    addEmbedCompat {
                        title("Available queues:")
                        description(Config.bot.queues.joinToString("\n") { it.name })
                    }
                }.awaitSingle()
            }
        }

        command("show", "show players currently in the queue") {
            arg("queue", "the name of the queue")

            onCall {
                val queue = args["queue"]

                channel().createMessageCompat {
                    addEmbedCompat {
                        title("${matchService.getNumPlayers(queue)} players waiting in queue")
                        description(matchService.printQueue(queue))
                    }
                }.awaitSingle()
            }
        }

        command("kick", "Kick a player from the queue") {
            arg("player", "the player to kick")
            arg("queue", "the name of the queue")

            onCall {
                val user = guild().findMember(args["player"])
                val queue = args["queue"]

                if (user == null) {
                    channel().createEmbedCompat {
                        description("User ${args["player"]} not found")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.leave(queue, user.id.asLong())

                channel().createEmbedCompat {
                    description("<@${user.id.asString()}> removed from the queue.")
                }.awaitSingle()
            }
        }

        command("request", "Request a match with less then 10 players") {
            arg("players", "the number of players")
            arg("queue", "the name of the queue")

            onCall {
                val players = args["players"].toIntOrNull()
                val queue = args["queue"]

                if (players == null) {
                    channel().createEmbedCompat {
                        description("Sorry, I didn't understand that.")
                    }.awaitSingle()
                    return@onCall
                }

                if (players <= 0) {
                    channel().createEmbedCompat {
                            description("You need at least 1 player to request a match!")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.request(queue, Snowflake.of(message.userData.id()).asLong(), players)
            }
        }
    }
}
