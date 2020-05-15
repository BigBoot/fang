package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.Config
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module

val databaseModule = module {
    single {
        Database.connect(
            url = Config.database.url,
            driver = Config.database.driver,
            user = Config.database.user,
            password = Config.database.pass
        )
    }
}
