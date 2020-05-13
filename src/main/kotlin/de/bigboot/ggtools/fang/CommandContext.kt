package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.service.MatchService
import de.bigboot.ggtools.fang.service.PermissionService
import de.bigboot.ggtools.fang.service.ServerService
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.KoinComponent
import org.koin.core.inject

data class CommandContext(
    val args: Arguments,
    val message: Message
) : KoinComponent {
    val serverService: ServerService by inject()
    val matchService: MatchService by inject()
    val permissionService: PermissionService by inject()
    private val _commands: Commands by inject()
    val commands = _commands.commands

    suspend fun channel(): MessageChannel = message.channel.awaitSingle()
    suspend fun guild(): Guild = message.guild.awaitSingle()

    class Arguments(private val arguments: Map<String, String>) {
        operator fun get(key: String) = arguments.getValue(key)
        fun optional(key: String) = arguments[key]
    }
}
