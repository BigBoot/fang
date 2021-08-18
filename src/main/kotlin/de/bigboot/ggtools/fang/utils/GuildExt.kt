package de.bigboot.ggtools.fang.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.util.Locale

private val userRegex = Regex("""<@!(\d+)>""")
suspend fun Guild.findMember(name: String): Member? {
    return when {
        userRegex.matches(name) -> getMemberById(Snowflake.of(userRegex.find(name)!!.groupValues[1])).awaitFirstOrNull()
        else -> members
            .filter {
                it.username.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) || it.displayName.lowercase(
                    Locale.getDefault()
                ) == name.lowercase(Locale.getDefault())
            }.awaitFirstOrNull()
    }
}

suspend fun Guild.findChannel(name: String): MessageChannel? {
    return channels
        .filter { it is MessageChannel }
        .filter { it.name == name }
        .awaitFirstOrNull() as? MessageChannel
}
