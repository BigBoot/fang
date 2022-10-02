@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V10__add_user_preferences : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |alter table Notifications
        |    rename to Preferences;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table Preferences
        |    add preferred_servers text default 'NA,EU';
        """.trimMargin()).execute()
    }
}
