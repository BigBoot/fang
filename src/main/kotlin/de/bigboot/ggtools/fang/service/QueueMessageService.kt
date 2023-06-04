package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.api.agent.model.StartRequest
import de.bigboot.ggtools.fang.api.agent.model.StartResponse
import de.bigboot.ggtools.fang.api.agent.model.ResultRequest
import de.bigboot.ggtools.fang.api.agent.model.ResultResponse
import de.bigboot.ggtools.fang.components.queue.QueueComponentSpec
import de.bigboot.ggtools.fang.components.queue.*
import de.bigboot.ggtools.fang.utils.*
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionCallbackSpec
import discord4j.core.spec.InteractionReplyEditSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class MatchState {
    QUEUE_POP,
    MAP_VOTE,
    MATCH_READY,
}

data class MatchRequest(
    val queue: String,
    val popEndTime: Instant,
    val pop: MatchService.Pop,
    val missingPlayers: MutableSet<Long>,
    val matchReady: CompletableFuture<Unit>,
    var message: Message,
    val finishedPlayers: MutableSet<Long> = mutableSetOf(),
    val dropPlayers: MutableSet<Long> = mutableSetOf(),
    val mapVotes: MutableMap<Long, String> = mutableMapOf(),
    var mapVoteEnd: Instant = Instant.now(),
    var declined: Boolean = false,
    var serverSetupPlayer: Snowflake? = null,
    var server: String? = null,
    var openUrl: String? =null,
    var creatures: Triple<String?, String?, String?> = Triple(null, null, null),
    var state: MatchState = MatchState.QUEUE_POP,
    var timeToJoin: Instant? = null,
    var teams: Pair<List<Long>, List<Long>>? = null,
    var ranked: Snowflake? = null
) {
    fun getMapVoteResult() = mapVotes
        .values
        .groupBy { it }
        .toList()
        .map { Pair(it.first, it.second.size) }
        .maxWithOrNull(compareBy<Pair<String, Int>> { it.second }.thenByDescending { it.first })
        ?.first ?: Maps.CANYON.id
}

