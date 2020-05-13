package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Server(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Server>(Servers)

    var name by Servers.name
    var url by Servers.url
    var apiKey by Servers.apiKey
}

object Servers : UUIDTable() {
    val name = text("name")
    val url = text("url")
    var apiKey = text("api_key")
}
