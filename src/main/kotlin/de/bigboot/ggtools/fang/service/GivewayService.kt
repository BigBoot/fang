package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.utils.Error
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import java.time.Instant
import java.util.*

interface GivewayService {
    data class Prize(val emoji: ReactionEmoji, val text: String, val count: Int, val id: UUID = UUID.randomUUID())
    enum class ConfirmationStatus { Waiting, Confirmed, Missed }
    data class Winner(val snowflake: Snowflake, val status: ConfirmationStatus)

    suspend fun createGiveaway(channel: Snowflake, title: String, description: String, end: Instant, maxJoinDate: Instant?, prizes: List<Prize>): Snowflake
    suspend fun enterGiveaway(snowflake: Snowflake, giveaway: UUID, prize: UUID): Error?
    suspend fun confirm(snowflake: Snowflake, giveaway: UUID): Error?
    suspend fun createGiveawayMessage(
        channel: MessageChannel,
        title: String,
        description: String,
        end: Instant,
        maxJoindDate: Instant?,
        prizes: List<Prize>,
        entries: List<Int>,
        giveaway: UUID = UUID.randomUUID(),
    ): Message

    suspend fun getGiveawayByMessageId(messageId: Snowflake): UUID?

    suspend fun getPrizesByGiveawayId(id: UUID): List<Prize>

    suspend fun getWinners(id: UUID): List<Pair<Prize, List<Winner>>>
}