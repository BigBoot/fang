package de.bigboot.ggtools.fang.service

interface MatchService {

    fun join(snowflake: Long): Boolean

    fun leave(snowflake: Long): Boolean

    fun canPop(): Boolean

    fun force()

    fun request(player: Long, minPlayers: Int)

    fun pop(previousPlayers: Collection<Long> = emptyList()): Pop

    fun getPlayers(): Collection<Player>

    fun getNumPlayers(): Long

    fun isPlayerQueued(snowflake: Long): Boolean

    fun printQueue(): String

    data class Player(
        val snowflake: Long,
        val joined: Long
    )

    data class Pop(
        val players: Collection<Long>,
        val forced: Boolean,
        val request: Request?,
        val previousPlayers: Collection<Long> = emptyList()
    )

    data class Request(
        val player: Long,
        val minPlayers: Int
    )
}
