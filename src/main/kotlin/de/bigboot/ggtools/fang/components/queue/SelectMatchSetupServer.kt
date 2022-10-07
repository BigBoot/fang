package de.bigboot.ggtools.fang.components.queue

import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.SelectMenu
import java.util.*

class SelectMatchSetupServer(val matchId: UUID, private val servers: Set<String>, private val default: String? = null):
    QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}"
    override fun component(): ActionComponent = SelectMenu.of(
        id(),
        servers.map { SelectMenu.Option.of(it, it).withDefault(it == default) }
    ).withMinValues(1).withMaxValues(1).withPlaceholder("Select server")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:SELECT:MATCH_SETUP_SERVER"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId) -> SelectMatchSetupServer(UUID.fromString(matchId), emptySet()) }
    }
}