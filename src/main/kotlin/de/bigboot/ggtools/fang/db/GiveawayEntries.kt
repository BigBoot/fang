package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.*

class GiveawayEntry(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GiveawayEntry>(GiveawayEntries)

    var user by GiveawayEntries.user
    var winner by GiveawayEntries.winner
    var confirmed by GiveawayEntries.confirmed
    var confirm_until by GiveawayEntries.confirm_until
    var giveaway by Giveaway referencedOn GiveawayEntries.giveaway
    var prize by Prize referencedOn GiveawayEntries.prize
}

object GiveawayEntries : UUIDTable() {
    val user = long("user")
    val winner = bool("winner").default(false)
    val confirmed = bool("confirmed").default(false)
    val confirm_until = timestamp("confirm_until").nullable()
    val giveaway = reference("giveaway", Giveaways)
    val prize = reference("prize", Prizes)
}
