package de.bigboot.ggtools.fang.service

interface PreferencesService {
    data class Preferences(val dmNotifications: Boolean, val preferredServers: Set<String>, val tokenConnect: Boolean)
    data class UpdatePreferences(val dmNotifications: Boolean? = null, val preferredServers: Set<String>? = null, val tokenConnect: Boolean? = null)

    fun getPreferences(snowflake: Long): Preferences
    fun setPreferences(snowflake: Long, preferences: UpdatePreferences)
    fun toggleDirectMessageNotifications(snowflake: Long, enabled: Boolean? = null): Boolean
    fun toggleTokenConnect(snowflake: Long, enabled: Boolean? = null): Boolean
}
