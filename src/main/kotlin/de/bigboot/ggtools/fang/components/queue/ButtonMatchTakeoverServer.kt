package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.*

data class ButtonMatchTakeoverServer(val matchId: UUID): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = Button.primary(id(), "Take over setup")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:MATCH_TAKEOVER_SERVER"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> ButtonMatchTakeoverServer(UUID.fromString(matchId)) }
    }
}