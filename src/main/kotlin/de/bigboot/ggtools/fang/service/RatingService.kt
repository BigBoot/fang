package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.UserRating

interface RatingService {

    fun findUser(snowflake: Long): UserRating?

    fun addResult(winning: List<Long>, loosing: List<Long>)

    fun makeTeams(players: List<Long>): Pair<List<Long>, List<Long>>

}
