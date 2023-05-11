package de.bigboot.ggtools.fang

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class CommandContext(
    val args: Arguments,
    val channel: MessageChannel,
    val guild: Guild,
    var author: User
) : KoinComponent {
    private val _commands: Commands by inject()
    val commands = _commands.commands

    suspend fun channel(): MessageChannel = channel
    suspend fun guild(): Guild = guild
    suspend fun author(): User = author

    class Arguments(private val arguments: Map<String, String>) {
        operator fun get(key: String) = arguments.getValue(key)
        fun optional(key: String) = arguments[key]
    }
}
