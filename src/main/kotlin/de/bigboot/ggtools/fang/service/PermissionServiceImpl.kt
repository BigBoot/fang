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
            if (Group.find { Groups.name eq Config.permissions.default_group_name }.empty()) {
                val defaultGroup = Group.new(UUID.randomUUID()) {
                    name = Config.permissions.default_group_name
                }
                Config.permissions.default_group_permissions.forEach {
                    GroupPermission.new {
                        permission = it
                        group = defaultGroup
                    }
                }
            }

            if (Group.find { Groups.name eq Config.permissions.admin_group_name }.empty()) {
                val defaultGroup = Group.new(UUID.randomUUID()) {
                    name = Config.permissions.admin_group_name
                }
                Config.permissions.admin_group_permissions.forEach {
                    GroupPermission.new {
                        permission = it
                        group = defaultGroup
                    }
                }
            }
        }
    }

    override fun addGroup(name: String) = transaction(database) {
        if (!Group.find { Groups.name eq name }.empty()) {
            return@transaction false
        }

        Group.new {
            this.name = name
        }

        return@transaction true
    }

    override fun removeGroup(name: String) = transaction(database) {
        Groups.deleteWhere { Groups.name eq name } != 0
    }

    override fun getGroups() = transaction(database) {
        Group.all().map { it.name }.toList()
    }

    override fun addPermissionToGroup(groupName: String, permission: String) = transaction(database) {
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

    override fun removePermissionFromGroup(groupName: String, permission: String) = transaction(database) {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        GroupPermissions.deleteWhere {
            (GroupPermissions.permission eq permission) and (GroupPermissions.group eq group.id)
        }

        return@transaction true
    }

    override fun getPermissions(groupName: String) = transaction(database) {
        Group.find { Groups.name eq groupName }.firstOrNull()?.permissions?.map { it.permission }?.toList()
    }

    override fun addUserToGroup(snowflake: Long, groupName: String) = transaction(database) {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        val user = User.find { Users.snowflake eq snowflake }.firstOrNull() ?: User.new {
            this.snowflake = snowflake
        }

        user.groups = SizedCollection(user.groups.toList() + group)
        return@transaction true
    }

    override fun removeUserFromGroup(snowflake: Long, groupName: String) = transaction(database) {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction false

        val user = User.find { Users.snowflake eq snowflake }.firstOrNull() ?: User.new {
            this.snowflake = snowflake
        }

        user.groups = SizedCollection(user.groups.toList() - group)
        return@transaction true
    }

    override fun getUsersByGroup(groupName: String) = transaction(database) {
        val group = Group.find { Groups.name eq groupName }.firstOrNull() ?: return@transaction null
        return@transaction group.users.toList()
    }

    override fun getGroupsByUser(snowflake: Long): List<Pair<String, List<String>>> = transaction(database) {
        (User.find { Users.snowflake eq snowflake }
            .firstOrNull()
            ?.groups
            ?.map { group -> Pair(group.name, group.permissions.map { it.permission }) }
            ?.toList()
            ?: emptyList())
            .plusElement(Pair("default", Group
                .find { Groups.name eq Config.permissions.default_group_name }
                .first()
                .permissions
                .map { it.permission }
                .toList()
            ))
    }
}
