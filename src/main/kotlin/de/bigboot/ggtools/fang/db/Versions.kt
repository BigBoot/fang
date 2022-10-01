package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

class Version(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Version>(Versions)

    var revision by Versions.revision
}

object Versions : UUIDTable() {
    val revision = integer("revision")
}
