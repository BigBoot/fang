package de.bigboot.ggtools.fang.components.giveaway

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.reaction.ReactionEmoji
import java.util.*

data class ButtonEnter(val giveaway: UUID, val prize: UUID, val emoji: ReactionEmoji?): GiveawayComponentSpec {
    override fun id() = "$PREFIX:${giveaway}:${prize}"
    override fun component(): ActionComponent = Button.secondary(id(), emoji ?: ReactionEmoji.unicode(""))

    companion object {
        private val PREFIX = "${GiveawayComponentSpec.ID_PREFIX}:BUTTON:ENTER"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let {
            (giveaway, prize) -> ButtonEnter(UUID.fromString(giveaway), UUID.fromString(prize), null)
        }
    }
}