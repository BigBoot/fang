package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class GiveawayEntry(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GiveawayEntry>(GiveawayEntries)

    var user by GiveawayEntries.user
    var giveaway by Giveaway referencedOn GiveawayEntries.giveaway
    var prize by Prize referencedOn GiveawayEntries.prize
}

object GiveawayEntries : UUIDTable() {
    val user = long("user")
    val giveaway = reference("giveaway", Giveaways)
    val prize = reference("prize", Prizes)
}
