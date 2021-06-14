package de.bigboot.ggtools.fang.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class QueueResponse(
    @field:Json(name = "players")
    val players: List<String>?
)

interface EmuApi {
    @GET("queue")
    suspend fun getQueue(): QueueResponse
}
