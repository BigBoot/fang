package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.db.Player
import de.bigboot.ggtools.fang.db.Players
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class MatchManager(database: Database) {
    private var force = false

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

    fun canPop(): Boolean = force || getNumPlayers() >= 10

    fun force() {
        force = true
    }

    fun pop(): Collection<Long> {
        force = false
        return transaction {
            Player
                .find { Players.inMatch eq false }
                .asSequence()
                .sortedBy { it.joined }
                .take(10)
                .onEach { it.inMatch = true }
                .map { it.snowflake }
                .toList()
        }
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
}