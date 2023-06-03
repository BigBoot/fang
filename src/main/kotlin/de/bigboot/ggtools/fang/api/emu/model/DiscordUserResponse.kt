package de.bigboot.ggtools.fang.api.emu.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscordUserResponse(
    @field:Json(name = "name")
    val name: String,

)