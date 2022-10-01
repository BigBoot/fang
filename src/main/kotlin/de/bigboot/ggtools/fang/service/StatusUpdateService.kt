package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class StatusUpdateService : AutostartService, KoinComponent {
    private val client: GatewayDiscordClient by inject()
    private val matchService by inject<MatchService>()

    private val updateStatusTimer = Timer(true)

    init {
        updateStatusTimer.schedule(object : TimerTask() {
            override fun run() {
                runBlocking {
                    updateStatus()
                }
            }
        }, 0, Config.bot.statusupdate_poll_rate)
    }

    private suspend fun updateStatus() {
        val playersInQueue = Config.bot.queues.map { matchService.getNumPlayers(it.name) }

        val status = when {
            playersInQueue.any { it > 0 } -> "${playersInQueue.joinToString("/")} players in queue"
            else -> LOOKING_AT[((System.currentTimeMillis() / (60 * 1000)) % LOOKING_AT.size).toInt()]
        }

        client.updatePresence(ClientPresence.online(ClientActivity.watching(status))).block()
    }

    companion object {
        val LOOKING_AT = listOf(
            "Imanis tights",
            "Vodens tail",
            "Pakkos toys"
        )
    }
}
