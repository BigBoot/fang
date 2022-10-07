package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.api.agent.ServerApi
import de.bigboot.ggtools.fang.db.Server

interface ServerService {
    suspend fun checkServer(name: String, url: String, apiKey: String): Boolean

    suspend fun addServer(name: String, url: String, apiKey: String)

    fun removeServer(name: String)

    fun getClient(name: String): ServerApi?

    fun getAllServers(): List<Server>
}
