@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V4__add_queue_type : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |delete from Players
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table Players
        |    add queue VARCHAR NOT NULL DEFAULT ''
        """.trimMargin()).execute()
    }
}
