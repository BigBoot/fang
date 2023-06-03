package de.bigboot.ggtools.fang.api.emu.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatchResponse(
    @field:Json(name = "reportToken")
    val reportToken: String,

    @field:Json(name = "team1")
    val team1: Team,

    @field:Json(name = "team2")
    val team2: Team,

) {
    @JsonClass(generateAdapter = true)
    data class Team(
        @field:Json(name = "players")
        val players: List<Player>,

        @field:Json(name = "averageSkill")
        val averageSkill: Double,
    )

    @JsonClass(generateAdapter = true)
    data class Player(
        @field:Json(name = "discordId")
        val discordId: String,

        @field:Json(name = "name")
        val name: String?,

        @field:Json(name = "matchToken")
        val matchToken: String?,
    )
}