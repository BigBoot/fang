package de.bigboot.ggtools.fang.api.emu

import de.bigboot.ggtools.fang.api.emu.model.QueueResponse
import retrofit2.http.GET

interface EmuApi {
    @GET("queue")
    suspend fun getQueue(): QueueResponse
}