class QueueMessageService : AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val matchService by inject<MatchService>()
    private val setupGuildService by inject<SetupGuildService>()
    private val preferencesService by inject<PreferencesService>()
    private val serverService by inject<ServerService>()
    private val ratingService by inject<RatingService>()

    private val matchReuests = hashMapOf<UUID, MatchRequest>()

    init {
        client.eventDispatcher.on<ComponentInteractionEvent>()
            .filter { it.customId.startsWith(QueueComponentSpec.ID_PREFIX) }
            .onEachSafe(this::handleInteraction)
            .launch()

        CoroutineScope(Dispatchers.Default).launch {
            for (queue in Config.bot.queues) {
                updateQueueMessage(queue.name)
            }
        }
    }

    private suspend fun updateQueueMessage(queue: String)
    {
        setupGuildService.getQueueMessage(queue).let { msg ->
            client.getMessageById(msg.channelId, msg.msgId)
        }.awaitSafe()?.also { updateQueueMessage(queue, it) }
    }

    private suspend fun updateQueueMessage(queue: String, msg: Message) {
        if (matchService.canPop(queue)) {
            val channel =  client.getChannelById(msg.channelId).awaitSingle() as MessageChannel
            handleQueuePop(queue, matchService.pop(queue), channel)
        }

        msg.editCompat {
            addEmbedCompat { printQueue(queue, this) }

            addComponent(ActionRow.of(
                ButtonJoin(queue).component(),
                ButtonLeave(queue).component(),
                ButtonPreferences(queue).component(),
            ))
        }.awaitSingle()
    }

    private suspend fun updateMapVoteMessage(matchId: UUID)
    {
        val request = matchReuests[matchId] ?: return

        if(request.state != MatchState.MAP_VOTE) return

        request.message.editCompat {
            addEmbedCompat {
                title("Map vote")

                for (map in Maps.FINISHED) {
                    val count = request.mapVotes.filter { it.value == map.id }.count()
                    addField(map.name, count.toString(), true)
                }

                addField(
                    "Time remaining",
                    "<t:${request.mapVoteEnd.epochSecond}:R>",
                    false,
                )
            }

            addComponent(ActionRow.of(Maps.FINISHED.map { ButtonMapVote(matchId, it.id).component() }))
        }.awaitSafe()
    }

    private suspend fun updateMatchReadyMessage(matchId: UUID)
    {
        val request = matchReuests[matchId] ?: return

        if (request.teams == null) {
            request.teams = ratingService.makeTeams(request.pop.allPlayers.toList());
        }

        if(request.state != MatchState.MATCH_READY) return

        request.message.editCompat {
            addEmbedCompat {
                val components = mutableListOf(ButtonMatchFinished(matchId), ButtonMatchDrop(matchId))

                if (Config.bot.rating && request.ranked == null) {
                    components.add(ButtonMatchUnranked(matchId, false));
                }

                title("Match ready!")
                description("""
                    |Everybody get ready, you've got a match.
                    |Have fun!
                    |
                    |Please react with a ${Config.emojis.match_finished} after the match is finished.
                    |React with a ${Config.emojis.match_drop} to drop out after this match."""
                    .trimMargin()
                )
                addField("Best server location", when(request.pop.server) {
                    "NA" -> Config.emojis.server_pref_na
                    "EU" -> Config.emojis.server_pref_eu
                    else -> request.pop.server ?: "None"
                }, true)

                addField("Map", Maps.fromId(request.getMapVoteResult())!!.name, true)
                
                addField("Team Differential", String.format("%.1f%%", ratingService.teamDifferential(request.teams!!)*100-100), true)


                if(request.serverSetupPlayer != null) {
                    val value = when {
                        request.openUrl != null -> if (Config.bot.rating) {
                                                       "`open ${request.openUrl}?team=0`\n`open ${request.openUrl}?team=1`\nby <@${request.serverSetupPlayer!!.asLong()}>"
                                                   }
                                                   else {
                                                       "`open ${request.openUrl}`\nby <@${request.serverSetupPlayer!!.asLong()}>"
                                                   }
                        else -> "Being set up by <@${request.serverSetupPlayer!!.asLong()}>"
                    }
                    addField("Server", value, false)
                } else {
                    components.add(ButtonMatchSetupServer(matchId))
                }

                if (Config.bot.rating) {
                    addField("Team 0", request.teams!!.first.joinToString(" ") { "<@$it>" }, false)
                    addField("Team 1", request.teams!!.second.joinToString(" ") { "<@$it>" }, false)
                }

                if (request.ranked != null) {
                    addField("Set Unraked by", "<@${request.ranked!!.asLong()}>", false)
                }

                if(request.timeToJoin != null) {
                    addField("Time to join", when {
                        Instant.now().compareTo(request.timeToJoin) >= 0 -> "If someone is still not in please report them."
                        else -> "<t:${request.timeToJoin!!.epochSecond}:R>" 
                    }, false)
                }

                addComponent(ActionRow.of(components.map { it.component() }))
            }
        }.awaitSingle()
    }

    private suspend fun handleQueuePop(queue: String, pop: MatchService.Pop, channel: MessageChannel) {
        val canDeny = pop.request != null
        val endTime = Instant.now().plusSeconds(Config.bot.accept_timeout.toLong())

        val matchId = UUID.randomUUID()
        val matchReady = CompletableFuture<Unit>()

        val message = channel.createMessageCompat {
            content(pop.players.joinToString(" ") { player -> "<@$player>" })

            addEmbedCompat {
                printQueuePop(pop, endTime, pop.players, this)
            }

            addComponent(ActionRow.of(when(canDeny) {
                true -> listOf(ButtonAccept(matchId), ButtonDecline(matchId))
                else -> listOf(ButtonAccept(matchId))
            }.map { it.component() }.toMutableList()))
        }.awaitSafe() ?: return

        matchReuests[matchId] = MatchRequest(queue, endTime, pop, pop.players.toMutableSet(), matchReady, message)

        CoroutineScope(Dispatchers.IO).launch {
            for (player in pop.players) {
                notifyPlayer(Snowflake.of(player), channel.id)
            }
        }

        matchReady.completeOnTimeout(null, Config.bot.accept_timeout.toLong(), TimeUnit.SECONDS)

        CoroutineScope(Dispatchers.Default).launch {
            matchReady.await()
            handleQueuePopFinished(matchId)
        }
    }

    private suspend fun handleQueuePopFinished(matchId: UUID) {
        val request = matchReuests[matchId] ?: return

        if(request.missingPlayers.isNotEmpty() || request.declined)
        {
            handleMatchCancelled(request)
            matchReuests.remove(matchId)
            return
        }

        request.state = MatchState.MAP_VOTE
        request.mapVoteEnd = Instant.now().plusSeconds(Config.bot.mapvote_time.toLong())

        request.message.delete().await()
        matchReuests[matchId]!!.message = request.message.channel.awaitSingle().createMessageCompat {
            content(request.pop.allPlayers.joinToString(" ") { "<@$it>" })
        }.awaitSingle()
        
        updateMapVoteMessage(matchId)

        CoroutineScope(Dispatchers.Default).launch {
            delay(Config.bot.mapvote_time.seconds)
            handleMapVoteFinished(matchId)
        }
    }

    private suspend fun handleMapVoteFinished(matchId: UUID) {
        val request = matchReuests[matchId] ?: return
        
        request.state = MatchState.MATCH_READY
        updateMatchReadyMessage(matchId)

        request.message.deleteAfter(90.minutes) {
            for (player in request.pop.allPlayers) {
                matchService.leave(request.queue, player, true)
            }
            matchReuests.remove(matchId)
        }
    }

    private suspend fun handleMatchCancelled(request: MatchRequest) {
        val message = request.message
        val accepted =  request.pop.allPlayers - request.missingPlayers

        for (player in request.missingPlayers) {
            matchService.leave(request.queue, player)
        }

        if(matchService.getNumPlayers(request.queue, request.pop.server) >= request.missingPlayers.size) {
            val repop = matchService.pop(request.queue, request.pop.server, accepted)
            message.delete().await()
            handleQueuePop(request.queue, repop, message.channel.awaitSingle())
        } else {
            message.editCompat {
                addEmbedCompat {
                    title("Match Cancelled!")
                    description("Not enough players accepted the match")
                }
                addAllComponents(emptyList())
            }.awaitSingle()

            for (player in accepted) {
                matchService.join(request.queue, player, true)
            }

            message.deleteAfter(5.minutes)
        }

        updateQueueMessage(request.queue)
    }

    private suspend fun handleMatchFinished(request: MatchRequest) {
        for (player in request.pop.allPlayers - request.dropPlayers) {
            matchService.join(request.queue, player, true)
        }

        for (player in request.dropPlayers) {
            matchService.leave(request.queue, player)
        }

        request.message.editCompat {
            addEmbedCompat {
                title("Match finished!")
                description("Players have rejoined the queue. Let's have another one!")
            }
            addAllComponents(emptyList())
        }.awaitSingle()

        request.message.deleteAfter(60.seconds)

        updateQueueMessage(request.queue)
    }

    private fun printQueuePop(request: MatchRequest, embed: EmbedCreateSpec.Builder)
            = printQueuePop(request.pop, request.popEndTime, request.missingPlayers, embed)

    private fun printQueuePop(pop: MatchService.Pop, endTime: Instant, missingPlayers: Set<Long>, embed: EmbedCreateSpec.Builder) {
        embed.title("Match found")
        embed.description(when {
            pop.request != null ->
                "A ${pop.request.minPlayers} player match has been requested by <@${pop.request.player}>. " +
                        "Please either `Accept` or `Decline` the match. " +
                        "You have ${Config.bot.accept_timeout} seconds to accept or deny, otherwise you will be removed from the queue."

            else ->
                "A match is ready, please press `Accept` to accept the match. " +
                        "You have ${Config.bot.accept_timeout} seconds to accept, otherwise you will be removed from the queue."
        })

        embed.addField("Time remaining", "<t:${endTime.epochSecond}:R>", true)
        if (missingPlayers.isNotEmpty()) {
            embed.addField(
                "Missing players: ",
                missingPlayers.joinToString(" ") { player -> "<@$player>" },
                true
            )
        }
    }

    private fun printPreference(user: Snowflake, embed: EmbedCreateSpec.Builder)
    {
        val preferences = preferencesService.getPreferences(user.asLong())

        embed.title("Your preferences")

        val enabled = when (preferences.dmNotifications) {
            true -> Config.emojis.dm_notifications_enabled
            false -> Config.emojis.dm_notifications_disabled
        }
        embed.addField("DM Notifications", enabled, true)

        val preferredServers = preferences.preferredServers
            .sortedDescending()
            .mapNotNull { when(it) {
                "NA" -> Config.emojis.server_pref_na
                "EU" -> Config.emojis.server_pref_eu
                else -> null
            } }.joinToString("")
        embed.addField("Preferred servers", preferredServers, true)
    }

    fun printQueue(queue: String, embed: EmbedCreateSpec.Builder)
    {
        val numPlayers = matchService.getNumPlayers(queue)

        embed.title("$numPlayers players waiting in queue")
        embed.description(when (numPlayers) {
            0L -> "No players in queue ${Config.emojis.queue_empty}."
            else -> matchService.getPlayers(queue).sortedBy { it.joined }.joinToString("\n") { player ->
                val joined = Instant.ofEpochMilli(player.joined).epochSecond
                val preferences = preferencesService.getPreferences(player.snowflake)
                val preferredServers = preferences.preferredServers
                    .sortedDescending()
                    .mapNotNull {
                        when (it) {
                            "NA" -> Config.emojis.server_pref_na
                            "EU" -> Config.emojis.server_pref_eu
                            else -> null
                        }
                    }.joinToString("")
                "<@${player.snowflake}> $preferredServers (In queue since <t:${joined}:R>)"
            }
        })
    }

    private fun printSetupServer(matchId: UUID, spec: InteractionReplyEditSpec.Builder) {
        val request = matchReuests[matchId] ?: return

        spec.addAllComponents(listOf(
            ActionRow.of(
                SelectMatchSetupServer(
                matchId,
                serverService.getAllServers().map { it.name }.toSet(),
                request.server,
            ).component()),
            ActionRow.of(SelectMatchSetupCreatures(matchId, request.creatures).component()),
            ActionRow.of(ButtonMatchStartServer(matchId).component()),
        ))
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonJoin) {
        event.deferEdit().awaitSafe()
        matchService.join(button.queue, event.interaction.user.id.asLong())
        updateQueueMessage(button.queue)
        return
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonLeave) {
        event.deferEdit().awaitSafe()
        matchService.leave(button.queue, event.interaction.user.id.asLong())
        updateQueueMessage(button.queue)
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonToggleDMNotifications) {
        event.deferEdit().awaitSafe()
        preferencesService.toggleDirectMessageNotifications(event.interaction.user.id.asLong())
        event.editReplyCompat {
            addEmbedCompat {
                printPreference(event.interaction.user.id, this)
            }
        }.await()
        updateQueueMessage(button.queue)
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonPreferences) {
        event.replyCompat {
            addEmbedCompat {
                printPreference(event.interaction.user.id, this)
            }

            addComponent(ActionRow.of(
                ButtonToggleDMNotifications(button.queue).component(),
                ButtonUpdateServerPreference(button.queue).component(),
            ))
            ephemeral(true)
        }.await()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonUpdateServerPreference) {
        event.deferEdit().awaitSafe()
        event.editReplyCompat {
            addEmbedCompat {
                printPreference(event.interaction.user.id, this)
            }

            addComponent(ActionRow.of(
                SelectServerPreference(button.queue).component(),
            ))
        }.await()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: SelectServerPreference) {
        event.deferEdit().awaitSafe()
        val result = (event as SelectMenuInteractionEvent).values
        preferencesService.setPreferences(event.interaction.user.id.asLong(), PreferencesService.UpdatePreferences(
            preferredServers = result.toSet()
        ))
        event.editReplyCompat {
            addEmbedCompat {
                printPreference(event.interaction.user.id, this)
            }

            addComponent(ActionRow.of(
                ButtonToggleDMNotifications(button.queue).component(),
                ButtonUpdateServerPreference(button.queue).component(),
            ))
        }.await()
        updateQueueMessage(button.queue)
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonAccept) {
        event.deferEdit().awaitSafe()

        val request = matchReuests[button.matchId] ?: return
        request.missingPlayers -= event.interaction.user.id.asLong()


        if(request.missingPlayers.isEmpty()) {
            request.matchReady.complete(null)
        } else {
            event.editReplyCompat {
                addEmbedCompat {
                    printQueuePop(request, this)
                }
            }.awaitSafe()
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonDecline) {
        event.deferEdit().awaitSafe()

        val request = matchReuests[button.matchId] ?: return
        request.missingPlayers -= event.interaction.user.id.asLong()
        request.declined = true

        if(request.missingPlayers.isEmpty()) {
            request.matchReady.complete(null)
        } else {
            event.editReplyCompat {
                addEmbedCompat {
                    printQueuePop(request, this)
                }
            }.awaitSafe()
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMapVote) {
        event.deferEdit().awaitSafe()

        val request = matchReuests[button.matchId] ?: return
        if(request.pop.allPlayers.contains(event.interaction.user.id.asLong())) {
            request.mapVotes += Pair(event.interaction.user.id.asLong(), button.map)
            updateMapVoteMessage(button.matchId)
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMatchDrop) {
        event.deferEdit().awaitSafe()

        val request = matchReuests[button.matchId] ?: return
        if(request.pop.allPlayers.contains(event.interaction.user.id.asLong()))
        {
            request.dropPlayers += event.interaction.user.id.asLong()
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMatchFinished) {
        event.deferEdit().awaitSafe()

        val request = matchReuests[button.matchId] ?: return

        if(request.pop.allPlayers.contains(event.interaction.user.id.asLong())) {
            request.finishedPlayers += event.interaction.user.id.asLong()

            if(request.finishedPlayers.size >= 2) {
                if (Config.bot.rating && request.ranked == null) {
                    val server = request.server ?: return

                    val api = serverService.getClient(server) ?: return

                    val result = try {
                        api.getResult(
                            ResultRequest(
                                id = request.openUrl!!.split(":").last().toInt(),
                            )
                        )
                    } catch (ex: Exception) {
                        ResultResponse(null)
                    }

                    result.winner.let {

                        if (it == "GRIFFIN") {
                            ratingService.addResult(request.teams!!.first, request.teams!!.second);
                        }
                        else {
                            ratingService.addResult(request.teams!!.second, request.teams!!.first);
                        }
                    }
                }

                handleMatchFinished(request)
                matchReuests.remove(button.matchId)
            }
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMatchSetupServer) {
        val request = matchReuests[button.matchId]

        if(request == null || request.serverSetupPlayer != null) {
            event.deferEdit().awaitSafe()
            return
        }

        request.serverSetupPlayer = event.interaction.user.id

        event
            .deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
            .awaitSafe()
        
        updateMatchReadyMessage(button.matchId)

        event.editReplyCompat {
            printSetupServer(button.matchId, this)
        }.awaitSafe()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMatchUnranked) {
        val request = matchReuests[button.matchId]

        if(request == null || request.ranked != null) {
            event.deferEdit().awaitSafe()
            return
        }

        if (!button.final) {
            event
                .deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
                .awaitSafe()

            updateMatchReadyMessage(button.matchId)

            event.editReplyCompat {
                addEmbedCompat {
                    description("Are you sure you want to set it to unranked? Ill intent use of this will get you reported? If you believe this should be unranked press the set ranked button under this, if not dismiss this message.")
                }
                addComponent(ActionRow.of(ButtonMatchUnranked(button.matchId, true).component()))
            }.awaitSafe()
        }
        else {
            event.deferEdit().withEphemeral(true).awaitSafe()
            event.editReplyCompat {
                addEmbedCompat {
                    description("You have set this match to be unranked, you can dissmis this message.")
                }
                addAllComponents(emptyList())
            }.awaitSafe()

            request.ranked = event.interaction.user.id

            updateMatchReadyMessage(button.matchId)
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: SelectMatchSetupCreatures) {
        val request = matchReuests[button.matchId] ?: return

        val (creature0, creature1, creature2) = (event as SelectMenuInteractionEvent).values
        request.creatures = Triple(creature0, creature1, creature2)

        event.deferEdit().withEphemeral(true).awaitSafe()
        event.editReplyCompat { printSetupServer(button.matchId, this) }.awaitSafe()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: SelectMatchSetupServer) {
        val request = matchReuests[button.matchId] ?: return

        request.server = (event as SelectMenuInteractionEvent).values.first()

        event.deferEdit().withEphemeral(true).awaitSafe()
        event.editReplyCompat { printSetupServer(button.matchId, this) }.awaitSafe()
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent, button: ButtonMatchStartServer) {
        val request = matchReuests[button.matchId] ?: return
        val server = request.server

        event.deferEdit().withEphemeral(true).awaitSafe()

        if(server == null) {
            event.editReplyCompat {
                contentOrNull("No server selected")
            }.awaitSafe()
            return
        }

        val api = serverService.getClient(server) ?: return

        val start = try {
            api.start(
                StartRequest(
                map = "lv_${request.getMapVoteResult()}",
                maxPlayers = request.pop.allPlayers.size,
                creature0 = request.creatures.first,
                creature1 = request.creatures.second,
                creature2 = request.creatures.third,
            )
            )
        } catch (ex: Exception) {
            StartResponse(ex.message, null)
        }

        if(start.error != null)
        {
            event.editReplyCompat {
                contentOrNull("Unable to start the server: ${start.error}")
                printSetupServer(button.matchId, this)
            }.awaitSafe()
            return
        }

        request.openUrl = start.openUrl

        request.timeToJoin = Instant.now().plusSeconds(Config.bot.time_to_join.toLong());

        event.editReplyCompat {
            addEmbedCompat {
                title("Server setup")
                description("Done, you can close this message")
            }
            addAllComponents(emptyList())
        }.awaitSafe()

        updateMatchReadyMessage(button.matchId)
        
        CoroutineScope(Dispatchers.Default).launch {
            delay(Config.bot.time_to_join.seconds)
            updateMatchReadyMessage(button.matchId)
        }
    }

    private suspend fun handleInteraction(event: ComponentInteractionEvent)
    {
        ButtonJoin.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonLeave.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonToggleDMNotifications.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonPreferences.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonUpdateServerPreference.parse(event.customId)?.also { handleInteraction(event, it); return }
        SelectServerPreference.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonAccept.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonDecline.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMapVote.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMatchDrop.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMatchFinished.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMatchUnranked.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMatchSetupServer.parse(event.customId)?.also { handleInteraction(event, it); return }
        SelectMatchSetupServer.parse(event.customId)?.also { handleInteraction(event, it); return }
        SelectMatchSetupCreatures.parse(event.customId)?.also { handleInteraction(event, it); return }
        ButtonMatchStartServer.parse(event.customId)?.also { handleInteraction(event, it); return }
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
