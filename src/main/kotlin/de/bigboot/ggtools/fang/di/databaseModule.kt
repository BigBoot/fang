package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.Config
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val databaseModule = module {
    single {
        Database.connect(
            url = Config.DB_URL,
            driver = Config.DB_DRIVER,
            user = Config.DB_USER,
            password = Config.DB_PASS
        )
    }
}