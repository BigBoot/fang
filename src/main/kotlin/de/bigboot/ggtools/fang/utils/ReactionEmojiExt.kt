package de.bigboot.ggtools.fang.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji

private val emojiRegex = Regex("""<(a?):(\w+):(\d+)>""")
fun String.asReaction(): ReactionEmoji = emojiRegex.find(this)?.let {
    val (animated, name, id) = it.destructured
    ReactionEmoji.custom(Snowflake.of(id), name, animated.isNotBlank())
} ?: ReactionEmoji.unicode(this)
