package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.service.*
import org.koin.dsl.bind
import org.koin.dsl.module

val serviceModule = module {
    single { MatchServiceImpl() } bind MatchService::class
    single { PermissionServiceImpl() } bind PermissionService::class
    single { ServerServiceImpl() } bind ServerService::class
    single { ChangelogServiceImpl() } bind ChangelogService::class

    single { CommandsService() } bind AutostartService::class
    single { QueueJoinNotificationService() } bind AutostartService::class
    single { QueueMessageService() } bind AutostartService::class
    single { SetupGuildServiceImpl() } bind AutostartService::class bind SetupGuildService::class
    single { StatusUpdateService() } bind AutostartService::class
}
