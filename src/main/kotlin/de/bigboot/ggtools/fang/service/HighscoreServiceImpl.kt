package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.Highscore
import de.bigboot.ggtools.fang.db.Highscores
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.Duration
import java.util.Timer
import java.util.TimerTask

class HighscoreServiceImpl : HighscoreService, AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()

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
            .flatMap { matchService.getPlayers(it.name) }
            .map { HighscoreService.Entry(it.snowflake, time - it.joined) }

        val oldscores = transaction { Highscore.all().map { HighscoreService.Entry(it.snowflake, it.score) } }
            .sortedByDescending { it.score }
            .toList()

        highscores = (oldscores + players)
            .groupBy { it.snowflake }
            .map { it.value.maxByOrNull { highscore ->  highscore.score } }
            .filterNotNull()
            .sortedByDescending { it.score }
            .take(10)
            .toList()

        transaction {
            Highscores.deleteAll()
            highscores.forEach { Highscore.new {
                snowflake = it.snowflake
                score = it.score
            } }
        }

        highscores.forEachIndexed { new, entry ->
            val old = oldscores.indexOfFirst { it.snowflake == entry.snowflake }

            if(old == -1 || old > new) {
                for (channel in channels)
                {
                    channel.createMessage { msg ->
                        msg.setContent("Congratulations <@${entry.snowflake}> you're now ranked #${new+1} in the queue highscores.")
                    }.awaitSafe()
                }
            }
        }
    }

    override fun printHighscore(): String = highscores.mapIndexed { index, entry ->
        "${(index+1).asReaction().print()} ${entry.score.milliSecondsToTimespan()} <@${entry.snowflake}>"
    }.joinToString("\n")
}
