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
)

@JsonClass(generateAdapter = true)
data class DatabaseConfig(
    val driver: String = "org.h2.Driver",
    val url: String = "jdbc:h2:./fang",
    val user: String = "",
    val pass: String = "",
)

@JsonClass(generateAdapter = true)
data class BotConfig(
    val token: String,
    val prefix: String = "!",
    val accept_timeout: Int = 120,
    val statusupdate_poll_rate: Long = 2000L,
    val required_players: Int = 10,
    val log_level: String = "info",
    val start_score: Int = 500,
    val score_increase: Int = 5,
)

@JsonClass(generateAdapter = true)
data class PermissionConfig(
    val default_group_name: String = "default",
    val default_group_permissions: List<String> = listOf(),
    val admin_group_name: String = "admin",
    val admin_group_permissions: List<String> = listOf("*"),
)

@JsonClass(generateAdapter = true)
data class RootConfig(
    val bot: BotConfig,
    val database: DatabaseConfig = DatabaseConfig(),
    val emojis: EmojisConfig = EmojisConfig(),
    val permissions: PermissionConfig = PermissionConfig(),
)

private val moshi = Moshi.Builder().build()
private val toml = Toml.parse(Paths.get(System.getProperty("user.dir")).resolve("config.toml"))
val Config = moshi.adapter(RootConfig::class.java).fromJson(toml.toJson())!!
