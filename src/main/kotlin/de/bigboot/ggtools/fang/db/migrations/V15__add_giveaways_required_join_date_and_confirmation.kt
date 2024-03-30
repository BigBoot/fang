package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V15__add_giveaways_required_join_date_and_confirmation : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |alter table Giveaways
        |    add max_join_date timestamp default null;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table GiveawayEntries
        |    add winner tinyint default 0 not null;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table GiveawayEntries
        |    add confirmed tinyint default 0 not null;
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |alter table GiveawayEntries
        |    add confirm_until timestamp default null;
        """.trimMargin()).execute()
    }
}