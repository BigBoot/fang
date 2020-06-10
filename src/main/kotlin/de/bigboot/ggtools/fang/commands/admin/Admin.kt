package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.group.Group

class Admin : CommandGroupSpec("admin", "Admin commands") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Server())
        group(Group())
        
         command("restart", "restart the bot") {
            onCall {
                @Suppress("BlockingMethodInNonBlockingContext")
                ProcessBuilder("/usr/bin/supervisorctl", "restart", "fang").start()

                channel().createMessage { message ->
                    message.setContent("I'm sorry for disappointing you")
                }.awaitSingle()
            }
        }
    }
}
