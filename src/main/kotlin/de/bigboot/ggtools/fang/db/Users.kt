package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var snowflake by Users.snowflake
    var groups by Group via UsersGroups

    val skill by UserRating referrersOn UsersRating.user
}

object Users : UUIDTable() {
    val snowflake = long("snowflake")
}
