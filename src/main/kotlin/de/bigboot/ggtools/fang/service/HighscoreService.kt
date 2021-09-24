package de.bigboot.ggtools.fang.service

interface HighscoreService {
    data class Entry(val snowflake: Long, val score: Long)

    fun printHighscore(): String
}