@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V6__add_highscore_offset : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |delete from Highscores;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table Highscores
        |    add score_offset bigint default 0;
        """.trimMargin()).execute()
    }
}
