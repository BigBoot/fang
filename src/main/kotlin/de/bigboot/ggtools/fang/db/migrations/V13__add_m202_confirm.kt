@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V13__add_m202_confirm : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists M202Confirms
        |(
        |    id        binary(16)           not null
        |        primary key,
        |    token     text                 not null
        |);
        """.trimMargin()).execute()
    }
}
