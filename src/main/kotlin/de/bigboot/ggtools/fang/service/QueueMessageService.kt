package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.`object`.component.ActionComponent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.component.SelectMenu
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.TextChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.math.max
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface ComponentSpec {
    fun id(): String
    fun component(): ActionComponent

    companion object {
        val ID_PREFIX = "QUEUE_MESSAGE"
    }
}

data class ButtonLeave(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent = Button.danger(id(), "Leave")

    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_BUTTON_LEAVE"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonLeave(queue) }
    }
}

data class ButtonJoin(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent  = Button.success(id(), "Join")

    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_BUTTON_JOIN"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonJoin(queue) }
    }
}

class ButtonPreferences(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent  = Button.secondary(id(), "Preferences")
    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_BUTTON_PREFERENCES_BASE"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonPreferences(queue) }
    }
}

class ButtonUpdateServerPreference(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent  = Button.secondary(id(), "Update server preferences")
    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_BUTTON_PREFERENCES_SERVER"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonUpdateServerPreference(queue) }
    }
}

class ButtonToggleDMNotifications(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent  = Button.secondary(id(), "Toggle DM notifications")
    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_BUTTON_PREFERENCES_DM_NOTIFICATIONS"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> ButtonToggleDMNotifications(queue) }
    }
}

class SelectServerPreference(val queue: String): ComponentSpec {
    override fun id() = "${PREFIX}_${queue}"
    override fun component(): ActionComponent = SelectMenu.of(id(),
        SelectMenu.Option.of("EU", "EU").withEmoji(Config.emojis.server_pref_eu.asReaction()),
        SelectMenu.Option.of("NA", "NA").withEmoji(Config.emojis.server_pref_na.asReaction()),
    ).withMaxValues(2).withPlaceholder("Select server locations")

    companion object {
        private val PREFIX = "${ComponentSpec.ID_PREFIX}_SELECT_PREFERENCES_SERVER"
        private val ID_REGEX = Regex("${PREFIX}_(.+)")
        fun parse(id: String) = ID_REGEX.find(id)?.destructured?.let { (queue) -> SelectServerPreference(queue) }
    }
}


