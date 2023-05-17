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

    override fun findUser(snowflake: Long) = transaction(database) {
        var user = User.find { Users.snowflake eq snowflake }.firstOrNull();

        if (user == null) {
            return@transaction null
        }

        if (user.rating == null) {
            val ratingSystem = RatingCalculator(0.06, 0.5);

            val rating = UserRating.new {
                this.rating = ratingSystem.getDefaultRating()
                this.ratingDeviation = ratingSystem.getDefaultRatingDeviation()
                this.volatility = ratingSystem.getDefaultVolatility()
            }

            user.rating = rating;

        }
        return@transaction user.rating
    }
}
