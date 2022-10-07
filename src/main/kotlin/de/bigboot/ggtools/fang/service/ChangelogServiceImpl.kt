package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.Version
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.min

private val CHANGELOG = listOf(
    listOf(
        "Fixed queue timers",
    ),
    listOf(
        "Fixed queue timers when match takes longer than 90 minutes",
    ),
    listOf(
        "Increase msg update times to fix queue emoji problems",
    ),
    listOf(
        "Fix queue channel cleanup",
    ),
    listOf(
        "Added Frankenbuild queue",
    ),
    listOf(
        "Added support for multiple queues",
        "Update dependencies",
    ),
    listOf(
        "Fixed a bug causing player to be unable to join a queue after using the dropout feature"
    ),
    listOf(
        "Fix connect string when starting a server using fang",
        "Fix queue channel cleanup",
        "Send a DM to all players when a match is ready",
    ),
    listOf(
        "Added queue highscores",
    ),
    listOf(
        "Added toggle to enable/disable dm notifications",
    ),
    listOf(
        "Fixed output of 'sudo admin group users list' command",
        "Removed 'server players' command",
        "Comp queue will no longer count towards highscores",
        "Highscore message will no longer ping the person",
        "When getting afk kicked from queue, the player will lose the highscore from this session",
        "Added 'admin highscores' commands",
    ),
    listOf(
        "Updated dependencies",
        "Added HikariCP database connection pooling",
    ),
    listOf(
        "Updated dependencies",
        "Removed Highscores",
        "Fixed username parsing in commands",
        "Try to fix queue channel cleanup again...",
    ),
    listOf(
        "Allow managing users who aren't in the server anymore",
    ),
    listOf(
        "Fix possible infinite loop in backfill logic",
    ),
    listOf(
        "Completely redo queue system",
        "Added mapvotes to queue",
        "Added server setup to queue",
        "Added Mistforge build intergration",
    )
)

class ChangelogServiceImpl : KoinComponent, ChangelogService {
    private val database: Database by inject()

    override val changelog: Changelog = transaction(database) {
        val version = Version.all().firstOrNull() ?: Version.new {
            this.revision = 0
        }

        val latestRevision = CHANGELOG.size
        val lastRevision = min(version.revision, latestRevision)

        version.revision = latestRevision

        CHANGELOG.subList(lastRevision, latestRevision).flatten()
    }
}
