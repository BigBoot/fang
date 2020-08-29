package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.VoiceStateUpdateEvent
import kotlinx.coroutines.flow.filter
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.time.seconds

class QueueJoinNotificationService : AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val matchService by inject<MatchService>()

    init {
        client.eventDispatcher.on<VoiceStateUpdateEvent>()
            .filter { !it.old.isPresent }
            .filter { it.current.channel.await()?.name == "Match Hub" }
            .onEachSafe(this::handleMatchHubJoined)
            .launch()
    }

    private suspend fun handleMatchHubJoined(event: VoiceStateUpdateEvent) {
        val userId = event.current.userId.asLong()

        if (!matchService.isPlayerQueued(userId)) {
            val msgChannel = event
                .current
                .guild
                .awaitSingle()
                .channels
                .filter { it.name == "match-queue" }
                .await()

            matchService.join(userId)

            if (msgChannel is MessageChannel) {
                val msg = msgChannel.createMessage {
                    it.setContent("Hey <@$userId>. I've noticed you joined the Match Hub voice channel, so I've added you to the queue. Don't forget to leave the queue when you're not available anymore.")
                }.awaitSingle()

                msg.deleteAfter(60.seconds)
            }
        }
    }
}