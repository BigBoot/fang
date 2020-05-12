package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.Server
import de.bigboot.ggtools.fang.db.Servers
import de.bigboot.ggtools.fang.api.ServerApi
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

interface ServerService {

    suspend fun checkServer(name: String, url: String, apiKey: String): Boolean

    suspend fun addServer(name: String, url: String, apiKey: String)

    fun removeServer(name: String)

    fun getClient(name: String): ServerApi?

    fun getAllServers(): List<Server>
}