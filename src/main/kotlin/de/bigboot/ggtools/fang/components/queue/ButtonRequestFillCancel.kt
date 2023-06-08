package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.asReaction
import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.UUID

data class ButtonRequestFillCancel(val matchId: UUID, val dropper: Snowflake): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${dropper.asLong()}"
    override fun component(): ActionComponent = Button.primary(id(), "Cancel")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:REQUEST_FILL_CANCEL"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, dropper) -> ButtonRequestFillCancel(UUID.fromString(matchId), Snowflake.of(dropper)) }
    }
}
