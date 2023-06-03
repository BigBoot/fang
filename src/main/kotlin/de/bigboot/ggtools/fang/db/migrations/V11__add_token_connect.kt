@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V11__add_token_connect : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |alter table Preferences
        |    add token_connect tinyint default 1 not null;
        """.trimMargin()).execute()
    }
}
