package de.bigboot.ggtools.fang.commands.queue

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import kotlinx.coroutines.reactive.awaitSingle

class Queue : CommandGroupSpec("queue", "Commands for matchmaking") {
    override val build: CommandGroupBuilder.() -> Unit = {
        command("show", "show players currently in the queue") {
            onCall {
                channel().createMessage {
                    it.setEmbed { embed ->
                        embed.setTitle("Players waiting in queue")
                        embed.setDescription(matchManager.getPlayers().joinToString("\n") { player ->
                            "<@$player>"
                        })
                    }
                }.awaitSingle()
            }
        }

        command("join", "join the queue") {
            onCall {
                if(matchManager.join(message.userData.id())) {
                    channel().createEmbed { embed ->
                        embed.setDescription("<@${message.userData.id()}> joined the queue.")
                    }.awaitSingle()
                }
            }
        }

        command("leave", "leave the queue") {
            onCall {
                if(matchManager.leave(message.userData.id())) {
                    channel().createEmbed { embed ->
                        embed.setDescription("<@${message.userData.id()}> left the queue.")
                    }.awaitSingle()
                }
            }
        }

        command("pop", "Force the queue to pop even of there aren't enough players") {
            onCall {
                matchManager.force()
            }
        }
    }
}