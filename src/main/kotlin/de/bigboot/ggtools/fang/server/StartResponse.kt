package de.bigboot.ggtools.fang.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartResponse (
    @field:Json(name = "error")
    val error: String?,

    @field:Json(name = "open_url")
    val openUrl: String?
)