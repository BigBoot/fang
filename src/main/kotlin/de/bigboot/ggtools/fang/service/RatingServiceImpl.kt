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
        val ratingSystem = RatingCalculator(0.06, 0.5);

        return@transaction UserRating.find { UsersRating.snowflake eq snowflake }
            .firstOrNull() ?: UserRating.new {

                this.snowflake = snowflake
                this.rating = ratingSystem.getDefaultRating()
                this.ratingDeviation = ratingSystem.getDefaultRatingDeviation()
                this.volatility = ratingSystem.getDefaultVolatility()
        };
    }
}
