package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.Preference
import de.bigboot.ggtools.fang.db.Preferences
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferencesServiceImpl : PreferencesService, KoinComponent {
    private val database by inject<Database>()
    override fun getPreferences(snowflake: Long) = transaction(database) {
        Preference.find { (Preferences.snowflake eq snowflake) }
            .firstOrNull()
            ?.let { PreferencesService.Preferences(
                dmNotifications =  it.directMessage,
                preferredServers = it.preferredServers
                    .split(",")
                    .map { server -> server.trim() }
                    .toSet(),
                tokenConnect = it.tokenConnect,
            ) }
            ?: PreferencesService.Preferences(true, setOf("NA","EU"), true)
    }

    override fun setPreferences(snowflake: Long, preferences: PreferencesService.UpdatePreferences) {
        transaction(database) {

            val entry = Preference.find { Preferences.snowflake eq snowflake }
                .firstOrNull() ?: Preference.new {

                this.snowflake = snowflake
                this.directMessage = true
                this.preferredServers = "NA,EU"
                this.tokenConnect = true
            }

            preferences.dmNotifications?.also { entry.directMessage = it }
            preferences.preferredServers?.also { entry.preferredServers = it.joinToString(",") }
            preferences.tokenConnect?.also { entry.tokenConnect = it }
        }
    }

    override fun toggleDirectMessageNotifications(snowflake: Long, enabled: Boolean?) = transaction(database) {
        val preferences = Preference.find { Preferences.snowflake eq snowflake }
            .firstOrNull() ?: Preference.new { this.snowflake = snowflake }

        val result = enabled ?: !preferences.directMessage

        preferences.directMessage = result

        result
    }

    override fun toggleTokenConnect(snowflake: Long, enabled: Boolean?): Boolean = transaction(database) {
        val preferences = Preference.find { Preferences.snowflake eq snowflake }
            .firstOrNull() ?: Preference.new { this.snowflake = snowflake }

        val result = enabled ?: !preferences.tokenConnect

        preferences.tokenConnect = result

        result
    }
}
