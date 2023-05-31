package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.asReaction
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.UUID

data class ButtonMatchUnranked(val matchId: UUID, val final: Boolean): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${final}"
    override fun component(): ActionComponent = Button.primary(id(), Config.emojis.match_unranked.asReaction(), "Set unranked")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:MATCH_UNRANKED"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):(true|false)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, final) -> ButtonMatchUnranked(UUID.fromString(matchId), final.toBoolean()) }
    }
}
