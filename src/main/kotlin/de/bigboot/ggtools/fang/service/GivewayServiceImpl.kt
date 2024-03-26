
package de.bigboot.ggtools.fang.service

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
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonEnter) {
        if (enterGiveaway(event.interaction.user.id, button.giveaway, button.prize)) {
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

    private suspend fun handleInteraction(event: ComponentInteractionEvent) {
        ButtonEnter.parse(event.customId)?.also { handleInteraction(event, it); return }
    }

    private fun createGiveawayTimer(giveaway: UUID, end: Instant) {
        timer("Giveaway Timer", true, initialDelay = Instant.now().until(end, ChronoUnit.MILLIS).coerceAtLeast(1), period = Long.MAX_VALUE) {
            cancel()
            val (channel, result) = transaction(database) {
                val giveaway = Giveaway[giveaway]

                giveaway.ended = true

                val entries = GiveawayEntry.find { GiveawayEntries.giveaway eq giveaway.id }
                val entryIds = entries.groupBy { it.prize.id }.mapValues { it.value.map { it.id } }

                val winners = entryIds.mapValues { it.value.shuffled(random).take(Prize[it.key].count).map { Pair(it.value, GiveawayEntry[it].user) } }
                GiveawayEntries.deleteWhere { GiveawayEntries.id inList winners.flatMap { it.value.map { it.first } }}

                return@transaction Pair(giveaway.channel, winners
                    .mapKeys { Prize[it.key].let { "${it.emoji} ${it.text}" } }
                    .mapValues { it.value.map { it.second } })

            }
            CoroutineScope(Dispatchers.Default).launch {
                createResultMessage(Snowflake.of(channel), result)
            }
        }
    }

    private suspend fun createResultMessage(channelId: Snowflake, result: Map<String, List<Long>>) {
        val channel = client.getChannelById(channelId).awaitSafe() as MessageChannel
        channel.createMessageCompat {
            content(result.flatMap { it.value }.joinToString(" ") {  "<@$it>" })
            addEmbedCompat {
                title("Giveaway Winners!")
                description("Congratulations to the lucky winners of the Giveaway")

                for ((prize, winners) in result) {
                    addField(prize, winners.joinToString("\n") { "<@$it>" }, false)
                }
            }
        }.awaitSafe()
    }

    override suspend fun createGiveawayMessage(
        channel: MessageChannel,
        title: String,
        description: String,
        end: Instant,
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
        }.awaitSingle()!!
    }

    override suspend fun rerollPrizes(giveaway: UUID, prizes: List<Pair<UUID, Int>>) {
        val (channel, result) = transaction(database) {
            val giveaway = Giveaway[giveaway]
            val prizes = prizes.toMap()

            val entries = GiveawayEntry.find { GiveawayEntries.giveaway eq giveaway.id }
            val entryIds = entries.groupBy { it.prize.id }.mapValues { it.value.map { it.id } }

            val winners = entryIds.mapValues { it.value.shuffled(random).take(prizes[it.key.value]!!).map { Pair(it.value, GiveawayEntry[it].user) } }
            GiveawayEntries.deleteWhere { GiveawayEntries.id inList winners.flatMap { it.value.map { it.first } }}

            return@transaction Pair(giveaway.channel, winners
                .mapKeys { Prize[it.key].let { "${it.emoji} ${it.text}" } }
                .mapValues { it.value.map { it.second } })

        }
        CoroutineScope(Dispatchers.Default).launch {
            createResultMessage(Snowflake.of(channel), result)
        }
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
        prizes: List<GivewayService.Prize>
    ): Snowflake {
        val id = UUID.randomUUID()
        val messageChannel = client.getChannelById(channel).awaitSingle() as MessageChannel
        val message = createGiveawayMessage(messageChannel, title, description, end, prizes, prizes.map { 0 }, id)

        transaction(database) {
            val giveaway = Giveaway.new(id) {
                this.message = message.id.asLong()
                this.channel = channel.asLong()
                this.title = title
                this.description = description
                this.end = end
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

    override suspend fun enterGiveaway(snowflake: Snowflake, giveaway: UUID, prize: UUID): Boolean = transaction(database) {
        val entry = GiveawayEntry.find { (GiveawayEntries.user eq snowflake.asLong()) and (GiveawayEntries.giveaway eq giveaway) }
            .firstOrNull() ?: GiveawayEntry.new {

            this.giveaway = Giveaway[giveaway]
            this.prize = Prize[prize]
            this.user = snowflake.asLong()
        }

        entry.prize = Prize[prize]

        return@transaction true
    }
}