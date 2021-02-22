package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.guild.GuildCreateEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.tinylog.Logger
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

class SetupGuildServiceImpl : AutostartService, SetupGuildService, KoinComponent {
    private val client: GatewayDiscordClient by inject()
    private val queueMessage = CompletableFuture<SetupGuildService.QueueMessage>()
    private val changelogService by inject<ChangelogService>()

    init {
        client.eventDispatcher.on<GuildCreateEvent>()
            .onEachSafe(this::handleGuildCreateEvent)
            .launch()
    }

    private suspend fun handleGuildCreateEvent(event: GuildCreateEvent) {
        Logger.info { "Joined Guild: ${event.guild.name}" }

        val queueChannel = event.guild.channels
            .flow()
            .firstOrNull { it.name == "match-queue" } as? MessageChannel

        if (queueChannel != null) {
            Logger.info { "Found queue channel! Starting cleanup!" }

            queueChannel.clean()

            val queueMsg = queueChannel.createMessage {
                it.setEmbed {}
            }.awaitSingle()

            queueMsg.addReaction(Config.emojis.join_queue.asReaction()).await()
            queueMsg.addReaction(Config.emojis.leave_queue.asReaction()).await()

            queueChannel.createMessage {
                @Suppress("MagicNumber")
                it.setContent("-".repeat(32))
            }.awaitSafe()

            queueMessage.complete(SetupGuildService.QueueMessage(queueMsg.id, queueChannel.id))
        }

        val secretChannel = event.guild.channels
            .flow()
            .firstOrNull { it.name == "secret-hub" } as? MessageChannel

        if(secretChannel != null) {
            val changelog = changelogService.changelog
            Logger.info { "Found secret channel! Posting changelog: ${changelog.isNotEmpty()}!" }

            if(changelog.isNotEmpty()) {
                secretChannel.createEmbed {
                    it.setTitle("I'm even better now!")
                    it.addField("Changes", changelog.joinToString("\n") { change -> "• $change" }, false)
                }.awaitSafe()
            }
        }
    }

    override suspend fun getQueueMessage(): SetupGuildService.QueueMessage {
        return Mono.fromFuture(queueMessage).awaitSingle()
    }
}
