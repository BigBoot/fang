package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Group(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Group>(Groups)

    var name by Groups.name
    var users by User via UsersGroups
    val permissions by GroupPermission referrersOn GroupPermissions.group
}

object Groups : UUIDTable() {
    val name = text("name")
}
