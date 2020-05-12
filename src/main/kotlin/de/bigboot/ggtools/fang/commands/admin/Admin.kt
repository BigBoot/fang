package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Server
import de.bigboot.ggtools.fang.commands.admin.group.Group

class Admin : CommandGroupSpec("admin", "Admin commands") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Server())
        group(Group())
    }
}