package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.db.Server
import de.bigboot.ggtools.fang.db.Servers
import de.bigboot.ggtools.fang.server.ServerApi
import okhttp3.OkHttpClient
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class ServerManager(private val database: Database) {
    private val clients: MutableMap<String, ServerApi>

    init {
        transaction(database) {
            SchemaUtils.create(Servers)
        }

        clients = transaction {
             Servers.selectAll().associate {
                Pair(it[Servers.name], createClient(it[Servers.url], it[Servers.apiKey]))
            }.toMutableMap()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun checkServer(name: String, url: String, apiKey: String): Boolean {
        try {
            val client = createClient(url, apiKey)
            client.getVersion()
        }
        catch (ex: Exception) {
            return false
        }
        return true
    }

    fun addServer(name: String, url: String, apiKey: String) {
        transaction(database) {
            clients[name] = createClient(url, apiKey)

            Servers.deleteWhere { Servers.name eq name }

            Server.new {
                this.name = name
                this.url = url
                this.apiKey = apiKey
            }
        }
    }

    fun removeServer(name: String) {
        clients.remove(name)
        transaction(database) {
            Servers.deleteWhere { Servers.name eq name }
        }
    }

    fun getClient(name: String): ServerApi? {
        return clients[name]
    }

    fun getAllServers(): List<Server> {
        return transaction {
            Server.all().toList()
        }
    }

    private fun createClient(url: String, apiKey: String) : ServerApi = Retrofit.Builder()
        .baseUrl("$url/api/".replace("//", "/"))
        .addConverterFactory(MoshiConverterFactory.create())
        .client(OkHttpClient().newBuilder()
            .addInterceptor {
                it.proceed(it.request().newBuilder().addHeader("x-api-key", apiKey).build())
            }.build())
        .build()
        .create(ServerApi::class.java)
}