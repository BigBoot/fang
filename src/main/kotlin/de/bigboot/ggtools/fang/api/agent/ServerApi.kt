package de.bigboot.ggtools.fang.api.agent

import de.bigboot.ggtools.fang.api.agent.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ServerApi {
    @POST("version")
    suspend fun getVersion(): VersionResponse

    @GET("players")
    suspend fun getPlayers(@Query("id") id: Int): List<PlayersResponse>

    @POST("start")
    suspend fun start(@Body startRequest: StartRequest): StartResponse

    @POST("kill")
    suspend fun kill(@Body killRequest: KillRequest): KillResponse

    @POST("admin_pw")
    suspend fun getAdminPW(@Body adminPWRequest: AdminPWRequest): AdminPWResponse

    @POST("events")
    suspend fun getEvents(@Body eventsRequest: EventsRequest): EventsResponse
}
