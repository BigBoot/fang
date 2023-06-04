package de.bigboot.ggtools.fang.api.emu

import de.bigboot.ggtools.fang.api.emu.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface EmuApi {
    @GET("queue")
    suspend fun getQueue(): QueueResponse

    @GET("discord/user")
    suspend fun getDiscordUser(@Query("id") id: String): DiscordUserResponse

    @POST("match")
    suspend fun postMatch(@Body matchRequest: MatchRequest): MatchResponse

    @POST("match/token")
    suspend fun postMatchToken(@Body matchTokenRequest: MatchTokenRequest): MatchTokenResponse
}
