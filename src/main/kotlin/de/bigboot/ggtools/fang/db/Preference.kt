package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Preference(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Preference>(Preferences)

    var snowflake by Preferences.snowflake
    var directMessage by Preferences.directMessage
    var preferredServers by Preferences.preferredServers
    var tokenConnect by Preferences.tokenConnect
}

object Preferences : UUIDTable() {
    val snowflake = long("snowflake")
    val directMessage = bool("direct_message").default(true)
    val preferredServers = text("preferred_servers").default("NA,EU")
    val tokenConnect = bool("token_connect").default(true)
}
