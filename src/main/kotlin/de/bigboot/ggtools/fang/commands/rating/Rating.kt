package de.bigboot.ggtools.fang.commands.rating

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.RatingService
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactive.awaitSingle
import okhttp3.Request
import okhttp3.OkHttpClient
import org.koin.core.component.inject

class Rating : CommandGroupSpec("rating", "Commands for ratings") {
    private val ratingService by inject<RatingService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("import", "import many games into the rating system") {
            onCall {
                val attachments = message.attachments;
                if (!attachments.isEmpty()) {
                    var client = OkHttpClient();
                    val message = channel().createMessageCompat {
                        addEmbedCompat {
                            description("Adding scores")
                        }
                    }.awaitSingle()
                    attachments.forEach {
                        val request = Request.Builder().url(it.url).build();
                        val response = client.newCall(request).execute().body();
                        if (response != null) {
                            response.string().lines().forEach {
                                if (it.trim() != "") {
                                    val split = it.split(" ").map{it.toLong()};
                                    val half = split.size/2;
                                    ratingService.addResult(split.slice(0..half-1), split.slice(half..split.size-1));
                                }
                            }
                        }
                        else {
                            channel().createMessageCompat {
                                addEmbedCompat {
                                    description("Failed to read the contents of your attachment, please try in a few minuets")
                                }
                            }.awaitSingle()
                            return@onCall
                        }
                    }
                    message.editCompat {
                        addEmbedCompat {
                            description("Added all of the results!")
                        }
                    }.awaitSingle()
                }
                else {
                    channel().createMessageCompat {
                        addEmbedCompat {
                            description("No attachments were provided")
                        }
                    }.awaitSingle()
                }
            }
        }

        // This is just a command for testing the teams created, this will not actually be pushed into production
        command("maketeam", "make a team") {
            arg("one", "")
            arg("two", "")
            arg("three", "")
            arg("four", "")
            arg("five","")
            arg("six", "")
            arg("seven", "")
            arg("eight", "")
            arg("nine", "")
            arg("ten", "")

            onCall {
                var teams = ratingService.makeTeams(listOf(args["one"].toLong(), args["two"].toLong(), args["three"].toLong(), args["four"].toLong(), args["five"].toLong(), args["six"].toLong(), args["seven"].toLong(), args["eight"].toLong(), args["nine"].toLong(), args["ten"].toLong()));
                var diff = ratingService.teamDifferential(teams);

                channel().createMessageCompat {
                    addEmbedCompat {
                        description("team one: <@${teams.first.joinToString("> <@")}>\nteam two: <@${teams.second.joinToString("> <@")}>\ndiff: ${diff}")
                    }
                }.awaitSingle()
            }
        }
    }
}
