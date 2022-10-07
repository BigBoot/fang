package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.asReaction
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.UUID

data class ButtonMatchDrop(val matchId: UUID): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = Button.danger(id(), Config.emojis.match_drop.asReaction(), "Drop out")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:MATCH_DROP"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> ButtonMatchDrop(UUID.fromString(matchId)) }
    }
}