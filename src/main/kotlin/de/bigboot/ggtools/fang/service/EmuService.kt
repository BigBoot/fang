package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.api.emu.model.MatchResponse

interface EmuService {
    data class User(val name: String)

    suspend fun getQueue(): List<String>

    suspend fun getUser(snowflake: Long): User?

    suspend fun requestMatch(players: List<Long>): MatchResponse?

    suspend fun getReportUrl(token: String): String?
}
