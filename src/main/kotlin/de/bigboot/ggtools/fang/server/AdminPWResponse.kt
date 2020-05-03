package de.bigboot.ggtools.fang.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminPWResponse (
    @field:Json(name = "admin_pw")
    val adminPW: String?
)