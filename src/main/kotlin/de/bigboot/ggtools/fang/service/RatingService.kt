package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.UserRating

interface RatingService {
    fun findUser(snowflake: Long): UserRating?
}
