package de.bigboot.ggtools.fang.api.agent.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartRequest(
    @field:Json(name = "map")
    val map: String,

    @field:Json(name = "max_players")
    val maxPlayers: Int?,

    @field:Json(name = "creature0")
    val creature0: String?,

    @field:Json(name = "creature1")
    val creature1: String?,

    @field:Json(name = "creature2")
    val creature2: String?
)