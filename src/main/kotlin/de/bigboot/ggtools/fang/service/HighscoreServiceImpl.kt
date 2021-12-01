package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.Highscore
import de.bigboot.ggtools.fang.db.Highscores
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Duration
import java.util.Timer
import java.util.TimerTask

class HighscoreServiceImpl : HighscoreService, AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()

    private val database: Database by inject()

    private val matchService by inject<MatchService>()

    private val updateTimer = Timer(true)

    private var highscores = listOf<HighscoreService.Entry>()

    init {
        updateTimer.schedule(object : TimerTask() {
            override fun run() {
                @Suppress("BlockingMethodInNonBlockingContext")
                runBlocking(Dispatchers.IO) {
                    updateHighscore()
                }
            }
        }, 0, 5000)
    }

    private suspend fun updateHighscore() {
        val time = System.currentTimeMillis()

        val channels = client.guilds
            .flow()
            .map { it.findChannel(Config.bot.highscore_channel) }
            .filterNotNull()
            .toList()

        val players = Config.bot.queues
            .filter { it.enable_highscore }
            .flatMap { matchService.getPlayers(it.name).map { player -> Pair(it.name, player) } }
            .map { HighscoreService.Entry(it.second.snowflake, time - it.second.joined, it.first) }
            .groupBy { it.snowflake }
            .map { it.value.maxByOrNull { entry -> entry.score }!! }

        val oldscores = transaction(database) {
                Highscore.all()
                    .orderBy(Highscores.score to SortOrder.DESC)
                    .limit(10)
                    .map { HighscoreService.Entry(it.snowflake, it.score, it.queue) }
            }
            .toList()

        transaction(database) {
            for (player in players) {
                val highscore = Highscore.find { Highscores.snowflake eq player.snowflake }
                    .firstOrNull() ?: Highscore.new {
                        snowflake = player.snowflake
                        score = 0
                        offset = 0
                }

                highscore.score = highscore.score - highscore.offset + player.score
                highscore.offset = player.score
                highscore.queue = player.queue
            }
        }

        highscores = transaction(database) {
                Highscore.all()
                    .orderBy(Highscores.score to SortOrder.DESC)
                    .limit(10)
                    .map { HighscoreService.Entry(it.snowflake, it.score, it.queue) }
            }
            .toList()

        highscores.forEachIndexed { new, entry ->
            val old = oldscores.indexOfFirst { it.snowflake == entry.snowflake }

            if(old == -1 || old > new) {
                for (channel in channels)
                {
                    channel.createEmbed { msg ->
                        msg.setDescription("Congratulations <@${entry.snowflake}> you're now ranked #${new+1} in the queue highscores.")
                    }.awaitSafe()
                }
            }
        }
    }

    override suspend fun resetAll(): Unit
        = transaction(database) { Highscores.deleteAll() }

    override suspend fun reset(snowflake: Long): Unit
        = transaction(database) { Highscores.deleteWhere { Highscores.snowflake eq snowflake } }

    override fun printHighscore(): String = highscores.mapIndexed { index, entry ->
        "${(index+1).asReaction().print()} ${entry.score.milliSecondsToTimespan()} <@${entry.snowflake}>"
    }.joinToString("\n")
}
