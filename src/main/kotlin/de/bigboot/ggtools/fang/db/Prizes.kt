package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Prize(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Prize>(Prizes)

    var giveaway by Giveaway referencedOn Prizes.giveaway
    var text by Prizes.text
    var emoji by Prizes.emoji
    var count by Prizes.count
}

object Prizes : UUIDTable() {
    val giveaway = reference("giveaway", Giveaways)
    val text = text("text")
    val emoji = text("emoji")
    val count = integer("count")
}
