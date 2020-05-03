package de.bigboot.ggtools.fang

import discord4j.core.DiscordClient
import kotlinx.coroutines.reactive.awaitSingle


suspend fun main() {
    val configExceptions = Config.exceptions()

    if (configExceptions.isNotEmpty()) {
        configExceptions.forEach {
            println(it.message)
        }
        return
    }

    val client = DiscordClient.create(Config.BOT_TOKEN).login().awaitSingle()

    Fang(client).run()
}