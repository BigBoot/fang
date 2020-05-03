package de.bigboot.ggtools.fang

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.reactive.awaitSingle


data class CommandContext(
    val args: Arguments,
    val message: Message,
    val serverManager: ServerManager,
    val matchManager: MatchManager,
    val permissionManager: PermissionManager
) {
    suspend fun channel(): MessageChannel = message.channel.awaitSingle()
    suspend fun guild(): Guild = message.guild.awaitSingle()

    class Arguments(private val arguments: Map<String, String>) {
        operator fun get(key: String) = arguments.getValue(key)
        fun optional(key: String) = arguments[key]
    }
}