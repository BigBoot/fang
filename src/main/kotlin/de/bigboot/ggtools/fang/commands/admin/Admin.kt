package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.group.Group
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.system.exitProcess

class Admin : CommandGroupSpec("score", "Admin commands") {
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
    }
}
