package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.UserRating
import org.goochjs.glicko2.Rating;

interface RatingService {
    fun newRating(): Boolean

    fun findUser(snowflake: Long): UserRating?
}
