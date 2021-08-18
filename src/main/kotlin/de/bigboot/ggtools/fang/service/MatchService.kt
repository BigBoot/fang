package de.bigboot.ggtools.fang.service

interface MatchService {

    fun join(queue: String, snowflake: Long): Boolean

    fun leave(queue: String, snowflake: Long, matchOnly: Boolean = false): Boolean

    fun canPop(queue: String): Boolean

    fun force(queue: String)

    fun request(queue: String, player: Long, minPlayers: Int)

    fun pop(queue: String, previousPlayers: Collection<Long> = emptyList()): Pop

    fun getPlayers(queue: String): Collection<Player>

    fun getNumPlayers(queue: String): Long

    fun printQueue(queue: String): String

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