class QueueMessageService : AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val matchService by inject<MatchService>()
    private val setupGuildService by inject<SetupGuildService>()
    private val preferencesService by inject<PreferencesService>()

    private val updateQueueTimer = Timer(true)

    init {
        client.eventDispatcher.on<ReactionAddEvent>()
            .filter { it.message.awaitSingle().run {
                isFromSelf() && hasReacted(Config.emojis.match_finished.asReaction())
            } }
            .onEachSafe(this::handleReactMatchFinished)
            .launch()

        client.eventDispatcher.on<ComponentInteractionEvent>()
            .filter { it.customId.startsWith(ComponentSpec.ID_PREFIX) }
            .onEachSafe(this::handleInteraction)
            .launch()

        CoroutineScope(Dispatchers.Default).launch {

            for (queue in Config.bot.queues) {
                val queueMessage = setupGuildService.getQueueMessage(queue.name)

                client.eventDispatcher.on<ReactionAddEvent>()
                    .filter { it.messageId == queueMessage.msgId && it.channelId == queueMessage.channelId }
                    .onEachSafe { updateQueueMessage(queue.name, it.message.awaitSingle()) }
                    .launch()
            }

            updateQueueTimer.schedule(object : TimerTask() {
                override fun run() {
                    runBlocking(Dispatchers.IO) {
                        for (queue in Config.bot.queues) {
                            val queueMessage = setupGuildService.getQueueMessage(queue.name)
                            updateQueueMessage(
                                queue.name,
                                client.getMessageById(queueMessage.channelId, queueMessage.msgId).awaitSingle(),
                            )
                        }
                    }
                }
            }, 0, 30000)

            updateQueueTimer.schedule(object : TimerTask() {
                override fun run() {
                    runBlocking(Dispatchers.IO) {
                        for (queue in Config.bot.queues) {
                            val queueMessage = setupGuildService.getQueueMessage(queue.name)
                            if (matchService.canPop(queue.name)) {
                                val channel =  client.getChannelById(queueMessage.channelId).awaitSingle() as MessageChannel
                                handleQueuePop(queue.name, matchService.pop(queue.name), channel)
                            }
                        }
                    }
                }
            }, 0, 5000)
        }
    }

    private suspend fun updateQueueMessage(queue: String)
    {
        setupGuildService.getQueueMessage(queue).let { msg ->
            client.getMessageById(msg.channelId, msg.msgId)
        }.awaitSafe()?.also { updateQueueMessage(queue, it) }
    }

    private suspend fun updateQueueMessage(queue: String, msg: Message) {
        val newMsgContent =
            """
            | ${matchService.printQueue(queue)}
            |  
            """.trimMargin()

        if (newMsgContent != msg.embeds.firstOrNull()?.description?.orNull()) {
            msg.editCompat {
                addEmbedCompat {
                    title("${matchService.getNumPlayers(queue)} players waiting in queue")
                    description(newMsgContent)
                }
                addComponent(ActionRow.of(
                    ButtonJoin(queue).component(),
                    ButtonLeave(queue).component(),
                    ButtonPreferences(queue).component(),
                ))
            }.awaitSingle()
        }
    }

    private suspend fun handleQueuePop(queue: String, pop: MatchService.Pop, channel: MessageChannel) {
        val players = pop.players + pop.previousPlayers
        val canDeny = pop.request != null
        val requiredPlayers = pop.request?.minPlayers ?: Config.bot.required_players

        val message = channel.createMessageCompat {
            content(players.joinToString(" ") { player -> "<@$player>" })
        }.awaitSingle()

        message.addReaction(Config.emojis.accept.asReaction()).await()

        if (canDeny) {
            message.addReaction(Config.emojis.deny.asReaction()).await()
        }

        val content = when {
            pop.request != null -> "A ${pop.request.minPlayers} player match has been requested by <@${pop.request.player}>." +
                    "Please react with a ${Config.emojis.accept} to accept the match. If you want to deny the match please react with a ${Config.emojis.deny}. " +
                    "You have ${Config.bot.accept_timeout} seconds to accept or deny, otherwise you will be removed from the queue."

            else -> "A match is ready, please react with a ${Config.emojis.accept} to accept the match. " +
                    "You have ${Config.bot.accept_timeout} seconds to accept, otherwise you will be removed from the queue."
        }

        CoroutineScope(Dispatchers.IO).launch {
            for (player in pop.players) {
                notifyPlayer(Snowflake.of(player), channel.id)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val (missing, accepted, denied) = waitForQueuePopResponses(message, pop, content)

            when {
                accepted.count() >= requiredPlayers -> {
                    message.editCompat {
                        addEmbedCompat {
                            title("Match ready!")
                            description("""
                                |Everybody get ready, you've got a match.
                                |Have fun!
                                |
                                |Please react with a ${Config.emojis.match_finished} after the match is finished.
                                |React with a ${Config.emojis.match_drop} to drop out after this match."""
                                .trimMargin()
                            )
                            addField("Best server location", when(pop.server) {
                                "NA" -> Config.emojis.server_pref_na
                                "EU" -> Config.emojis.server_pref_eu
                                else -> pop.server ?: "None"
                            }, true)

                            addField("Players", players.joinToString(" ") { "<@$it>" }, false)
                        }
                    }.awaitSingle()

                    message.removeAllReactions().await()

                    message.addReaction(Config.emojis.match_drop.asReaction()).awaitSafe()
                    message.reactAfter(3.minutes, Config.emojis.match_finished.asReaction())
                    message.deleteAfter(90.minutes) {
                        accepted.forEach {
                            matchService.leave(queue, it, true)
                        }
                    }
                }
                matchService.getNumPlayers(queue) + accepted.size >= requiredPlayers -> {
                    val repop = matchService.pop(queue, pop.server, accepted)
                    message.delete().await()
                    handleQueuePop(queue, repop, channel)
                }
                else -> {
                    message.editCompat {
                        addEmbedCompat {
                            title("Match Cancelled!")
                            description("Not enough players accepted the match")
                        }
                    }.awaitSingle()

                    for (player in accepted) {
                        matchService.join(queue, player)
                    }
                    message.deleteAfter(5.minutes)
                }
            }

            for (player in denied) {
                matchService.join(queue, player)
            }

            for (player in missing) {
                matchService.leave(queue, player, resetScore = false)
            }
        }
    }

    private data class PopResonse(
        val missing: Collection<Long>,
        val accepted: MutableCollection<Long>,
        val denied: MutableCollection<Long>,
    )

    private suspend fun waitForQueuePopResponses(message: Message, pop: MatchService.Pop, messageContent: String): PopResonse {
        val endTime = System.currentTimeMillis() + (Config.bot.accept_timeout * 1000L)
        val missing = HashSet(pop.players)
        val accepted = HashSet(pop.previousPlayers)
        val denied = HashSet<Long>()
        val requiredPlayers = pop.request?.minPlayers ?: Config.bot.required_players

        while (true) {
            message.getReactors(Config.emojis.accept.asReaction())
                .filter { missing.contains(it.id.asLong()) }
                .collectList()
                .awaitSingle()
                .onEach { accepted.add(it.id.asLong()) }
                .onEach { missing.remove(it.id.asLong()) }

            message.getReactors(Config.emojis.deny.asReaction())
                .filter { missing.contains(it.id.asLong()) }
                .collectList()
                .awaitSingle()
                .onEach { denied.add(it.id.asLong()) }
                .onEach { missing.remove(it.id.asLong()) }

            if (System.currentTimeMillis() >= endTime || accepted.count() >= requiredPlayers || missing.isEmpty()) {
                break
            }

            message.editCompat {
                addEmbedCompat {
                    title("Match found!")
                    description(messageContent)
                    addField("Time remaining: ", "${max(0, (endTime - System.currentTimeMillis()) / 1000)}", true)
                    if (missing.isNotEmpty()) {
                        addField(
                            "Missing players: ",
                            missing.joinToString(" ") { player -> "<@$player>" },
                            true
                        )
                    }
                }
            }.awaitSingle()

            delay(1000)
        }

        return PopResonse(missing, accepted, denied)
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent)
    {
        ButtonJoin.parse(event.customId)?.also {
            event.deferEdit().awaitSafe()
            matchService.join(it.queue, event.interaction.user.id.asLong())
            updateQueueMessage(it.queue)
            return
        }

        ButtonLeave.parse(event.customId)?.also {
            event.deferEdit().awaitSafe()
            matchService.leave(it.queue, event.interaction.user.id.asLong())
            updateQueueMessage(it.queue)
            return
        }

        ButtonToggleDMNotifications.parse(event.customId)?.also {
            event.deferEdit().awaitSafe()
            preferencesService.toggleDirectMessageNotifications(event.interaction.user.id.asLong())
            updateQueueMessage(it.queue)
            return
        }

        ButtonPreferences.parse(event.customId)?.also {
            event.replyCompat {
                addComponent(ActionRow.of(
                    ButtonToggleDMNotifications(it.queue).component(),
                    ButtonUpdateServerPreference(it.queue).component(),
                ))
                ephemeral(true)
            }.await()
            return
        }

        ButtonUpdateServerPreference.parse(event.customId)?.also {
            event.replyCompat {
                addComponent(ActionRow.of(
                    SelectServerPreference(it.queue).component(),
                ))
                ephemeral(true)
            }.await()
            updateQueueMessage(it.queue)
            return
        }

        SelectServerPreference.parse(event.customId)?.also {
            val result = (event as SelectMenuInteractionEvent).values
            preferencesService.setPreferences(event.interaction.user.id.asLong(), PreferencesService.UpdatePreferences(
                preferredServers = result.toSet()
            ))
            event.deferEdit().awaitSafe()
            updateQueueMessage(it.queue)
            return
        }

    }

    private suspend fun handleReactMatchFinished(event: ReactionAddEvent) {
        val message = event.message.awaitSingle()
        val channel = event.channel.awaitSingle() as? TextChannel

        val queue = Config.bot.queues.find { it.channel == channel?.name }?.name ?: return

        val players = message
            .embeds
            .mapNotNull { embed ->
                embed
                    .fields
                    .find { it.name == "Players" }
                    ?.value
                    ?.split(" ")
                    ?.map { it.substringAfter("<@").substringBefore(">") }
                    ?.mapNotNull { it.toLongOrNull() }
            }
            .flatten()

        val finished = message.getReactors(Config.emojis.match_finished.asReaction())
            .map { it.id.asLong() }
            .await()
            .toSet()

        val dropped = message.getReactors(Config.emojis.match_drop.asReaction())
            .map { it.id.asLong() }
            .await()
            .toSet()

        if (players.intersect(finished).size >= 3) {

            for (player in (players - dropped)) {
                matchService.join(queue, player)
            }

            for (player in dropped) {
                matchService.leave(queue, player)
            }

            message.editCompat {
                addEmbedCompat {
                    title("Match finished!")
                    description("Players have rejoined the queue. Let's have another one!")
                }
            }.awaitSingle()

            message.removeAllReactions().await()
            message.deleteAfter(60.seconds)
        }
    }

    private suspend fun notifyPlayer(player: Snowflake, channel: Snowflake) {
        if (preferencesService.getPreferences(player.asLong()).dmNotifications) {
            client.getUserById(player).awaitSafe()
                ?.privateChannel?.awaitSafe()
                ?.createMessageCompat {
                    content("There's a match ready for you, please head over to <#${channel.asLong()}>")
                }?.awaitSafe()
        }
    }
}
