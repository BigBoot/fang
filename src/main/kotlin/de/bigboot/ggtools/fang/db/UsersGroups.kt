package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.sql.Table

object UsersGroups : Table() {
    val user = reference("user", Users)
    val group = reference("group", Groups)

    override val primaryKey = PrimaryKey(user, group)
}