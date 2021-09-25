package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Notification(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Notification>(Notifications)

    var snowflake by Notifications.snowflake
    var directMessage by Notifications.directMessage
}

object Notifications : UUIDTable() {
    val snowflake = long("snowflake")
    val directMessage = bool("direct_message").default(true)
}
