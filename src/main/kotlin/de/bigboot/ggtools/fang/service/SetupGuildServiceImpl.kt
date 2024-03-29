package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.guild.GuildCreateEvent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.tinylog.Logger
import reactor.core.publisher.Mono
import java.lang.Exception
import java.util.concurrent.CompletableFuture

class SetupGuildServiceImpl : AutostartService, SetupGuildService, KoinComponent {
    private val client: GatewayDiscordClient by inject()
    private val queueMessages = HashMap<String, CompletableFuture<SetupGuildService.QueueMessage>>()
    private val changelogService by inject<ChangelogService>()

    init {
        client.eventDispatcher.on<GuildCreateEvent>()
            .onEachSafe(this::handleGuildCreateEvent)
            .launch()
    }

    private suspend fun handleGuildCreateEvent(event: GuildCreateEvent) {
        Logger.info { "Joined Guild: ${event.guild.name}" }

        for (queue in Config.bot.queues) {
            val future = queueMessages.getOrPut(queue.name) { CompletableFuture<SetupGuildService.QueueMessage>() }

            val queueChannel = event.guild.channels
                .flow()
                .firstOrNull { it.name == queue.channel } as? MessageChannel

            if (queueChannel != null) {
                Logger.info { "Found ${queue.name} queue channel! Starting cleanup!" }

                queueChannel.clean()

                val queueMsg = queueChannel.createMessageCompat {
                    addEmbedCompat {
                        description("...")
                    }
                }.awaitSingle()

                queueChannel.createMessageCompat {
                    @Suppress("MagicNumber")
                    content("-".repeat(32))
                }.awaitSafe()

                future.complete(SetupGuildService.QueueMessage(queueMsg.id, queueChannel.id))
            } else {
                future.completeExceptionally(Exception("Unable to find ${queue.channel} channel"))
            }
        }

        val secretChannel = event.guild.channels
            .flow()
            .firstOrNull { it.name == "host-update" } as? MessageChannel

        if (secretChannel != null) {
            val changelog = changelogService.changelog
            Logger.info { "Found secret channel! Posting changelog: ${changelog.isNotEmpty()}!" }

            if (changelog.isNotEmpty()) {
                secretChannel.createEmbedCompat {
                    title("I'm even better now!")
                    addField("Changes", changelog.joinToString("\n") { change -> "• $change" }, false)
                }.awaitSafe()
            }
        }
    }

    override suspend fun getQueueMessage(queue: String): SetupGuildService.QueueMessage {
        return queueMessages.getOrPut(queue) { CompletableFuture<SetupGuildService.QueueMessage>() }
            .let { Mono.fromFuture(it) }.awaitSingle()
    }
}
