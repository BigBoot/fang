package de.bigboot.ggtools.fang.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayersResponse (
    @field:Json(name = "name")
    val name: String,

    @field:Json(name = "hero")
    val hero: String?
)