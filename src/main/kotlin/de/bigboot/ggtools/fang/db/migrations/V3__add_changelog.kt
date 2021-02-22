@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V3__add_changelog : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists `Versions`
        |(
        |    id       binary(16) not null
        |        primary key,
        |    revision int        not null
        |);
        """.trimMargin()).execute()
    }
}
