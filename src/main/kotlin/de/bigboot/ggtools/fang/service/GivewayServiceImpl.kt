
package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.components.giveaway.ButtonConfirm
import de.bigboot.ggtools.fang.components.giveaway.ButtonEnter
import de.bigboot.ggtools.fang.components.giveaway.GiveawayComponentSpec
import de.bigboot.ggtools.fang.db.*
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.common.util.TimestampFormat
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildChannel
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.concurrent.timer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

private val CONFIRM_DURATION = 2.minutes

class GivewayServiceImpl : GivewayService, AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val database: Database by inject()
    val random = SecureRandom.getInstanceStrong()

    init {
        client.eventDispatcher.on<ComponentInteractionEvent>()
            .filter { it.customId.startsWith(GiveawayComponentSpec.ID_PREFIX) }
            .onEachSafe(this::handleInteraction)
            .launch()


        for ((giveaway, end) in transaction(database) { Giveaway.find { Giveaways.ended eq false }.map { Pair(it.id.value, it.end) } }) {
            createGiveawayTimer(giveaway, end)
        }

        for ((giveaway, end) in transaction(database) {
                GiveawayEntries
                    .slice(GiveawayEntries.giveaway, GiveawayEntries.confirm_until, GiveawayEntries.confirm_until.max())
                    .select { (GiveawayEntries.confirmed eq false) and (GiveawayEntries.confirm_until.isNotNull()) }
                    .groupBy(GiveawayEntries.giveaway)
                    .map { Pair(it[GiveawayEntries.giveaway].value, it[GiveawayEntries.confirm_until.max()]) }
        }) {
            createConfirmTimer(giveaway, end!!)
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonEnter) {
        val result = enterGiveaway(event.interaction.user.id, button.giveaway, button.prize)
        when (result) {
            is Error -> {
                event.replyCompat {
                    content(result.msg)
                    ephemeral(true)
                }.awaitSafe()
            }
            else -> {
                val (title, description, end) = transaction(database) { Giveaway[button.giveaway].load(Giveaway::prizes).let { Triple(it.title, it.description, it.end) } }

                data class PrizeData(val id: UUID, val emoji: String, val text: String, val entries: Long)
                val prizes = transaction(database) {
                    return@transaction (Prizes leftJoin GiveawayEntries)
                        .slice(Prizes.text, Prizes.emoji, Prizes.id, GiveawayEntries.id.count())
                        .select { Prizes.giveaway eq button.giveaway }
                        .groupBy(Prizes.id)
                        .map { PrizeData(it[Prizes.id].value, it[Prizes.emoji], it[Prizes.text], it[GiveawayEntries.id.count()]) }
                }

                event.editCompat {
                    addEmbedCompat {
                        title(title)
                        description(description)

                        for (prize in prizes) {
                            addField(
                                "Press ${prize.emoji} to enter for ${prize.text}",
                                "${prize.entries} Entries",
                                false
                            )
                        }

                        addField("The giveaway will end ${TimestampFormat.RELATIVE_TIME.format(end)}", "", false)
                    }

                    addComponent(ActionRow.of(prizes.map {
                        ButtonEnter(
                            button.giveaway,
                            it.id,
                            it.emoji.asReaction()
                        ).component()
                    }))
                }.awaitSafe()
            }
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonConfirm) {
        val result = confirm(event.interaction.user.id, button.giveaway)
        when (result) {
            is Error -> {
                event.replyCompat {
                    content(result.msg)
                    ephemeral(true)
                }.awaitSafe()
            }
            else -> {
                event.replyCompat {
                    content("Congratulations, you have confirmed your win.")
                    ephemeral(true)
                }.awaitSafe()
            }
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent) {
        ButtonEnter.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonConfirm.parse(event.customId)?.also { handleInteraction(event, it); return }
    }

    private fun drawWinners(id: UUID) = transaction(database) {
        val giveaway = Giveaway[id]

        giveaway.ended = true

        val entries = GiveawayEntry.find { (GiveawayEntries.giveaway eq giveaway.id) and (GiveawayEntries.winner eq false) }
        val entryIds = entries.groupBy { it.prize.id }.mapValues { it.value.map { it.id } }

        val now = Instant.now()
        val winners = entryIds.mapValues {
            val availablePrizes = Prize[it.key].count - GiveawayEntry.find {
                (GiveawayEntries.giveaway eq giveaway.id) and (GiveawayEntries.winner eq true) and (GiveawayEntries.confirmed eq true)
            }.count().toInt()

            it.value
                .shuffled(random)
                .take(availablePrizes)
                .map { Pair(it.value, GiveawayEntry[it].user) }
        }

        GiveawayEntries.update({(GiveawayEntries.giveaway eq giveaway.id) and GiveawayEntries.confirm_until.isNotNull()}) {
            it[confirm_until] = null
        }

        GiveawayEntries.update({GiveawayEntries.id inList winners.flatMap { it.value.map { it.first } }}) {
            it[winner] = true
            it[confirm_until] = now + CONFIRM_DURATION.toJavaDuration()
        }

        return@transaction Pair(giveaway.channel, winners
            .mapKeys { Prize[it.key].let { "${it.emoji} ${it.text}" } }
            .mapValues { it.value.map { it.second } })
    }

    private fun createGiveawayTimer(giveaway: UUID, end: Instant) {
        timer("Giveaway Timer", true, initialDelay = Instant.now().until(end, ChronoUnit.MILLIS).coerceAtLeast(1), period = Long.MAX_VALUE) {
            cancel()
            CoroutineScope(Dispatchers.Default).launch {
                val (channel, result) = drawWinners(giveaway)
                createResultMessage(Snowflake.of(channel), result, giveaway)
            }
        }
    }

    private fun createConfirmTimer(giveaway: UUID, end: Instant) {
        timer("Confirm Timer", true, initialDelay = Instant.now().until(end, ChronoUnit.MILLIS).coerceAtLeast(1), period = Long.MAX_VALUE) {
            cancel()
            val (channel, result) = drawWinners(giveaway)

            if (result.flatMap { it.value }.isNotEmpty()) {
                CoroutineScope(Dispatchers.Default).launch {
                    createResultMessage(Snowflake.of(channel), result, giveaway)
                    createConfirmTimer(giveaway, Instant.now() + CONFIRM_DURATION.toJavaDuration())
                }
            }
        }
    }

    private suspend fun createResultMessage(channelId: Snowflake, result: Map<String, List<Long>>, giveaway: UUID) {
        val channel = client.getChannelById(channelId).awaitSafe() as MessageChannel
        channel.createMessageCompat {
            content(result.flatMap { it.value }.joinToString(" ") {  "<@$it>" })
            addEmbedCompat {
                title("Giveaway Winners!")
                description("Congratulations to the lucky winners of the Giveaway. You have ${TimestampFormat.RELATIVE_TIME.format(
                    Instant.now() + CONFIRM_DURATION.toJavaDuration())} to confirm your win.")

                for ((prize, winners) in result) {
                    addField(prize, winners.joinToString("\n") { "<@$it>" }, false)
                }
            }


            addComponent(ActionRow.of(ButtonConfirm(giveaway).component()))
        }.awaitSafe()

        createConfirmTimer(giveaway, Instant.now() + CONFIRM_DURATION.toJavaDuration())
    }

    override suspend fun createGiveawayMessage(
        channel: MessageChannel,
        title: String,
        description: String,
        end: Instant,
        maxJoindDate: Instant?,
        prizes: List<GivewayService.Prize>,
        entries: List<Int>,
        giveaway: UUID,
    ): Message {
        return channel.createMessageCompat {
            addEmbedCompat {
                title(title)
                description(description)

                for ((prize, i) in prizes.zip(entries)) {
                    addField(
                        "Press ${prize.emoji.print()} to enter for ${prize.text}",
                        "${i} Entries",
                        false
                    )
                }

                addField("The giveaway will end ${TimestampFormat.RELATIVE_TIME.format(end)}", "", false)
            }

            addComponent(ActionRow.of(prizes.map {
                ButtonEnter(
                    giveaway,
                    it.id,
                    it.emoji,
                ).component()
            }))
        }.awaitSingle()
    }

    override suspend fun getWinners(id: UUID) = transaction(database) {
        GiveawayEntry
            .find { (GiveawayEntries.giveaway eq id) and (GiveawayEntries.winner eq true) }
            .groupBy { GivewayService.Prize(it.prize.emoji.asReaction(), it.prize.text, it.prize.count, it.prize.id.value) }
            .mapValues { it.value.map { GivewayService.Winner(Snowflake.of(it.user), when
            {
                it.confirmed -> GivewayService.ConfirmationStatus.Confirmed
                it.confirm_until != null -> GivewayService.ConfirmationStatus.Waiting
                else -> GivewayService.ConfirmationStatus.Missed
            }) } }
            .toList()
    }

    override suspend fun getGiveawayByMessageId(messageId: Snowflake): UUID? = transaction(database) {
        Giveaway.find { Giveaways.message eq messageId.asLong() }.firstOrNull()?.id?.value
    }

    override suspend fun getPrizesByGiveawayId(id: UUID): List<GivewayService.Prize> = transaction(database) {
        Prize.find { Prizes.giveaway eq id }.map { GivewayService.Prize(it.emoji.asReaction(), it.text, it.count, it.id.value) }
    }

    override suspend fun createGiveaway(
        channel: Snowflake,
        title: String,
        description: String,
        end: Instant,
        maxJoinDate: Instant?,
        prizes: List<GivewayService.Prize>
    ): Snowflake {
        val id = UUID.randomUUID()
        val messageChannel = client.getChannelById(channel).awaitSingle() as MessageChannel
        val message = createGiveawayMessage(messageChannel, title, description, end, maxJoinDate, prizes, prizes.map { 0 }, id)

        transaction(database) {
            val giveaway = Giveaway.new(id) {
                this.message = message.id.asLong()
                this.channel = channel.asLong()
                this.title = title
                this.description = description
                this.end = end
                this.maxJoinDate = maxJoinDate
            }

            for (prize in prizes) {
                Prize.new(prize.id) {
                    this.giveaway = giveaway
                    this.emoji = prize.emoji.print()
                    this.text = prize.text
                    this.count = prize.count
                }
            }
        }

        createGiveawayTimer(id, end)

        return message.id
    }

    override suspend fun enterGiveaway(snowflake: Snowflake, giveaway: UUID, prize: UUID): Error? {
        val giveaway = transaction(database) { Giveaway[giveaway] }
        val channel = client.getChannelById(Snowflake.of(giveaway.channel)).awaitSingle() as GuildChannel
        val member = client.getMemberById(channel.guildId, snowflake).awaitSingle()

        if (giveaway.ended) {
            return Error("This giveaway already ended")
        }

        if (giveaway.maxJoinDate?.isBefore(member.joinTime.orNull() ?: Instant.MAX) == true)
        {
            return Error("Sorry but you are not eligible to enter this giveaway as you joined this server too recently.")
        }

        return transaction(database) {
            val entry = GiveawayEntry.find { (GiveawayEntries.user eq snowflake.asLong()) and (GiveawayEntries.giveaway eq giveaway.id) }
                .firstOrNull() ?: GiveawayEntry.new {

                this.giveaway = giveaway
                this.prize = Prize[prize]
                this.user = snowflake.asLong()
            }

            entry.prize = Prize[prize]

            return@transaction null
        }
    }

    override suspend fun confirm(snowflake: Snowflake, giveaway: UUID) = transaction(database) {
        val entry = GiveawayEntry.find { (GiveawayEntries.giveaway eq giveaway) and (GiveawayEntries.user eq snowflake.asLong()) }.singleOrNull()

        if (entry == null || !entry.winner) {
            return@transaction Error("Sorry but you did not win in this giveaway.")
        }

        if (entry.confirmed) {
            return@transaction Error("You already confirmed your entry.")
        }

        if (entry.confirm_until?.isBefore(Instant.now()) ?: true) {
            return@transaction Error("Sorry but the window for confirmation has already ended.")
        }

        entry.confirmed = true
        entry.confirm_until = null

        return@transaction null
    }
}