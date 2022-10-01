package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.Config
import discord4j.core.DiscordClient
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import org.koin.dsl.module

val discordModule = module {
    single {
        DiscordClient
            .create(Config.bot.token)
            .gateway()
            .setEnabledIntents(IntentSet.nonPrivileged().or(IntentSet.of(Intent.GUILD_MEMBERS)))
            .login()
            .block()
    }
}
