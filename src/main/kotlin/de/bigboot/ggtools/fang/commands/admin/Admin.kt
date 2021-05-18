package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.commands.admin.group.Group
import kotlinx.coroutines.reactive.awaitSingle
import kotlin.system.exitProcess
import discord4j.common.util.Snowflake
import de.bigboot.ggtools.fang.service.MatchService
import org.koin.core.inject
import de.bigboot.ggtools.fang.Config


class Admin : CommandGroupSpec("admin", "Admin commands") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Server())
        group(Group())
        val matchService by inject<MatchService>()
        
         command("restart", "restart the bot") {
            onCall {
                channel().createMessage { message ->
                    message.setContent("I'm sorry for disappointing you")
                }.awaitSingle()

                exitProcess(0)
            }
        }

        command("score", "@ the 10 people that played but the first 5 people you @ are the winners") {
            arg("player1", "player1 - won")
            arg("player2", "player2 - won")
            arg("player3", "player3 - won")
            arg("player4", "player4 - won")
            arg("player5", "player5 - won")
            arg("player6", "player6 - lost")
            arg("player7", "player7 - lost")
            arg("player8", "player8 - lost")
            arg("player9", "player9 - lost")
            arg("player10", "player10 - lost")

            onCall {
                val player1 = Snowflake.of(args["player1"].dropLast(1).drop(2)).asLong()
                val player2 = Snowflake.of(args["player2"].dropLast(1).drop(2)).asLong()
                val player3 = Snowflake.of(args["player3"].dropLast(1).drop(2)).asLong()
                val player4 = Snowflake.of(args["player4"].dropLast(1).drop(2)).asLong()
                val player5 = Snowflake.of(args["player5"].dropLast(1).drop(2)).asLong()
                val player6 = Snowflake.of(args["player6"].dropLast(1).drop(2)).asLong()
                val player7 = Snowflake.of(args["player7"].dropLast(1).drop(2)).asLong()
                val player8 = Snowflake.of(args["player8"].dropLast(1).drop(2)).asLong()
                val player9 = Snowflake.of(args["player9"].dropLast(1).drop(2)).asLong()
                val player10 = Snowflake.of(args["player10"].dropLast(1).drop(2)).asLong()
                val teamOneSkill = matchService.getPlayerSkill(player1) +  matchService.getPlayerSkill(player2) + matchService.getPlayerSkill(player3) + matchService.getPlayerSkill(player4) + matchService.getPlayerSkill(player5)
                val teamTwoSkill = matchService.getPlayerSkill(player6) +  matchService.getPlayerSkill(player7) + matchService.getPlayerSkill(player8) + matchService.getPlayerSkill(player9) + matchService.getPlayerSkill(player10)
                val howFair = teamTwoSkill / teamOneSkill
                matchService.setPlayerSkill(player1, matchService.getPlayerSkill(player1) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player2, matchService.getPlayerSkill(player2) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player3, matchService.getPlayerSkill(player3) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player4, matchService.getPlayerSkill(player4) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player5, matchService.getPlayerSkill(player5) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player6, matchService.getPlayerSkill(player6) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player7, matchService.getPlayerSkill(player7) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player8, matchService.getPlayerSkill(player8) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player9, matchService.getPlayerSkill(player9) + (Config.bot.score_increase * howFair))
                matchService.setPlayerSkill(player10, matchService.getPlayerSkill(player10) + (Config.bot.score_increase * howFair))
            }
        }
    }
}
