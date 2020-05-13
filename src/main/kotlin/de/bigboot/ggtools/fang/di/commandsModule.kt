package de.bigboot.ggtools.fang.di

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.Commands
import de.bigboot.ggtools.fang.commands.Root
import org.koin.dsl.module

val commandsModule = module {
    single<Commands> {
        CommandGroupBuilder("", "").apply(Root().build).build()
    }
}
