package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.db.Player
import de.bigboot.ggtools.fang.db.Players
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class MatchManager(database: Database) {
    private var force = false
    private var request: Request? = null

    init {
        transaction(database) {
            SchemaUtils.create(Players)
        }
    }

    fun join(snowflake: Long): Boolean {
        transaction {
            val player = Player.find { Players.snowflake eq snowflake }.firstOrNull() ?: Player.new {
                this.snowflake = snowflake
                this.joined = System.currentTimeMillis()
            }

            player.inMatch = false
        }
        return true
    }

    fun leave(snowflake: Long): Boolean {
        return transaction {
            Players.deleteWhere { Players.snowflake eq snowflake } != 0
        }
    }

    fun canPop(): Boolean = force || request != null || getNumPlayers() >= 10

    fun force() {
        force = true
    }

    fun request(player: Long, minPlayers: Int) {
        request = Request(player, minPlayers)
    }

    fun pop(): Pop {
        val pop = Pop(
            forced = force,
            request = request,
            players = transaction {
                Player
                    .find { Players.inMatch eq false }
                    .asSequence()
                    .sortedBy { it.joined }
                    .take(10)
                    .onEach { it.inMatch = true }
                    .map { it.snowflake }
                    .toList()
            }
        )

        force = false
        request = null

        return pop
    }

    fun getPlayers(): Collection<Long> = transaction {
        Player
            .find { Players.inMatch eq false }
            .map { it.snowflake }
    }

    fun getNumPlayers() = transaction {
        Player
            .find { Players.inMatch eq false }
            .count()
    }

    fun isPlayerQueued(snowflake: Long) = transaction {
        !Player.find { Players.snowflake eq snowflake }.empty()
    }

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