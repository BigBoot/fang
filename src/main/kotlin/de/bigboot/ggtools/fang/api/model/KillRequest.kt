package de.bigboot.ggtools.fang.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KillRequest (
    @field:Json(name = "id")
    val id: Int
)