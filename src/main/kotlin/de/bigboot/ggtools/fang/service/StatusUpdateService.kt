package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Status
import discord4j.discordjson.json.ActivityUpdateRequest
import discord4j.discordjson.json.gateway.StatusUpdate
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

class StatusUpdateService : AutostartService, KoinComponent {
    private val client: GatewayDiscordClient by inject()
    private val matchService by inject<MatchService>()

    private val updateStatusTimer = Timer(true)

    init {
        updateStatusTimer.schedule(object : TimerTask() {
            override fun run() {
                updateStatus()
            }
        }, 0, Config.bot.statusupdate_poll_rate)
    }

    private fun updateStatus() {
        val playersInQueue = matchService.getNumPlayers()

        val status = when {
            playersInQueue > 0 -> "at ${matchService.getNumPlayers()} players in queue"
            else -> "at ${LOOKING_AT[((System.currentTimeMillis() / (60 * 1000)) % LOOKING_AT.size).toInt()]}"
        }

        client.updatePresence(
            StatusUpdate.builder()
                .status(Status.ONLINE.value)
                .game(
                    ActivityUpdateRequest.builder()
                        .name(status)
                        .type(Activity.Type.WATCHING.value)
                        .build()
                )
                .afk(false)
                .since(Optional.empty())
                .build()
        ).block()
    }

    companion object {
        val LOOKING_AT = listOf(
            "Imanis tights",
            "Vodens tail",
            "Pakkos toys"
        )
    }
}