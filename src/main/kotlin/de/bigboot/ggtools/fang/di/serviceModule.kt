package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.service.*
import org.koin.dsl.module

val serviceModule = module {
    single<MatchService> { MatchServiceImpl() }
    single<PermissionService> { PermissionServiceImpl() }
    single<ServerService> { ServerServiceImpl() }
}
