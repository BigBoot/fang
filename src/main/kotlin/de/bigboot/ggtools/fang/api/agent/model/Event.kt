package de.bigboot.ggtools.fang.api.agent.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Event(
    @field:Json(name = "id")
    val id: String,

    @field:Json(name = "instance_id")
    val instanceId: Int,

    @field:Json(name = "description")
    val description: String,

    @field:Json(name = "data")
    val data: String?,

    @field:Json(name = "timestamp")
    val timestamp: Long
)