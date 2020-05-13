package de.bigboot.ggtools.fang.commands.queue

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle

class Queue : CommandGroupSpec("queue", "Commands for matchmaking") {
    override val build: CommandGroupBuilder.() -> Unit = {
        command("show", "show players currently in the queue") {
            onCall {
                channel().createMessage {
                    it.setEmbed { embed ->
                        embed.setTitle("Players waiting in queue")
                        embed.setDescription(matchService.getPlayers().joinToString("\n") { player ->
                            "<@$player>"
                        })
                    }
                }.awaitSingle()
            }
        }

        command("join", "join the queue") {
            onCall {
                if (matchService.join(Snowflake.of(message.userData.id()).asLong())) {
                    channel().createEmbed { embed ->
                        embed.setDescription("<@${message.userData.id()}> joined the queue.")
                    }.awaitSingle()
                }
            }
        }

        command("leave", "leave the queue") {
            onCall {
                if (matchService.leave(Snowflake.of(message.userData.id()).asLong())) {
                    channel().createEmbed { embed ->
                        embed.setDescription("<@${message.userData.id()}> left the queue.")
                    }.awaitSingle()
                }
            }
        }

        command("pop", "Force the queue to pop even of there aren't enough players") {
            onCall {
                matchService.force()
            }
        }

        command("kick", "Kick a player from the queue") {
            arg("player", "the player to kick")

            onCall {
                val user = guild().members
                    .filter { it.displayName == args["player"] }
                    .awaitFirstOrNull()

                if (user == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("User ${args["player"]} not found")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.leave(user.id.asLong())

                channel().createEmbed { embed ->
                    embed.setDescription("<@${user.id.asString()}> removed from the queue.")
                }.awaitSingle()
            }
        }

        command("request", "Request a match with less then 10 players") {
            arg("players", "the number of players")

            onCall {
                val players = args["players"].toIntOrNull()

                if (players == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("Sorry, I didn't understand that.")
                    }.awaitSingle()
                    return@onCall
                }

                matchService.request(Snowflake.of(message.userData.id()).asLong(), players)
            }
        }
    }
}
