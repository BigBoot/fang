package de.bigboot.ggtools.fang.components

import discord4j.core.`object`.component.ActionComponent

interface ComponentSpec {
    fun id(): String
    fun component(): ActionComponent
}