package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button

data class ButtonLeave(val queue: String): QueueComponentSpec {
    override fun id() = "$PREFIX:${queue}"
    override fun component(): ActionComponent = Button.danger(id(), "Leave")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:LEAVE"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonLeave(queue) }
    }
}