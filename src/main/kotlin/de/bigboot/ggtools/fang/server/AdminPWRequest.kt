package de.bigboot.ggtools.fang.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminPWRequest (
    @field:Json(name = "id")
    val id: Int
)