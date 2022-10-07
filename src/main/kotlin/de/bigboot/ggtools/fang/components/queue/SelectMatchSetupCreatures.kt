package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.utils.Creatures
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.SelectMenu
import java.util.*

class SelectMatchSetupCreatures(val matchId: UUID, private val default: Triple<String?, String?, String?> = Triple(null, null, null)):
    QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = SelectMenu.of(
        id(),
        Creatures.ALL.map { SelectMenu.Option.of(it.name, it.id).withDefault(default.toList().contains(it.id)) }
    ).withMinValues(3).withMaxValues(3).withPlaceholder("Select creatures")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:SELECT:MATCH_SETUP_CREATURES"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> SelectMatchSetupCreatures(UUID.fromString(matchId)) }
    }
}