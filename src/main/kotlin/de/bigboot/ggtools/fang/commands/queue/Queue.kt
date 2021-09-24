package de.bigboot.ggtools.fang.commands.queue

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.HighscoreService
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.utils.findMember
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.inject
import java.time.Duration

class Queue : CommandGroupSpec("queue", "Commands for matchmaking") {
    private val matchService by inject<MatchService>()
    private val highscoreService by inject<HighscoreService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("list", "show all available queues") {
            onCall {
                channel().createMessage { msg ->
                    msg.addEmbed { embed ->
                        embed.setTitle("Available queues:")
                        embed.setDescription(Config.bot.queues.joinToString("\n") { it.name })
                    }
                }.awaitSingle()
            }
        }

        command("show", "show players currently in the queue") {
            arg("queue", "the name of the queue")

            onCall {
                val queue = args["queue"]

                channel().createMessage {
                    it.addEmbed { embed ->
                        embed.setTitle("${matchService.getNumPlayers(queue)} players waiting in queue")
                        embed.setDescription(matchService.printQueue(queue))
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
                    channel().createEmbed { embed ->
                        embed.setDescription("User ${args["player"]} not found")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.leave(queue, user.id.asLong())

                channel().createEmbed { embed ->
                    embed.setDescription("<@${user.id.asString()}> removed from the queue.")
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
                    channel().createEmbed { embed ->
                        embed.setDescription("Sorry, I didn't understand that.")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.request(queue, Snowflake.of(message.userData.id()).asLong(), players)
            }
        }

        command("highscores", "Show the queue highscores") {
            onCall {
                channel().createEmbed { embed ->
                    embed.setTitle("Queue Highscores")
                    embed.setDescription(highscoreService.printHighscore())
                }.awaitSingle()
            }
        }
    }
}
