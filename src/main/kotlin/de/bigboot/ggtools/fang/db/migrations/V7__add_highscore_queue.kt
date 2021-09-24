@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V7__add_highscore_queue : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |delete from Highscores;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table Highscores
        |    add queue text not null;
        """.trimMargin()).execute()
    }
}
