package de.bigboot.ggtools.fang.components.giveaway

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.reaction.ReactionEmoji
import java.util.*

data class ButtonConfirm(val giveaway: UUID): GiveawayComponentSpec {
    override fun id() = "$PREFIX:${giveaway}"
    override fun component(): ActionComponent = Button.success(id(), ReactionEmoji.unicode("\u2714"), "Confirm")

    companion object {
        private val PREFIX = "${GiveawayComponentSpec.ID_PREFIX}:BUTTON:CONFIRM"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let {
            (giveaway) -> ButtonConfirm(UUID.fromString(giveaway))
        }
    }
}