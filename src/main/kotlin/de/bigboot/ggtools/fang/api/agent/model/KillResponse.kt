package de.bigboot.ggtools.fang.api.agent.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KillResponse(
    @field:Json(name = "error")
    val error: String?
)