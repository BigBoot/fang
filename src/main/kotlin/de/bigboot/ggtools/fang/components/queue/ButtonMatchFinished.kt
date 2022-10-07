package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.asReaction
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.UUID

data class ButtonMatchFinished(val matchId: UUID): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = Button.success(id(), Config.emojis.match_finished.asReaction(), "Match finished")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:MATCH_FINISHED"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> ButtonMatchFinished(UUID.fromString(matchId)) }
    }
}