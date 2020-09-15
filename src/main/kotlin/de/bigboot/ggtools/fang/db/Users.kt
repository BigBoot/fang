package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var snowflake by Users.snowflake
    var skill by Users.skill
    var groups by Group via UsersGroups
}

object Users : UUIDTable() {
    val snowflake = long("snowflake")
    val skill = integer("skill").default(1)
}
