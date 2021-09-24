package de.bigboot.ggtools.fang.db

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

class Highscore(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Highscore>(Highscores)

    var snowflake by Highscores.snowflake
    var score by Highscores.score
    var offset by Highscores.offset
    var queue by Highscores.queue
}

object Highscores : UUIDTable() {
    val snowflake = long("snowflake")
    val score = long("score")
    val offset = long("score_offset").default(0)
    val queue = text("queue")
}
