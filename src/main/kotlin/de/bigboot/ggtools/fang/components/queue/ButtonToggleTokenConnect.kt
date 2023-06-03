package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button

class ButtonToggleTokenConnect(val queue: String): QueueComponentSpec {
    override fun id() = "$PREFIX:${queue}"
    override fun component(): ActionComponent = Button.secondary(id(), "Toggle Connect using Token")
    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:PREFERENCES_TOKEN_CONNECT"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonToggleTokenConnect(queue) }
    }
}