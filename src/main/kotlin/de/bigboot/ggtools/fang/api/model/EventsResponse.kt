package de.bigboot.ggtools.fang.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventsResponse (
    @field:Json(name = "timestamp")
    val timestamp: Long,

    @field:Json(name = "events")
    val events: List<Event>
)