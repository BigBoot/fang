package de.bigboot.ggtools.fang.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventsRequest (
    @field:Json(name = "timestamp")
    val timestamp: Long
)