package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.Notification
import de.bigboot.ggtools.fang.db.Notifications
import de.bigboot.ggtools.fang.utils.awaitSafe
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject

class NotificationServiceImpl : NotificationService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val database by inject<Database>()

    override suspend fun toggleDirectMessageNotifications(snowflake: Long, enabled: Boolean?) = transaction(database) {
        val notification = Notification.find { Notifications.snowflake eq snowflake }
            .firstOrNull() ?: Notification.new { this.snowflake = snowflake }

        val result = enabled ?: !notification.directMessage

        notification.directMessage = result

        result
    }

    override fun getDirectMessageNotificationsEnabled(snowflake: Long) = transaction(database) {
        Notification.find { (Notifications.snowflake eq snowflake) and (Notifications.directMessage eq false) }.empty()
    }

    override suspend fun notify(player: Long, channel: Long) {
        if(getDirectMessageNotificationsEnabled(player)) {
            client.getUserById(Snowflake.of(player)).awaitSafe()
                ?.privateChannel?.awaitSafe()
                ?.createMessage() { msg ->
                    msg.setContent("There's a match ready for you, please head over to <#${channel}>")
                }?.awaitSafe()
        }
    }

}