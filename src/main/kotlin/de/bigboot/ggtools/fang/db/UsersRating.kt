package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class UserRating(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserRating>(UsersRating)

    var rating by UsersRating.rating
    var ratingDeviation by UsersRating.ratingDeviation
    var volatility by UsersRating.volatility

    val userRating by User referrersOn Users.rating
}

object UsersRating : UUIDTable() {
    val rating = double("rating")
    val ratingDeviation = double("ratingDeviation")
    val volatility = double("volatility")
}
