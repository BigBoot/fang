package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.Config
import discord4j.core.DiscordClient
import org.koin.dsl.module

val discordModule = module {
    single { DiscordClient.create(Config.bot.token).login().block() }
}
