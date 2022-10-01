@file:Suppress("ClassName", "ClassNaming", "unused", "LongMethod")

package de.bigboot.ggtools.fang.db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

class V1__initial_database_schema : BaseJavaMigration() {
    override fun migrate(context: Context) {
        context.connection.prepareStatement("""
        |create table if not exists `Groups`
        |(
        |    id   binary(16) not null
        |        primary key,
        |    name text       not null
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists GroupPermissions
        |(
        |    id         binary(16) not null
        |        primary key,
        |    permission text       not null,
        |    `group`    binary(16) not null,
        |    constraint fk_GroupPermissions_group_id
        |        foreign key (`group`) references `Groups` (id)
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists Players
        |(
        |    id        binary(16)           not null
        |        primary key,
        |    snowflake bigint               not null,
        |    joined    bigint               not null,
        |    in_match  tinyint    default 0 not null
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists Servers
        |(
        |    id      binary(16) not null
        |        primary key,
        |    name    text       not null,
        |    url     text       not null,
        |    api_key text       not null
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists Users
        |(
        |    id        binary(16) not null
        |        primary key,
        |    snowflake bigint     not null
        |);
        """.trimMargin()).execute()

        context.connection.prepareStatement("""
        |create table if not exists UsersGroups
        |(
        |    `user`    binary(16) not null,
        |    `group`   binary(16) not null,
        |    primary key (`user`, `group`),
        |    constraint fk_UsersGroups_group_id
        |        foreign key (`group`) references `Groups` (id),
        |    constraint fk_UsersGroups_user_id
        |        foreign key (`user`) references Users (id)
        |);
        """.trimMargin()).execute()
    }
}
