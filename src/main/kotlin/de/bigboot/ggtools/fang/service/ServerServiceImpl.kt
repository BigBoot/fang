package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.api.ServerApi
import de.bigboot.ggtools.fang.db.Server
import de.bigboot.ggtools.fang.db.Servers
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ServerServiceImpl : ServerService, KoinComponent {
    private val database: Database by inject()
    private val clients: MutableMap<String, ServerApi>

    init {
        transaction(database) {
            SchemaUtils.create(Servers)
        }

        clients = transaction {
            Servers.selectAll().associate {
                Pair(it[Servers.name].toLowerCase(), createClient(it[Servers.url], it[Servers.apiKey]))
            }.toMutableMap()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun checkServer(name: String, url: String, apiKey: String): Boolean {
        try {
            val client = createClient(url, apiKey)
            client.getVersion()
        }
        catch (ex: Exception) {
            return false
        }
        return true
    }

    override suspend fun addServer(name: String, url: String, apiKey: String) {
        transaction(database) {
            clients[name.toLowerCase()] = createClient(url, apiKey)

            Servers.deleteWhere { Servers.name eq name }

            Server.new {
                this.name = name
                this.url = url
                this.apiKey = apiKey
            }
        }
    }

    override fun removeServer(name: String) {
        clients.remove(name.toLowerCase())
        transaction(database) {
            Servers.deleteWhere { Servers.name eq  name }
        }
    }

    override fun getClient(name: String): ServerApi? {
        return clients[name]
    }

    override fun getAllServers(): List<Server> {
        return transaction {
            Server.all().toList()
        }
    }

    private fun createClient(url: String, apiKey: String) : ServerApi = Retrofit.Builder()
        .baseUrl("$url/api/".replace("//", "/"))
        .addConverterFactory(MoshiConverterFactory.create())
        .client(
            OkHttpClient().newBuilder()
            .addInterceptor {
                it.proceed(it.request().newBuilder().addHeader("x-api-key", apiKey).build())
            }.build())
        .build()
        .create(ServerApi::class.java)
}