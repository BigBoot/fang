@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V8__add_notifications : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists Notifications
        |(
        |    id        binary(16)           not null
        |        primary key,
        |    snowflake       bigint               not null,
        |    direct_message  tinyint(1) default 0 not null
        |);
        """.trimMargin()).execute()
    }
}
