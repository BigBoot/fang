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
    }
}
