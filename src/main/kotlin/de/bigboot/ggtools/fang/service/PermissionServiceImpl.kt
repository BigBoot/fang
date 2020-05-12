package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.util.*

class PermissionServiceImpl : PermissionService, KoinComponent {
    private val database: Database by inject()

    init {
        transaction(database) {
            SchemaUtils.create(Groups, GroupPermissions, Users, UsersGroups)

            if (Group.find { Groups.name eq Config.DEFAULT_GROUP_NAME }.empty()) {
                val defaultGroup = Group.new(UUID.randomUUID()) {
                    name = Config.DEFAULT_GROUP_NAME
                }
                Config.DEFAULT_GROUP_PERMISSIONS.forEach {
                    GroupPermission.new {
                        permission = it
                        group = defaultGroup
                    }
                }
            }

            if (Group.find { Groups.name eq Config.ADMIN_GROUP_NAME }.empty()) {
                val defaultGroup = Group.new(UUID.randomUUID()) {
                    name = Config.ADMIN_GROUP_NAME
                }
                Config.ADMIN_GROUP_PERMISSIONS.forEach {
                    GroupPermission.new {
                        permission = it
                        group = defaultGroup
                    }
                }
            }
        }
    }

    override fun addGroup(name: String) = transaction {
        if (!Group.find { Groups.name eq name }.empty()) {
            return@transaction false
        }

        Group.new {
            this.name = name
        }

        return@transaction true
    }

    override fun removeGroup(name: String) = transaction {
        Groups.deleteWhere { Groups.name eq name } != 0
    }

    override fun getGroups() = transaction {
        Group.all().map { it.name }.toList()
    }

    override fun addPermissionToGroup(groupName: String, permission: String) = transaction {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        if (!GroupPermission.find {
                (GroupPermissions.permission eq permission) and (GroupPermissions.group eq group.id)
            }.empty()) {
            return@transaction true
        }

        GroupPermission.new {
            this.permission = permission
            this.group = group
        }

        return@transaction true
    }

    override fun removePermissionFromGroup(groupName: String, permission: String) = transaction {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        GroupPermissions.deleteWhere {
            (GroupPermissions.permission eq permission) and (GroupPermissions.group eq group.id)
        }

        return@transaction true
    }

    override fun getPermissions(groupName: String) = transaction {
        Group.find { Groups.name eq groupName }.firstOrNull()?.permissions?.map { it.permission }?.toList()
    }

    override fun addUserToGroup(snowflake: Long, groupName: String) = transaction {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        val user = User.find { Users.snowflake eq snowflake }.firstOrNull() ?: User.new {
            this.snowflake = snowflake
        }

        user.groups = SizedCollection(user.groups.toList() + group)
        return@transaction true
    }

    override fun removeUserFromGroup(snowflake: Long, groupName: String) = transaction {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        val user = User.find { Users.snowflake eq snowflake }.firstOrNull() ?: User.new {
            this.snowflake = snowflake
        }

        user.groups = SizedCollection(user.groups.toList() - group)
        return@transaction true
    }

    override fun getUsersByGroup(groupName: String) = transaction {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction null
        return@transaction group.users.toList()
    }

    override fun getGroupsByUser(snowflake: Long): List<Pair<String, List<String>>> = transaction {
        (User.find { Users.snowflake eq snowflake }
            .firstOrNull()
            ?.groups
            ?.map { group -> Pair(group.name, group.permissions.map { it.permission }) }
            ?.toList()
            ?: emptyList())
            .plusElement(Pair("default", Group
                .find { Groups.name eq Config.DEFAULT_GROUP_NAME }
                .first()
                .permissions
                .map { it.permission }
                .toList()
            ))
    }
}