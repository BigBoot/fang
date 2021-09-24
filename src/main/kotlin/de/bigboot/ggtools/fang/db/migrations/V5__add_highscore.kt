@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V5__add_highscore : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists Highscores
        |(
        |    id        binary(16)           not null
        |        primary key,
        |    snowflake bigint               not null,
        |    score     bigint               not null
        |);
        """.trimMargin()).execute()
    }
}
