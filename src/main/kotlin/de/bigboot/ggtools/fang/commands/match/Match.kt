package de.bigboot.ggtools.fang.commands.match

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.service.QueueMessageService
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Match : CommandGroupSpec("match", "Commands for matches") {
    private val matchService by inject<MatchService>()
    private val queueMessageService by inject<QueueMessageService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("swap", "shows the swap commands for the server") {
            onCall {
                val currentServer = queueMessageService.findUser(author().id.asLong());

                if (currentServer != null && currentServer.openUrl != null) {
                    channel().createMessageCompat {
                        addEmbedCompat {
                            title("Swap commands")
                            description("Leiren: `open ${currentServer.openUrl}?team=0`\nGrenn: `open ${currentServer.openUrl}?team=1`")
                        }
                    }.awaitSingle()
                }
                else {
                    channel().createMessageCompat {
                        addEmbedCompat {
                            description("You are not in a game with a server up")
                        }
                    }.awaitSingle()
                }
            }
        }
    }
}
