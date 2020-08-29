package de.bigboot.ggtools.fang.service

import discord4j.common.util.Snowflake

interface SetupGuildService {
    data class QueueMessage(val msgId: Snowflake, val channelId: Snowflake)

    suspend fun getQueueMessage(): QueueMessage
}