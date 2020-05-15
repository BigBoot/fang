package de.bigboot.ggtools.fang.utils

import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.rest.util.Snowflake

private val emojiRegex = Regex("""<(?<animated>a?):(?<id>\d+):(?<name>\w+)>""")
fun String.asReaction(): ReactionEmoji = emojiRegex.find(this)?.let {
    val (animated, id, name) = it.destructured
    ReactionEmoji.custom(Snowflake.of(id), name, animated.isNotBlank())
} ?: ReactionEmoji.unicode(this)
