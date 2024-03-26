package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V14__add_giveaways : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists Giveaways
        |(
        |    `id`          binary(16)           not null primary key,
        |    `message`     bigint               not null,
        |    `channel`     bigint               not null,
        |    `title`       text                 not null,
        |    `description` text                 not null,
        |    `end`         timestamp            not null,
        |    `ended`       tinyint    default 0 not null
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists Prizes
        |(
        |    `id`       binary(16) not null primary key,
        |    `text`     text       not null,
        |    `emoji`    text       not null,
        |    `count`    int        not null,
        |    `giveaway` binary(16) not null,
        |    
        |    constraint fk_Prizes_giveaway_id
        |        foreign key (`giveaway`) references `Giveaways` (id)
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists GiveawayEntries
        |(
        |    `id`       binary(16) not null primary key,
        |    `user`     bigint     not null,
        |    `giveaway` binary(16) not null,
        |    `prize`    binary(16) not null,
        |    
        |    constraint fk_GiveawayEntries_giveaway_id
        |        foreign key (`giveaway`) references `Giveaways` (id),
        |    constraint fk_GiveawayEntries_prize_id
        |        foreign key (`prize`) references `Prizes` (id)
        |);
        """.trimMargin()).execute()
    }
}