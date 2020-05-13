package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.Player
import de.bigboot.ggtools.fang.db.Players
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject

class MatchServiceImpl : MatchService, KoinComponent {
    private val database: Database by inject()
    private var force = false
    private var request: MatchService.Request? = null

    init {
        transaction(database) {
            SchemaUtils.create(Players)
        }
    }

    override fun join(snowflake: Long): Boolean {
        transaction {
            val player = Player.find { Players.snowflake eq snowflake }.firstOrNull() ?: Player.new {
                this.snowflake = snowflake
                this.joined = System.currentTimeMillis()
            }

            player.inMatch = false
        }
        return true
    }

    override fun leave(snowflake: Long): Boolean {
        return transaction {
            Players.deleteWhere { Players.snowflake eq snowflake } != 0
        }
    }

    override fun canPop(): Boolean = force || request != null || getNumPlayers() >= Config.REQUIRED_PLAYERS

    override fun force() {
        force = true
    }

    override fun request(player: Long, minPlayers: Int) {
        request = MatchService.Request(player, minPlayers)
    }

    override fun pop(): MatchService.Pop {
        val pop = MatchService.Pop(
            forced = force,
            request = request,
            players = transaction {
                Player
                    .find { Players.inMatch eq false }
                    .asSequence()
                    .sortedBy { it.joined }
                    .take(Config.REQUIRED_PLAYERS)
                    .onEach { it.inMatch = true }
                    .map { it.snowflake }
                    .toList()
            }
        )

        force = false
        request = null

        return pop
    }

    override fun getPlayers(): Collection<Long> = transaction {
        Player
            .find { Players.inMatch eq false }
            .map { it.snowflake }
    }

    override fun getNumPlayers() = transaction {
        Player
            .find { Players.inMatch eq false }
            .count()
    }

    override fun isPlayerQueued(snowflake: Long) = transaction {
        !Player.find { Players.snowflake eq snowflake }.empty()
    }
}
