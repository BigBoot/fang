package de.bigboot.ggtools.fang.components.queue

import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.*
import de.bigboot.ggtools.fang.utils.asReaction

data class ButtonUpvote(val matchId: UUID, val suggester: Snowflake): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${suggester.asLong()}"
    override fun component(): ActionComponent = Button.success(id(), "👍".asReaction(), "")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:UPVOTE"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, suggester) -> ButtonUpvote(UUID.fromString(matchId), Snowflake.of(suggester) ) }
    }
}
