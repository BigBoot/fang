package de.bigboot.ggtools.fang.components.queue

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.*

data class ButtonAccept(val matchId: UUID, val dropper: Snowflake?): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${if (dropper != null) dropper.asLong() else ""}"
    override fun component(): ActionComponent = Button.success(id(), "Accept")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:ACCEPT"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)?")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, dropper) -> ButtonAccept(UUID.fromString(matchId), if (dropper != "") Snowflake.of(dropper) else null) }
    }
}
