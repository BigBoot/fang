package de.bigboot.ggtools.fang.commands.queue

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.service.QueueMessageService
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Queue : CommandGroupSpec("queue", "Commands for matchmaking") {
    private val matchService by inject<MatchService>()
    private val queueMessageService by inject<QueueMessageService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("list", "show all available queues") {
            onCall {
                {
                    addEmbedCompat {
                        title("Available queues:")
                        description(Config.bot.queues.joinToString("\n") { it.name })
                    }
                }
            }
        }

        command("show", "show players currently in the queue") {
            arg("queue", "the name of the queue")

            onCall {
                val queue = args["queue"]

                {
                    addEmbedCompat {
                        queueMessageService.printQueue(queue, this)
                    }
                }
            }
        }

        command("kick", "Kick a player from the queue") {
            arg("player", "the player to kick")
            arg("queue", "the name of the queue")

            onCall {
                val user = guild().findUser(args["player"])
                val queue = args["queue"]

                if (user == null) {
                    return@onCall {addEmbedCompat{
                        description("User ${args["player"]} not found")
                    }}
                }

                matchService.leave(queue, user.id.asLong())

                return@onCall {addEmbedCompat{
                    description("<@${user.id.asString()}> removed from the queue.")
                }}
            }
        }

        command("request", "Request a match with less then 10 players") {
            arg("players", "the number of players")
            arg("queue", "the name of the queue")

            onCall {
                val players = args["players"].toIntOrNull()
                val queue = args["queue"]

                if (players == null) {
                    return@onCall {addEmbedCompat {
                        description("Sorry, I didn't understand that.")
                    }}
                }

                if (players <= 0) {
                    return@onCall {addEmbedCompat {
                            description("You need at least 1 player to request a match!")
                    }}
                }

                matchService.request(queue, Snowflake.of(author().userData.id()).asLong(), players)
                return@onCall {addEmbedCompat {
                        description("I have created the request!")
                }}
            }
        }
    }
}
