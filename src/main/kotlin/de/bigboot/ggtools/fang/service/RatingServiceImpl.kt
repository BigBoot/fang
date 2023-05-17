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
import org.topalski.teams.*;

class RatingServiceImpl : RatingService, KoinComponent {
    private val database: Database by inject()
    private val ratingSystem = RatingCalculator(0.06, 0.5);

    override fun findUser(snowflake: Long) = transaction(database) {
        return@transaction UserRating.find { UsersRating.snowflake eq snowflake }
            .firstOrNull() ?: UserRating.new {

                this.snowflake = snowflake
                this.rating = ratingSystem.getDefaultRating()
                this.ratingDeviation = ratingSystem.getDefaultRatingDeviation()
                this.volatility = ratingSystem.getDefaultVolatility()
        };
    }

    override fun addResult(winning: List<Long>, loosing: List<Long>) = transaction(database) {
        var winning = winning.map{findUser(it)};
        var loosing = loosing.map{findUser(it)};

        var glickoWinning = winning.map{Rating(it.snowflake.toString(), it.rating, it.ratingDeviation, it.volatility)}.toMutableSet();
        var glickoLoosing = loosing.map{Rating(it.snowflake.toString(), it.rating, it.ratingDeviation, it.volatility)}.toMutableSet();

        var game = TeamIndividualUpdate();

        var teamWinning = Team(mutableSetOf()); // for some reason when you put glickWinning right into this it does not actually set it
        var teamLoosing = Team(mutableSetOf());

        glickoWinning.forEach {
            teamWinning.addTeamPlayer(it)
        }
        glickoLoosing.forEach {
            teamLoosing.addTeamPlayer(it)
        }

        game.addResult(teamWinning, teamLoosing);
        game.updateRating(ratingSystem);

        var i = 0;
        glickoWinning.forEach {
            winning[i].rating = it.getRating();
            winning[i].ratingDeviation = it.getRatingDeviation();
            winning[i].volatility = it.getVolatility();
            i++;
        }

        i = 0;
        glickoLoosing.forEach {
            loosing[i].rating = it.getRating();
            loosing[i].ratingDeviation = it.getRatingDeviation();
            loosing[i].volatility = it.getVolatility();
            i++;
        }
    }
}
