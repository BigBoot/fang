package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.Button
import java.util.*

data class ButtonMapVote(val matchId: UUID, val map: String): QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${map}"
    override fun component(): ActionComponent = Button.primary(id(), when(map) {
        "canyon" -> "Ghost Reef"
        "mistforge" -> "Sanctum Falls"
        "valley" -> "Sirens Strand"
        else -> map
    })

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:BUTTON:MAPVOTE"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, level) -> ButtonMapVote(UUID.fromString(matchId), level) }
    }
}