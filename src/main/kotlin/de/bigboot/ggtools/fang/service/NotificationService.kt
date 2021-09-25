package de.bigboot.ggtools.fang.service

interface NotificationService {
    suspend fun toggleDirectMessageNotifications(snowflake: Long, enabled: Boolean? = null): Boolean
    fun getDirectMessageNotificationsEnabled(snowflake: Long): Boolean
    suspend fun notify(player: Long, channel: Long)
}