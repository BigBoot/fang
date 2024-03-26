package de.bigboot.ggtools.fang.commands.giveway

import de.bigboot.ggtools.fang.CommandContext
import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.service.GivewayService
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.common.util.TimestampFormat
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.reaction.ReactionEmoji
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes

private val TIMEOUT = 30.minutes.inWholeMilliseconds

class Giveaway : CommandGroupSpec("giveaway", "Commands for managing giveaways") {
    private val client: GatewayDiscordClient by inject()
    private val giveawayService by inject<GivewayService>()

    private fun CommandContext.cancel(job: Job) {
        CoroutineScope(Dispatchers.Default).launch {
            channel().createMessageCompat {
                content("I haven't heard anything from you for a while so I cancelled interaction...")
            }.awaitSingle()
        }

        job.cancel()
    }

    override val build: CommandGroupBuilder.() -> Unit = {
        command("create", "create a new giveaway") {
            onCall {

                val author = message.author.get()
                channel().createMessageCompat {
                    content("Sure let's get started, you can cancel the creation any time by answering `cancel`, first tell me which channel the giveaway should be created in.")
                }.awaitSingle()

                var timeout: Timer? = null

                var channel: Snowflake? = null
                var title: String? = null
                var description: String? = null
                var end: Instant? = null
                val prizes: MutableList<GivewayService.Prize> = mutableListOf()

                var prizeTitle: String? = null
                var prizeEmoji: ReactionEmoji? = null
                var prizeCount: Int? = null

                var confirm = false

                var job: Job? = null

                job = client.eventDispatcher.on<MessageCreateEvent>()
                    .filter { it.message.channelId == this@onCall.channel().id && it.message.author.get().id == author.id }
                    .takeWhile { ev ->
                        timeout?.cancel()
                        timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job?.apply { cancel(this) } }

                        val next = ev.message
                        var content: String? = next.content

                        if (content == "cancel") {
                            channel().createMessageCompat {
                                content("Ok, I cancelled the giveaway!")
                            }.awaitSingle()

                            return@takeWhile false
                        }

                        if (channel == null)
                        {
                            channel = guild().findChannel(next.content)?.id

                            if (channel != null) {
                                channel().createMessageCompat {
                                    content("Got it, next please tell me the title for the giveaway.")
                                }.awaitSingle()
                            }
                            else
                            {
                                channel().createMessageCompat {
                                    content("Sorry I could not find that channel...")
                                }.awaitSingle()
                            }

                            return@takeWhile true
                        }

                        if (title == null)
                        {
                            title = next.content

                            channel().createMessageCompat {
                                content("Okay, now please tell me the description for the giveaway.")
                            }.awaitSingle()

                            return@takeWhile true
                        }

                        if (description == null)
                        {
                            description = next.content

                            channel().createMessageCompat {
                                content("Done, and when should the giveaway end? ([Unix Epoch Timestamp](https://www.unixtimestamp.com/))")
                            }.awaitSingle()

                            return@takeWhile true
                        }

                        if (end == null) {
                            end = next.content.toLongOrNull()?.let { Instant.ofEpochSecond(it) }

                            if (end != null) {
                                channel().createMessageCompat {
                                    content("Great, the giveaway will end ${TimestampFormat.RELATIVE_TIME.format(end!!)}.\nNow we just need to add the prizes.")
                                }.awaitSingle()
                                content = null
                            }
                            else
                            {
                                channel().createMessageCompat {
                                    content("Sorry this doesn't look like a timestamp, please try again...")
                                }.awaitSingle()

                                return@takeWhile true
                            }
                        }

                        if (confirm) {
                            if (content == "confirm") {
                                giveawayService.createGiveaway(channel!!, title!!, description!!, end!!, prizes)

                                return@takeWhile false
                            }
                            else
                            {
                                channel().createMessageCompat {
                                    content("Please `confirm` or `cancel` and try again")
                                }.awaitSingle()

                                return@takeWhile true
                            }
                        }

                        if (content == "done") {
                            confirm = true

                            channel().createMessageCompat {
                                content("Okay, here is a preview of the giveaway, please `confirm` or `cancel` and try again")
                            }.awaitSingle()

                            giveawayService.createGiveawayMessage(channel(), title!!, description!!, end!!, prizes, prizes.map { (Math.random()*100).toInt() })

                            return@takeWhile true
                        }

                        if (content != null) {
                            if (prizes.count() >= 5) {
                                channel().createMessageCompat {
                                    content("Sorry you can only add a maximum of 5 prizes.")
                                }.awaitSingle()

                                return@takeWhile true
                            }

                            if (prizeTitle == null) {
                                prizeTitle = content

                                channel().createMessageCompat {
                                    content("Next provide the emoji for the prize `${prizeTitle}`")
                                }.awaitSingle()

                                return@takeWhile true
                            }

                            if (prizeEmoji == null) {
                                prizeEmoji = content.trim().asReaction()

                                channel().createMessageCompat {
                                    content("Great, the emoji will be ${prizeEmoji!!.print()}. And lastly, how many of these prizes are there?")
                                }.awaitSingle()

                                return@takeWhile true
                            }

                            if (prizeCount == null) {
                                prizeCount = content.trim().toIntOrNull()

                                if (prizeCount != null) {
                                    prizes.add(GivewayService.Prize(prizeEmoji!!, prizeTitle!!, prizeCount!!))
                                    prizeTitle = null
                                    prizeEmoji = null
                                    prizeCount = null
                                    content = null
                                }
                                else
                                {
                                    channel().createMessageCompat {
                                        content("Sorry this doesn't look like a number to me, please try again...")
                                    }.awaitSingle()

                                    return@takeWhile true
                                }
                            }
                        }

                        if (content == null) {
                            channel().createMessageCompat {
                                content("Currently the following prizes are defined:\n${prizes.joinToString("\n") { "${it.count} x ${it.emoji.print()} ${it.text}" }}\n\n Please specify the next prize. Answer `done` if all prizes are added.")
                            }.awaitSingle()
                            return@takeWhile true
                        }


                        return@takeWhile false
                    }
                    .onCompletion { timeout?.cancel() }
                    .launch()

                    timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job?.apply { cancel(this) } }
            }
        }


        command("reroll", "reroll some prizes for a existing giveaway") {
            arg("message_id", "the message id of of the giveaway message")

            onCall {
                val messageId = args["message_id"]
                val giveawayId = giveawayService.getGiveawayByMessageId(Snowflake.of(messageId))

                if (giveawayId == null) {
                    channel().createEmbedCompat {
                        description("Sorry, I couldn't find this giveaway.")
                    }.awaitSingle()

                    return@onCall
                }

                data class Reroll(val prize: GivewayService.Prize, var count: Int? = null)
                val prizes = giveawayService
                        .getPrizesByGiveawayId(giveawayId).map { Reroll(it) }

                if (prizes.isEmpty()) {
                    channel().createEmbedCompat {
                        description("Sorry, I couldn't find any prizes for this giveaway.")
                    }.awaitSingle()

                    return@onCall
                }

                val first = prizes.first()

                val author = message.author.get()
                channel().createMessageCompat {
                    content("Sure let's get started, you can cancel the reroll any time by answering `cancel`. How many ${first.prize.emoji.print()} ${first.prize.text} do you want to reroll?")
                }.awaitSingle()

                var timeout: Timer? = null

                var job: Job? = null

                job = client.eventDispatcher.on<MessageCreateEvent>()
                    .filter { it.message.channelId == this@onCall.channel().id && it.message.author.get().id == author.id }
                    .takeWhile { ev ->
                        timeout?.cancel()
                        timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job?.apply { cancel(this) } }

                        val next = ev.message
                        val content: String? = next.content

                        if (content == "cancel") {
                            channel().createMessageCompat {
                                content("Ok, I cancelled the reroll!")
                            }.awaitSingle()

                            return@takeWhile false
                        }

                        for (prize in prizes) {
                            if(prize.count != null) {
                                continue
                            }

                            prize.count = content?.toIntOrNull()

                            if (prize.count != null) {
                                val nextPrize = prizes.filter { it.count == null }.firstOrNull()
                                if (nextPrize == null) {
                                    channel().createMessageCompat {
                                        content("All right, let's recap:\n${prizes.joinToString("\n") { "${it.count} x ${it.prize.emoji.print()} ${it.prize.text}" }}\n\nPlease `confirm` or `cancel` and try again")
                                    }.awaitSingle()
                                    return@takeWhile true
                                }
                                else
                                {
                                    channel().createMessageCompat {
                                        content("Great, and how many ${nextPrize.prize.emoji.print()} ${nextPrize.prize.text} do you want to reroll?.")
                                    }.awaitSingle()
                                    return@takeWhile true
                                }
                            }
                            else
                            {
                                channel().createMessageCompat {
                                    content("Sorry this doesn't look like a number, please try again...")
                                }.awaitSingle()

                                return@takeWhile true
                            }
                        }

                        if (content == "confirm") {
                            giveawayService.rerollPrizes(giveawayId, prizes.map { Pair(it.prize.id, it.count!!) })

                            return@takeWhile false
                        }
                        else
                        {
                            channel().createMessageCompat {
                                content("Please `confirm` or `cancel` and try again")
                            }.awaitSingle()

                            return@takeWhile true
                        }

                    }
                    .onCompletion { timeout?.cancel() }
                    .launch()

                timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job?.apply { cancel(this) } }
            }
        }
    }
}
