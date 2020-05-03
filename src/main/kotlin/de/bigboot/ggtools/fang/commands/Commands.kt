package de.bigboot.ggtools.fang.commands

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.Admin
import de.bigboot.ggtools.fang.commands.queue.Queue

class Commands : CommandGroupSpec("", "") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Admin())
        group(Queue())
        group(Server())
    }
}

