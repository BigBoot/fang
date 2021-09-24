package de.bigboot.ggtools.fang.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji

private val emojiRegex = Regex("""<(a?):(\w+):(\d+)>""")
fun String.asReaction(): ReactionEmoji = emojiRegex.find(this)?.let {
    val (animated, name, id) = it.destructured
    ReactionEmoji.custom(Snowflake.of(id), name, animated.isNotBlank())
} ?: ReactionEmoji.unicode(this)

fun Int.asReaction(): ReactionEmoji = ReactionEmoji.unicode(when(this) {
    0 -> "0️⃣"
    1 -> "1️⃣"
    2 -> "2️⃣"
    3 -> "3️⃣"
    4 -> "4️⃣"
    5 -> "5️⃣"
    6 -> "6️⃣"
    7 -> "7️⃣"
    8 -> "8️⃣"
    9 -> "9️⃣"
    10 -> "\uD83D\uDD1F"
    else -> "❔"
})

fun ReactionEmoji.print(): String = when (this) {
    is ReactionEmoji.Unicode -> this.raw
    is ReactionEmoji.Custom -> this.asFormat()
    else -> this.toString()
}