package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class GroupPermission(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<GroupPermission>(GroupPermissions)

    var permission by GroupPermissions.permission
    var group by Group referencedOn GroupPermissions.group
}

object GroupPermissions : UUIDTable() {
    val permission = text("permission")
    val group = reference("group", Groups)
}
