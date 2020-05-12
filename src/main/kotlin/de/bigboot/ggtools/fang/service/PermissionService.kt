package de.bigboot.ggtools.fang.service

import de.bigboot.ggtools.fang.db.User

interface PermissionService {
    fun addGroup(name: String): Boolean

    fun removeGroup(name: String): Boolean

    fun getGroups(): List<String>

    fun addPermissionToGroup(groupName: String, permission: String): Boolean

    fun removePermissionFromGroup(groupName: String, permission: String): Boolean

    fun getPermissions(groupName: String): List<String>?

    fun addUserToGroup(snowflake: Long, groupName: String): Boolean

    fun removeUserFromGroup(snowflake: Long, groupName: String): Boolean

    fun getUsersByGroup(groupName: String): List<User>?

    fun getGroupsByUser(snowflake: Long): List<Pair<String, List<String>>>
}