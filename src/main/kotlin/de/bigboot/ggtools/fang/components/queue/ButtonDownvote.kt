package de.bigboot.ggtools.fang.components.queue

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.*
import de.bigboot.ggtools.fang.utils.asReaction

data class ButtonDownvote(val matchId: UUID, val suggester: Snowflake): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${suggester.asLong()}"
    override fun component(): ActionComponent = Button.danger(id(), "👎".asReaction(), "")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:DOWNVOTE"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, suggester) -> ButtonDownvote(UUID.fromString(matchId), Snowflake.of(suggester) ) }
    }
}
