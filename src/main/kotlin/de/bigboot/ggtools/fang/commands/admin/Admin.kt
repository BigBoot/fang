package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.group.Group
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.system.exitProcess

class Admin : CommandGroupSpec("admin", "Admin commands") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Server())
        group(Group())
        
         command("restart", "restart the bot") {
            onCall {
                channel().createMessage { message ->
                    message.setContent("I'm sorry for disappointing you")
                }.awaitSingle()

                exitProcess(0)
            }
        }

        command("score", "restart the bot") {
            arg("player1", "player1 - won")
            arg("player2", "player2 - won")
            arg("player3", "player3 - won")
            arg("player4", "player4 - won")
            arg("player5", "player5 - won")
            arg("player6", "player6 - lost")
            arg("player7", "player7 - lost")
            arg("player8", "player8 - lost")
            arg("player9", "player9 - lost")
            arg("player10", "player10 - lost")

            onCall {
                val player1 = args["player1"].dropLast(1).drop(2)
                val player2 = args["player2"].dropLast(1).drop(2)
                val player3 = args["player3"].dropLast(1).drop(2)
                val player4 = args["player4"].dropLast(1).drop(2)
                val player5 = args["player5"].dropLast(1).drop(2)
                val player6 = args["player6"].dropLast(1).drop(2)
                val player7 = args["player7"].dropLast(1).drop(2)
                val player8 = args["player8"].dropLast(1).drop(2)
                val player9 = args["player9"].dropLast(1).drop(2)
                val player10 = args["player10"].dropLast(1).drop(2)

                
            }
        }
    }
}
