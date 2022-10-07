@file:Suppress("unused")

package de.bigboot.ggtools.fang.utils

import discord4j.common.util.Snowflake
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.*
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.tinylog.kotlin.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.*
import kotlin.time.Duration

/** Flux and Mono **/

suspend fun <T> Mono<T>.await(): T? = awaitFirstOrNull()
suspend fun <T> Mono<T>.awaitSafe(log: Boolean = true): T? = try { await() } catch (e: ClientException) { if(log) { Logger.warn(e) }; null }
suspend fun <T> Mono<T>.awaitSingle(): T = this.awaitSingle()
suspend fun <T> Flux<T>.await(): List<T> = collectList().awaitSingle()
suspend fun <T> Flux<T>.awaitSafe(log: Boolean = true): List<T> = try { await() } catch (e: ClientException) { if(log) { Logger.warn(e) }; emptyList() }

fun <T> Optional<T>.orNull(): T? = orElse(null)
fun OptionalInt.orNull(): Int? = takeIf { isPresent }?.asInt
fun OptionalLong.orNull(): Long? = takeIf { isPresent }?.asLong

fun <T : Any> Flux<T>.flow(): Flow<T> = asFlow()
inline fun <reified T : Event> EventDispatcher.on(): Flow<T> = on(T::class.java).asFlow()

fun <T> Flow<T>.launch() = launchIn(CoroutineScope(Dispatchers.Default))
fun <T> Flow<T>.onEachSafe(action: suspend (T) -> Unit) = transform { value ->
    try {
        action(value)
    } catch (fault: Throwable) {
        Logger.warn(fault)
    }
    return@transform emit(value)
}

/** Message **/

suspend fun Message.doAfter(duration: Duration, block: suspend Message.() -> Unit) {
    val channelId = this.channelId
    val messageId = this.id
    CoroutineScope(Dispatchers.Default).launch {
        delay(duration)
        client.getMessageById(channelId, messageId).awaitFirstOrNull()?.block()
    }
}

suspend fun Message.deleteAfter(duration: Duration, andThen: (suspend () -> Unit)? = null) = doAfter(duration) {
    delete().awaitFirstOrNull()
    andThen?.invoke()
}

suspend fun Message.reactAfter(duration: Duration, emoji: ReactionEmoji, andThen: (suspend () -> Unit)? = null) = doAfter(duration) {
    addReaction(emoji).awaitFirstOrNull()
    andThen?.invoke()
}

fun Message.isFromSelf() = client.selfId == Snowflake.of(userData.id())

suspend fun Message.hasReacted(emoji: ReactionEmoji) = hasReacted(client.selfId, emoji)
suspend fun Message.hasReacted(user: Snowflake, emoji: ReactionEmoji): Boolean =
        getReactors(emoji).any { it.id == user }.awaitSingle()

/** User **/

fun User.isSelf() = client.selfId == id

private val USER_REGEX = Regex("""<@!?(\d+)>""")

suspend fun Guild.findUser(name: String): User? {
    return when {
        USER_REGEX.matches(name) -> client.getUserById(Snowflake.of(USER_REGEX.find(name)!!.groupValues[1])).awaitSafe()
        else -> findMember(name)
    }
}

suspend fun Guild.findMember(name: String): Member? {
    return when {
        USER_REGEX.matches(name) -> getMemberById(Snowflake.of(USER_REGEX.find(name)!!.groupValues[1])).awaitSafe()
        else -> members
            .filter {
                it.username.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) || it.displayName.lowercase(
                    Locale.getDefault()
                ) == name.lowercase(Locale.getDefault())
            }.awaitFirstOrNull()
    }
}

suspend fun Guild.findChannel(name: String): MessageChannel? {
    return channels
        .filter { it is MessageChannel }
        .filter { it.name == name }
        .awaitFirstOrNull() as? MessageChannel
}


/** Channel **/
suspend fun MessageChannel.clean() {
    messages().publishOn(Schedulers.boundedElastic()).doOnNext { it.delete().onErrorContinue { throwable, o ->
        Logger.error(throwable) { "Error while processing $o. Cause: ${throwable.message}" }
    }.subscribe() }.awaitSafe()
}

@Suppress("ReactiveStreamsUnusedPublisher")
suspend fun MessageChannel.messages(): Flux<Message> =
    getMessagesBefore(Snowflake.of(Instant.now().plus(java.time.Duration.ofDays(1000))))

/** Compat **/

@Suppress("HasPlatformType")
fun MessageChannel.createEmbedCompat(spec: EmbedCreateSpec.Builder.() -> Unit) =
    createMessage(EmbedCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun MessageChannel.createMessageCompat(spec: MessageCreateSpec.Builder.() -> Unit) =
    createMessage(MessageCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun Message.editCompat(spec: MessageEditSpec.Builder.() -> Unit) =
    edit(MessageEditSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun MessageCreateSpec.Builder.addEmbedCompat(spec: EmbedCreateSpec.Builder.() -> Unit) =
    addEmbed(EmbedCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun MessageEditSpec.Builder.addEmbedCompat(spec: EmbedCreateSpec.Builder.() -> Unit) =
    addEmbed(EmbedCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun DeferrableInteractionEvent.replyCompat(spec: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit) =
    reply(InteractionApplicationCommandCallbackSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun ComponentInteractionEvent.editCompat(spec: InteractionApplicationCommandCallbackSpec.Builder.() -> Unit) =
    edit(InteractionApplicationCommandCallbackSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun InteractionApplicationCommandCallbackSpec.Builder.addEmbedCompat(spec: EmbedCreateSpec.Builder.() -> Unit) =
    addEmbed(EmbedCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun InteractionReplyEditSpec.Builder.addEmbedCompat(spec: EmbedCreateSpec.Builder.() -> Unit) =
    addEmbed(EmbedCreateSpec.builder().apply(spec).build())

@Suppress("HasPlatformType")
fun DeferrableInteractionEvent.editReplyCompat(spec: InteractionReplyEditSpec.Builder.() -> Unit) =
    editReply(InteractionReplyEditSpec.builder().apply(spec).build())