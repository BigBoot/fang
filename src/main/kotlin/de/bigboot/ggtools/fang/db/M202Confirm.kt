package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class M202Confirm(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<M202Confirm>(M202Confirms)

    var token by M202Confirms.token
}

object M202Confirms : UUIDTable() {
    val token = text("token")
}
