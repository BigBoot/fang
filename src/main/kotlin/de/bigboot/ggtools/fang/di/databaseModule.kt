package de.bigboot.ggtools.fang.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.bigboot.ggtools.fang.Config
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.bind
import org.koin.dsl.module
import javax.sql.DataSource

val databaseModule = module {
    single { HikariConfig().apply {
        driverClassName = Config.database.driver
        jdbcUrl = Config.database.url
        username = Config.database.user
        password = Config.database.pass
    } }

    single { HikariDataSource(get<HikariConfig>()) } bind DataSource::class
    single { Database.connect(get<DataSource>()) }
}
