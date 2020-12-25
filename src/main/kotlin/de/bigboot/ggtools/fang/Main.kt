package de.bigboot.ggtools.fang

import de.bigboot.ggtools.fang.di.commandsModule
import de.bigboot.ggtools.fang.di.databaseModule
import de.bigboot.ggtools.fang.di.discordModule
import de.bigboot.ggtools.fang.di.serviceModule
import de.bigboot.ggtools.fang.service.AutostartService
import discord4j.core.GatewayDiscordClient
import kotlinx.coroutines.reactive.awaitSingle
import org.flywaydb.core.Flyway
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Level.*
import org.koin.core.logger.MESSAGE
import org.tinylog.kotlin.Logger

class Main

suspend fun main() {
    println("Working dir: ${System.getProperty("user.dir")}")

    org.tinylog.configuration.Configuration.set("level", Config.bot.log_level)

    Flyway
        .configure()
        .locations(Main::class.java.`package`.name.replace(".", "/"))
        .dataSource(Config.database.url, Config.database.user, Config.database.pass)
        .baselineVersion("0")
        .baselineOnMigrate(true)
        .load()
        .migrate()

    val app = startKoin {
        logger(object : org.koin.core.logger.Logger() {
            override fun log(level: Level, msg: MESSAGE) {
                when (level) {
                    ERROR -> Logger.error(msg)
                    INFO -> Logger.info(msg)
                    DEBUG -> Logger.debug(msg)
                    NONE -> Logger.trace(msg)
                }
            }
        })

        koin.loadModules(listOf(
            databaseModule,
            serviceModule,
            commandsModule,
            discordModule,
        ))
        koin.createRootScope()
    }

    app.koin.getAll<AutostartService>()
    app.koin.get<GatewayDiscordClient>().onDisconnect().awaitSingle()
}

