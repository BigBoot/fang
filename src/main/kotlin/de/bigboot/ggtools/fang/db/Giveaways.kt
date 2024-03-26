package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

class Giveaway(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Giveaway>(Giveaways)

    var message by Giveaways.message
    var channel by Giveaways.channel
    var title by Giveaways.title
    var description by Giveaways.description
    var end by Giveaways.end
    var ended by Giveaways.ended

    val prizes by Prize referrersOn Prizes.giveaway
}

object Giveaways : UUIDTable() {
    val message = long("message")
    val channel = long("channel")
    val title = text("title")
    val description = text("description")
    val end = timestamp("end")
    val ended = bool("ended").default(false)
}
