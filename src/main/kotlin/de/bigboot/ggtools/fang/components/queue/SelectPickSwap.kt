package de.bigboot.ggtools.fang.components.queue

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.SelectMenu
import java.util.*
import de.bigboot.ggtools.fang.utils.awaitSingle

class SelectPickSwap(val matchId: UUID, private val players: List<Pair<String, Long>>, val team: Boolean):
    QueueComponentSpec {
    override fun id() = "$PREFIX:${matchId}:${team.toString()}"
    override fun component(): ActionComponent = SelectMenu.of(
        id(),
        players.map { SelectMenu.Option.of(it.first, it.second.toString()) }
    ).withMinValues(1).withMaxValues(1).withPlaceholder("Pick a person")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:SELECT:PICK_SWAP"
        private val ID_REGEX = Regex("$PREFIX:([^:]+):([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (matchId, team) -> SelectPickSwap(UUID.fromString(matchId), listOf(), team.toBoolean()) }
    }
}
