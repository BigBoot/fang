package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.*
import de.bigboot.ggtools.fang.utils.milliSecondsToTimespan
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

class MatchServiceImpl : MatchService, KoinComponent {
    private val database: Database by inject()
    private val notificationService by inject<NotificationService>()

    private var force = HashSet<String>()
    private var requests = HashMap<String, MatchService.Request>()

    init {
        transaction(database) {
            Players.deleteWhere { Players.inMatch eq true }
        }
    }

    override fun join(queue: String, snowflake: Long): Boolean {
        transaction {
            val player = Player.find { (Players.snowflake eq snowflake) and (Players.queue eq queue) }
                .firstOrNull() ?: Player.new {

                this.snowflake = snowflake
                this.joined = System.currentTimeMillis()
                this.queue = queue
            }

            player.inMatch = false
        }
        return true
    }

    override fun leave(queue: String, snowflake: Long, matchOnly: Boolean): Boolean {
        return transaction {
            val result = Players.deleteWhere {
                when (matchOnly) {
                    true -> (Players.snowflake eq snowflake) and (Players.queue eq queue) and (Players.inMatch eq true)
                    false -> (Players.snowflake eq snowflake) and (Players.queue eq queue)
                }
            } != 0

            if(result) {
                Highscore.find { (Highscores.snowflake eq snowflake) and (Highscores.queue eq queue) }.forEach { it.offset = 0 }
            }

            result
        }
    }

    override fun canPop(queue: String): Boolean
        = force.contains(queue) || requests.containsKey(queue) || getNumPlayers(queue) >= Config.bot.required_players

    override fun force(queue: String) {
        force.add(queue)
    }

    override fun request(queue: String, player: Long, minPlayers: Int) {
        requests[queue] = MatchService.Request(player, minPlayers);
    }

    override fun pop(queue: String, previousPlayers: Collection<Long>): MatchService.Pop {
        val pop = MatchService.Pop(
            forced = force.contains(queue),
            request = requests[queue],
            players = transaction {
                Player
                    .find { (Players.inMatch eq false) and (Players.queue eq queue) }
                    .asSequence()
                    .sortedBy { it.joined }
                    .take(kotlin.math.max(0, Config.bot.required_players - previousPlayers.size) )
                    .onEach { it.inMatch = true }
                    .map { it.snowflake }
                    .toList()
            },
            previousPlayers = previousPlayers
        )

        force.remove(queue)
        requests.remove(queue)

        return pop
    }

    override fun getPlayers(queue: String): Collection<MatchService.Player> = transaction(database) {
        Player
            .find { Players.queue eq queue }
            .filter { Player.find { (Players.snowflake eq it.snowflake) and ((Players.inMatch eq true)) }.empty() }
            .map { MatchService.Player(it.snowflake, it.joined) }
    }

    override fun getNumPlayers(queue: String) = transaction(database) {
        Player
            .find { Players.queue eq queue }
            .count { Player.find { (Players.snowflake eq it.snowflake) and ((Players.inMatch eq true)) }.empty() }
            .toLong()
    }

    override fun printQueue(queue: String): String = when {
        getNumPlayers(queue) == 0L -> "No one in queue ${Config.emojis.queue_empty}."
        else -> getPlayers(queue).sortedBy { it.joined }.joinToString("\n") { player ->
            val duration = ChronoUnit.MILLIS.between(Instant.ofEpochMilli(player.joined), Instant.now())
            val notification = when(notificationService.getDirectMessageNotificationsEnabled(player.snowflake)) {
                true -> Config.emojis.dm_notifications_enabled
                false -> Config.emojis.dm_notifications_disabled
            }
            "<@${player.snowflake}> $notification (In queue for ${duration.milliSecondsToTimespan()})"
        }
    }
}

