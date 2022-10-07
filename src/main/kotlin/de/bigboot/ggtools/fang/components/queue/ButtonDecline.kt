package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.UUID

data class ButtonDecline(val matchId: UUID): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = Button.danger(id(), "Decline")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:DECLINE"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> ButtonDecline(UUID.fromString(matchId)) }
    }
}