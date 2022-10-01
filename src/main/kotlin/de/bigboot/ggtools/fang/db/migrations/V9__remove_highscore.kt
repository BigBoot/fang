@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V9__remove_highscore : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |drop table if exists Highscores;
        """.trimMargin()).execute()
    }
}
