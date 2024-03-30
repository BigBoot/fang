package de.bigboot.ggtools.fang.commands.giveway

import de.bigboot.ggtools.fang.CommandContext
import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.service.GivewayService
import de.bigboot.ggtools.fang.service.GivewayService.ConfirmationStatus.*
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.common.util.TimestampFormat
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.rest.http.client.ClientException
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

                var channel: Optional<Snowflake> = Optional.empty()
                var title: Optional<String> = Optional.empty()
                var description: Optional<String> = Optional.empty()
                var end: Optional<Instant> = Optional.empty()
                var maxJoinDate: Optional<Optional<Instant>> = Optional.empty()
                val prizes: MutableList<GivewayService.Prize> = mutableListOf()

                var prizeTitle: Optional<String> = Optional.empty()
                var prizeEmoji: Optional<ReactionEmoji> = Optional.empty()
                var prizeCount:Optional< Int> = Optional.empty()

                var confirm = false

                var job: Job? = null

                job = client.eventDispatcher.on<MessageCreateEvent>()
                    .filter { it.message.channelId == this@onCall.channel().id && it.message.author.get().id == author.id }
                    .takeWhile { ev ->
                        timeout?.cancel()
                        timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job?.apply { cancel(this) } }

                        val next = ev.message
                        var content: String? = next.content

                        if (content?.lowercase() == "cancel") {
                            channel().createMessageCompat {
                                content("Ok, I cancelled the giveaway!")
                            }.awaitSingle()

                            return@takeWhile false
                        }

                        if (channel.isEmpty)
                        {
                            channel = Optional.ofNullable(guild().findChannel(content.orEmpty())?.id)

                            if (channel.isPresent) {
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

                        if (title.isEmpty)
                        {
                            title = Optional.ofNullable(content)

                            channel().createMessageCompat {
                                content("Okay, now please tell me the description for the giveaway.")
                            }.awaitSingle()

                            return@takeWhile true
                        }

                        if (description.isEmpty)
                        {
                            description = Optional.ofNullable(content)

                            channel().createMessageCompat {
                                content("Done, and when should the giveaway end? ([Unix Epoch Timestamp](https://www.unixtimestamp.com/))")
                            }.awaitSingle()

                            return@takeWhile true
                        }

                        if (end.isEmpty) {
                            end = Optional.ofNullable(content?.toLongOrNull()?.let { Instant.ofEpochSecond(it) })

                            if (end.isPresent) {
                                channel().createMessageCompat {
                                    content("Great, the giveaway will end ${TimestampFormat.RELATIVE_TIME.format(end.get())}.\nShould participants be required to have joined the server before a specific date? If so enter the timestamp, other answer `no`.")
                                }.awaitSingle()
                            }
                            else
                            {
                                channel().createMessageCompat {
                                    content("Sorry this doesn't look like a timestamp, please try again...")
                                }.awaitSingle()
                            }

                            return@takeWhile true
                        }

                        if (maxJoinDate.isEmpty) {
                            //
                            maxJoinDate = Optional.ofNullable(when {
                                next.content.lowercase() == "no" -> Optional.empty()
                                else -> content?.toLongOrNull()?.let { Optional.of(Instant.ofEpochSecond(it)) }
                            })

                            if (maxJoinDate.isPresent) {
                                if (maxJoinDate.get().isPresent) {
                                    channel().createMessageCompat {
                                        content("Ok, participants will be required to have joined the server before ${TimestampFormat.RELATIVE_TIME.format(maxJoinDate.get().get())}.\nNow we just need to add the prizes..")
                                    }.awaitSingle()
                                }
                                else
                                {
                                    channel().createMessageCompat {
                                        content("Ok, participants will not be required to have joined the server before a specific date.\nNow we just need to add the prizes..")
                                    }.awaitSingle()
                                }

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
                                giveawayService.createGiveaway(
                                    channel.get(),
                                    title.get(),
                                    description.get(),
                                    end.get(),
                                    maxJoinDate.get().orNull(),
                                    prizes
                                )

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

                            try {
                                giveawayService.createGiveawayMessage(
                                    channel(),
                                    title.get(),
                                    description.get(),
                                    end.get(),
                                    maxJoinDate.get().orNull(),
                                    prizes,
                                    prizes.map { (Math.random()*100).toInt() },
                                )
                            } catch (ex: ClientException) {
                                channel().createMessageCompat {
                                    content("Sorry I was unable to create this giveaway: ${ex.errorResponse.orNull()?.fields}")
                                }.awaitSingle()

                                return@takeWhile false
                            }

                            return@takeWhile true
                        }

                        if (content != null) {
                            if (prizes.count() >= 5) {
                                channel().createMessageCompat {
                                    content("Sorry you can only add a maximum of 5 prizes.")
                                }.awaitSingle()

                                return@takeWhile true
                            }

                            if (prizeTitle.isEmpty) {
                                prizeTitle = Optional.ofNullable(content)

                                channel().createMessageCompat {
                                    content("Next provide the emoji for the prize `${prizeTitle.get()}`")
                                }.awaitSingle()

                                return@takeWhile true
                            }

                            if (prizeEmoji.isEmpty) {
                                prizeEmoji = Optional.ofNullable(content.trim().asReactionOrNull())

                                if (prizeEmoji.isPresent) {
                                    channel().createMessageCompat {
                                        content("Great, the emoji will be ${prizeEmoji.get().print()}. And lastly, how many of these prizes are there?")
                                    }.awaitSingle()
                                }
                                else
                                {
                                    channel().createMessageCompat {
                                        content("Sorry this doesn't look like a valid emoji to me, please try again...")
                                    }.awaitSingle()
                                }

                                return@takeWhile true
                            }

                            if (prizeCount.isEmpty) {
                                prizeCount = Optional.ofNullable(content.trim().toIntOrNull())

                                if (prizeCount.isPresent) {
                                    prizes.add(GivewayService.Prize(prizeEmoji.get(), prizeTitle.get(), prizeCount.get()))
                                    prizeTitle = Optional.empty()
                                    prizeEmoji = Optional.empty()
                                    prizeCount = Optional.empty()
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

                    timeout = timer(period = TIMEOUT, initialDelay = TIMEOUT) { job.apply { cancel(this) } }
            }
        }

        command("winners", "Get the list of winners for a giveaway") {
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

                val result = giveawayService.getWinners(giveawayId)

                channel().createMessageCompat {
                    val sb = StringBuilder("Here's the list of winners:\n")

                    for ((prize, winners) in result) {
                        sb.appendLine("${prize.emoji.print()} ${prize.text}:")
                        for(winner in winners.sortedBy { arrayOf(Confirmed, Waiting, Missed).indexOf(it.status) }) {
                            val user = client.getUserById(winner.snowflake).awaitSingle()
                            val status = when (winner.status) {
                                Waiting -> "\u23F1"
                                GivewayService.ConfirmationStatus.Confirmed -> "\u2705"
                                Missed -> "\uD83D\uDEAB"
                            }
                            sb.appendLine("- $status ${user.username} (${user.mention})")
                        }

                        content(sb.toString())
                    }
                }.awaitSafe()
            }
        }
    }
}
