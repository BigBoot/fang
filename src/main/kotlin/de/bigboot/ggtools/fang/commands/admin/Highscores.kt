package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.service.HighscoreService
import de.bigboot.ggtools.fang.utils.findMember
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.inject

class Highscores : CommandGroupSpec("highscores", "Commands for managing highscores") {
    private val highscoreService by inject<HighscoreService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("reset_all", "Resets all highscores") {

            onCall {
                highscoreService.resetAll()

                channel().createEmbed {
                    it.setDescription("Highscores cleared")
                }.awaitSingle()
            }
        }

        command("reset", "Reset highscore for a player") {
            arg("player", "The player to reset")

            onCall {
                val player = guild().findMember(args["player"])

                if (player == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("User ${args["player"]} not found")
                    }.awaitSingle()
                    return@onCall
                }

                highscoreService.reset(player.id.asLong())

                channel().createEmbed {
                    it.setDescription("Highscore cleared")
                }.awaitSingle()
            }
        }
    }
}
