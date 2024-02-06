package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.utils.Creatures
import de.bigboot.ggtools.fang.utils.Maps
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.SelectMenu
import java.util.*

class SelectVoteMap(val matchId: UUID):
    QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = SelectMenu.of(
        id(),
        Maps.ENABLED.map { SelectMenu.Option.of(it.name, it.id) }
    ).withPlaceholder("Select map")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:SELECT:MATCH_VOTE_MAP"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> SelectVoteMap(UUID.fromString(matchId)) }
    }
}