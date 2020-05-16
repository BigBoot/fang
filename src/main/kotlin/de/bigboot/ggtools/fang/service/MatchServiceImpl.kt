package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.Player
import de.bigboot.ggtools.fang.db.Players
import de.bigboot.ggtools.fang.utils.milliSecondsToTimespan
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant
import java.time.temporal.ChronoUnit

class MatchServiceImpl : MatchService, KoinComponent {
    private val database: Database by inject()
    private var force = false
    private var request: MatchService.Request? = null

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

    override fun canPop(): Boolean = force || request != null || getNumPlayers() >= Config.bot.required_players

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
                    .take(Config.bot.required_players)
                    .onEach { it.inMatch = true }
                    .map { it.snowflake }
                    .toList()
            }
        )

        force = false
        request = null

        return pop
    }

    override fun getPlayers(): Collection<MatchService.Player> = transaction(database) {
        Player
            .find { Players.inMatch eq false }
            .map { MatchService.Player(it.snowflake, it.joined) }
    }

    override fun getNumPlayers() = transaction(database) {
        Player
            .find { Players.inMatch eq false }
            .count()
    }

    override fun isPlayerQueued(snowflake: Long) = transaction(database) {
        !Player.find { Players.snowflake eq snowflake }.empty()
    }

    override fun printQueue(): String = when {
        getNumPlayers() == 0 -> "No one in queue ${Config.emojis.queue_empty}."
        else -> getPlayers().sortedBy { it.joined }.joinToString("\n") { player ->
            val duration = ChronoUnit.MILLIS.between(Instant.ofEpochMilli(player.joined), Instant.now())
            "<@${player.snowflake}> (In queue for ${duration.milliSecondsToTimespan()})"
        }
    }
}
