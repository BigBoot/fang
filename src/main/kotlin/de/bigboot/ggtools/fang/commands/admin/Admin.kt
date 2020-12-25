package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.commands.admin.group.Group
import de.bigboot.ggtools.fang.utils.parseArgs
import kotlinx.coroutines.reactive.awaitSingle

class Admin : CommandGroupSpec("admin", "Admin commands") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Server())
        group(Group())
        
         command("restart", "restart the bot") {
            onCall {
                @Suppress("BlockingMethodInNonBlockingContext")
                ProcessBuilder(parseArgs(Config.bot.restart_command)).start()

                channel().createMessage { message ->
                    message.setContent("I'm sorry for disappointing you")
                }.awaitSingle()
            }
        }
    }
}
