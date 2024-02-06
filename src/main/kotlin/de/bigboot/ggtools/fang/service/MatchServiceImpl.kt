package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.Player
import de.bigboot.ggtools.fang.db.Players
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MatchServiceImpl : MatchService, KoinComponent {
    private val database: Database by inject()
    private val preferencesService by inject<PreferencesService>()

    private var force = HashSet<String>()
    private var requests = HashMap<String, MatchService.Request>()

    init {
        transaction(database) {
            Players.deleteWhere { Players.inMatch eq true }
        }
    }

    override fun join(queue: String, snowflake: Long, force: Boolean): Boolean {
        transaction {
            val player = Player.find { (Players.snowflake eq snowflake) and (Players.queue eq queue) }
                .firstOrNull() ?: Player.new {

                this.snowflake = snowflake
                this.joined = System.currentTimeMillis()
                this.queue = queue
            }
            if (force) {
                player.inMatch = false
            }
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

            result
        }
    }

    override fun resetQueuePosition(queue: String, snowflake: Long): Boolean {
        return transaction {
            val player = Player.find { (Players.snowflake eq snowflake) and (Players.queue eq queue) }
                .firstOrNull() ?: return@transaction false

            player.joined = System.currentTimeMillis()
            true
        }
    }

    override fun canPop(queue: String): Boolean =
        force.contains(queue) || requests.containsKey(queue) || getNumPlayers(queue) >= Config.bot.required_players

    override fun force(queue: String) {
        force.add(queue)
    }

    override fun request(queue: String, player: Long, minPlayers: Int) {
        requests[queue] = MatchService.Request(player, minPlayers)
    }

    override fun pop(queue: String, server: String?, previousPlayers: Set<Long>): MatchService.Pop {
        val pop = transaction {
            val possibleServers = if (server != null ) listOf(server) else listOf("NA", "EU")

            val possiblePlayers =
                Player
                    .find { (Players.inMatch eq false) and (Players.queue eq queue) }
                    .asSequence()
                    .sortedBy { it.joined }
                    .map { Pair(it, preferencesService.getPreferences(it.snowflake)) }
                    .toMutableList()

            val requiredPlayers = Config.bot.required_players - previousPlayers.size


            val (queueServer, players) = kotlin.run {
                for(i in requiredPlayers until possiblePlayers.size) {
                    val players = possibleServers
                        .map { Pair(it, possiblePlayers
                            .take(i)
                            .filter { (_, prefs) -> prefs.preferredServers.contains(it) }
                            .toList()
                        )}
                        .maxBy { (_, players) -> players.size }

                    if (players.second.size >= requiredPlayers) {
                        return@run players
                    }
                }

                return@run possibleServers
                    .map { Pair(it, possiblePlayers
                        .filter { (_, prefs) -> prefs.preferredServers.contains(it) }
                        .toList()
                    )}
                    .maxBy { (_, players) -> players.size }
            }

            for ((player, _) in players)
            {
                player.inMatch = true
            }

            MatchService.Pop(
                forced = force.contains(queue),
                request = requests[queue],
                players = players.map { (player, _) -> player.snowflake }.toSet(),
                previousPlayers = previousPlayers,
                server = queueServer
            )
        }

        force.remove(queue)
        requests.remove(queue)

        return pop
    }

    override fun getPlayers(queue: String): Collection<MatchService.Player> = transaction(database) {
        Player
            .find { Players.queue eq queue }
            .filter { Player.find { (Players.snowflake eq it.snowflake) and ((Players.inMatch eq true)) }.empty() }
            .map { MatchService.Player(it.snowflake, it.joined, preferencesService.getPreferences(it.snowflake).preferredServers) }
    }

    override fun getNumPlayers(queue: String, server: String?) = transaction(database) {
        val players = getPlayers(queue)
        val servers = server?.let { setOf(it) } ?: setOf("NA", "EU")

        servers.map { players.filter { player -> player.preferredServers.contains(it)} }
            .maxOf { it.count() }
            .toLong()
    }
}
