package de.bigboot.ggtools.fang.utils

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.Event
import discord4j.rest.util.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.tinylog.kotlin.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional
import java.util.OptionalInt
import java.util.OptionalLong
import kotlin.time.Duration

/** Flux and Mono **/

suspend fun <T> Mono<T>.await(): T? = awaitFirstOrNull()
suspend fun <T> Mono<T>.awaitSingle(): T = this.awaitSingle()
suspend fun <T> Flux<T>.await(): List<T> = collectList().awaitSingle()

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

suspend fun Message.doAfter(duration: Duration, block: suspend Message.()->Unit) {
    val channelId = this.channelId
    val messageId = this.id
    CoroutineScope(Dispatchers.Default).launch {
        delay(duration)
        client.getMessageById(channelId, messageId).awaitFirstOrNull()?.block()
    }
}

suspend fun Message.deleteAfter(duration: Duration) = doAfter(duration) {
    delete().awaitFirstOrNull()
}

suspend fun Message.reactAfter(duration: Duration, emoji: ReactionEmoji) = doAfter(duration) {
    addReaction(emoji).awaitFirstOrNull()
}

suspend fun Message.isFromSelf() = client.selfId.awaitSingle() == Snowflake.of(userData.id())

suspend fun Message.hasReacted(emoji: ReactionEmoji) = hasReacted(client.selfId.awaitSingle(), emoji)
suspend fun Message.hasReacted(user: Snowflake, emoji: ReactionEmoji): Boolean
        = getReactors(emoji).any { it.id == user }.awaitSingle()

/** User **/

suspend fun User.isSelf() = client.selfId.awaitSingle() == id