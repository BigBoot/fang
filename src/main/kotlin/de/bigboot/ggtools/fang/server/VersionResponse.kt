package de.bigboot.ggtools.fang.server

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionResponse(
    @field:Json(name = "app_version")
    val appVersion: String,

    @field:Json(name = "app_version_major")
    val appVersionMajor: String,

    @field:Json(name = "app_version_minor")
    val appVersionMinor: String,

    @field:Json(name = "app_version_patch")
    val appVersionPatch: String,

    @field:Json(name = "api_version")
    val apiVersion: Int
)

