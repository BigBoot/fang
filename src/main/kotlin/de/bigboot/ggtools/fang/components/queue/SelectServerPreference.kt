package de.bigboot.ggtools.fang.components.queue

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.asReaction
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.SelectMenu

class SelectServerPreference(val queue: String): QueueComponentSpec {
    override fun id() = "$PREFIX:${queue}"
    override fun component(): ActionComponent = SelectMenu.of(
        id(),
        SelectMenu.Option.of("EU", "EU").withEmoji(Config.emojis.server_pref_eu.asReaction()),
        SelectMenu.Option.of("NA", "NA").withEmoji(Config.emojis.server_pref_na.asReaction()),
    ).withMaxValues(2).withPlaceholder("Select server locations")

    companion object {
        private val PREFIX = "${QueueComponentSpec.ID_PREFIX}:SELECT:PREFERENCES_SERVER"
        private val ID_REGEX = Regex("$PREFIX:([^:]+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> SelectServerPreference(queue) }
    }
}