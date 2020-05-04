package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Player(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Player>(Players)

    var snowflake by Players.snowflake
    var joined by Players.joined
    var inMatch by Players.inMatch
}

object Players : UUIDTable() {
    val snowflake = long("snowflake")
    val joined = long("joined")
    val inMatch = bool("in_match").default(false)
}