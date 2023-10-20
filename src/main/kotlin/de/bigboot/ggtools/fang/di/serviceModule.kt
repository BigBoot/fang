package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.service.*
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val serviceModule = module {
    single { EmuServiceImpl() } bind EmuService::class
    single { MatchServiceImpl() } bind MatchService::class
    single { PermissionServiceImpl() } bind PermissionService::class
    single { ServerServiceImpl() } bind ServerService::class
    single { ChangelogServiceImpl() } bind ChangelogService::class
    single { PreferencesServiceImpl() } bind PreferencesService::class

    single { SetupGuildServiceImpl() } binds arrayOf(AutostartService::class, SetupGuildService::class)
    single { CommandsService() } bind AutostartService::class
    single { QueueMessageService() } bind AutostartService::class
    single { StatusUpdateService() } bind AutostartService::class
    single { MistforgeService() } bind AutostartService::class
    single { M202VerifyServiceImpl() } bind AutostartService::class
}
