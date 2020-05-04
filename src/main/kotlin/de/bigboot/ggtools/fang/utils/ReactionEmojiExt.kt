package de.bigboot.ggtools.fang.utils

import discord4j.core.`object`.reaction.ReactionEmoji

fun ReactionEmoji.print() = when (this) {
    is ReactionEmoji.Custom -> "<:$name:${id.asString()}>"
    is ReactionEmoji.Unicode -> raw
    else -> ""
}