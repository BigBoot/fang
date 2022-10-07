@file:Suppress("ConstructorParameterNaming")

package de.bigboot.ggtools.fang

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.tomlj.Toml
import java.nio.file.Paths

@JsonClass(generateAdapter = true)
data class EmojisConfig(
    val accept: String = "\uD83D\uDC4D",
    val deny: String = "\uD83D\uDC4E",
    val match_finished: String = "\uD83C\uDFC1",
    val match_drop: String = "\uD83D\uDC4E",
    val queue_empty: String = "\uD83D\uDE22",
    val join_queue: String = "\uD83D\uDC4D",
    val leave_queue: String = "\uD83D\uDC4E",
    val dm_notifications_enabled: String = "\uD83D\uDD14",
    val dm_notifications_disabled: String = "\uD83D\uDD15",
    val server_pref_none: String = "\uD83C\uDDFA\uD83C\uDDF3",
    val server_pref_na: String = "\uD83C\uDDFA\uD83C\uDDF8",
    val server_pref_eu: String = "\uD83C\uDDEA\uD83C\uDDFA",
)

@JsonClass(generateAdapter = true)
data class DatabaseConfig(
    val driver: String = "org.h2.Driver",
    val url: String = "jdbc:h2:./fang",
    val user: String = "",
    val pass: String = "",
)

@JsonClass(generateAdapter = true)
data class QueueConfig(
    val name: String,
    val channel: String,
    val enable_highscore: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class BotConfig(
    val token: String,
    val prefix: String = "!",
    val accept_timeout: Int = 120,
    val mapvote_time: Int = 30,
    val statusupdate_poll_rate: Long = 2000L,
    val required_players: Int = 10,
    val log_level: String = "info",
    val queues: List<QueueConfig> = listOf(),
    val highscore_channel: String = "",
)

@JsonClass(generateAdapter = true)
data class PermissionConfig(
    val default_group_name: String = "default",
    val default_group_permissions: List<String> = listOf(),
    val admin_group_name: String = "admin",
    val admin_group_permissions: List<String> = listOf("*"),
)

@JsonClass(generateAdapter = true)
data class EmuConfig(
    val url: String? = null,
    val api_key: String? = null,
)

@JsonClass(generateAdapter = true)
data class RootConfig(
    val bot: BotConfig,
    val database: DatabaseConfig = DatabaseConfig(),
    val emojis: EmojisConfig = EmojisConfig(),
    val permissions: PermissionConfig = PermissionConfig(),
    val emu: EmuConfig = EmuConfig(),
)

private val moshi = Moshi.Builder().build()
private val toml = Toml.parse(Paths.get(System.getProperty("user.dir")).resolve("config.toml"))
val Config = moshi.adapter(RootConfig::class.java).fromJson(toml.toJson())!!
