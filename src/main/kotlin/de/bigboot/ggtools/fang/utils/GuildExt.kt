package de.bigboot.ggtools.fang.utils

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.reactive.awaitFirstOrNull

private val userRegex = Regex("""<@!(\d+)>""")
suspend fun Guild.findMember(name: String): Member? {
    return when {
        userRegex.matches(name) -> getMemberById(Snowflake.of(userRegex.find(name)!!.groupValues[1])).awaitFirstOrNull()
        else -> members
            .filter {
                it.username.toLowerCase() == name.toLowerCase() || it.displayName.toLowerCase() == name.toLowerCase()
            }.awaitFirstOrNull()
    }
}

suspend fun Guild.findChannel(name: String): MessageChannel? {
    return channels
        .filter { it is MessageChannel }
        .filter { it.name == name }
        .awaitFirstOrNull() as? MessageChannel
}
