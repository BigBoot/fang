package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.*
import org.goochjs.glicko2.Rating;
import org.goochjs.glicko2.RatingCalculator;
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class RatingServiceImpl : RatingService, KoinComponent {
    private val database: Database by inject()

    override fun newRating() = transaction(database) {
        val ratingSystem = RatingCalculator(0.06, 0.5);

        UserRating.new {
            this.rating = ratingSystem.getDefaultRating()
            this.ratingDeviation = ratingSystem.getDefaultRatingDeviation()
            this.volatility = ratingSystem.getDefaultVolatility()
        }

        return@transaction true
    }

    override fun findUser(snowflake: Long): UserRating? {
        return User.find { Users.snowflake eq snowflake }.firstOrNull()?.rating;
    }
}
