package de.bigboot.ggtools.fang.service

interface MatchService {

    fun join(snowflake: Long): Boolean

    fun leave(snowflake: Long): Boolean

    fun canPop(): Boolean

    fun force()

    fun request(player: Long, minPlayers: Int)

    fun pop(): Pop

    fun getPlayers(): Collection<Long>

    fun getNumPlayers(): Int

    fun isPlayerQueued(snowflake: Long): Boolean

    data class Pop(
        val players: Collection<Long>,
        val forced: Boolean,
        val request: Request?
    )

    data class Request(
        val player: Long,
        val minPlayers: Int
    )
}