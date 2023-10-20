package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.M202Confirm
import de.bigboot.ggtools.fang.db.M202Confirms
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.entity.channel.PrivateChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.inputstream.ZipInputStream
import net.lingala.zip4j.model.LocalFileHeader
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString.Companion.encode
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.InputStream
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory


class M202VerifyServiceImpl : AutostartService, KoinComponent {
    private val client by inject<GatewayDiscordClient>()
    private val http = OkHttpClient()
    private val database by inject<Database>()
    private val hasher = MessageDigest.getInstance("SHA-256")

    init {
        client.eventDispatcher.on<MessageCreateEvent>()
            .onEachSafe(this::handleCommandEvent)
            .launch()
    }

    private fun extractFileFromEncryptedZip(input: InputStream, password: String?, targetFileName: String): InputStream? {
        val zip = ZipInputStream(input, password?.toCharArray())

        var localFileHeader: LocalFileHeader? = zip.nextEntry
        while (localFileHeader != null) {
            if(localFileHeader.fileName == targetFileName) {
                return zip
            }
            localFileHeader = zip.nextEntry
        }

        return null
    }

    private fun getUserTokens(input: InputStream, game: String): List<String> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(input)

        val tokens = doc.getElementsByTagName("user")
        return sequence {
            for (i in 0 until tokens.length) {
                val element = tokens.item(i)
                if(element.attributes.getNamedItem("gameabbr").nodeValue == game) {
                    yield(element.attributes.getNamedItem("token").nodeValue)
                }
            }
        }.toList()
    }

    private fun verifyToken(token: String) = transaction(database) {
        val hash = hasher.digest(token.encode(Charsets.US_ASCII).toByteArray()).fold("") { str, it ->
            str + "%02x".format(it)
        }

        if (M202Confirm.find { M202Confirms.token eq hash }.any()) {
            return@transaction false
        }

        M202Confirm.new { this.token = hash }

        return@transaction true
    }

    private suspend fun handleCommandEvent(event: MessageCreateEvent) {
        val msg = event.message
        val channel = event.message.channel.awaitSingle();

        if (channel !is PrivateChannel) {
            return
        }

        val arcZone = msg.attachments.firstOrNull { it.filename == "ArcZone.dat" } ?: return

        if (arcZone.size > 2 * 1024 * 1024) {
            channel.createMessageCompat {
                content("It looks like you tried to verify your access to M202, but the file you sent exceeds the size limit, if you believe this is an error please contact one of the moderators.")
            }.awaitSafe()
            return
        }

        val response = http.newCall(Request.Builder().url(arcZone.url).build()).await()
        val body = response.body() ?: return

        val tokens = withContext(Dispatchers.IO) {
            extractFileFromEncryptedZip(body.byteStream(), Config.m202.arc_pw, "ArcZone.xml")?.let {
                getUserTokens(it, "m202")
            }
        } ?: return

        val valid = tokens.any { verifyToken(it) }

        if(!valid) {
            channel.createMessageCompat {
                content("This token has already been used to verify access and cannot be used again.")
            }.awaitSafe()
            return
        }

        for (guild in client.guilds.awaitSafe())
        {
            val member = guild.getMemberById(msg.author.get().id).awaitSafe() ?: continue
            val role = guild.roles.filter { it.name == Config.m202.role }.awaitFirstOrNull() ?: continue
            member.addRole(role.id, "Assigned by fang using token verification").awaitSafe()

            channel.createMessageCompat {
                content("Role ${role.name} has been assigned in ${guild.name}.")
            }.awaitSafe()
        }
    }
}