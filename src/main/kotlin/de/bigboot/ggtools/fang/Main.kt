package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.di.commandsModule
import de.bigboot.ggtools.fang.di.databaseModule
import de.bigboot.ggtools.fang.di.serviceModule
import discord4j.core.DiscordClient
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.context.startKoin

suspend fun main() {
    val configExceptions = Config.exceptions()

    if (configExceptions.isNotEmpty()) {
        configExceptions.forEach {
            println(it.message)
        }
        return
    }

    startKoin {
        printLogger()

        modules(
            databaseModule,
            serviceModule,
            commandsModule
        )
    }

    val client = DiscordClient.create(Config.BOT_TOKEN).login().awaitSingle()

    Fang(client).run()
}
