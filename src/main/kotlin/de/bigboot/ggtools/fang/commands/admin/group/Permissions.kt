package de.bigboot.ggtools.fang.commands.admin.group

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.PermissionService
import de.bigboot.ggtools.fang.utils.createEmbedCompat
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Permissions : CommandGroupSpec("permissions", "Manage group permissions") {
    val permissionService by inject<PermissionService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("add", "Grant a permission to a group") {
            arg("group", "The group to modify")
            arg("permission", "The permission to grant to the group")

            onCall {
                val group = args["group"]
                val permission = args["permission"]

                if (group == Config.permissions.admin_group_name) {
                    channel().createEmbedCompat {
                        description("The admin groups cannot be modified")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbedCompat {
                    description(
                        when {
                            permissionService.addPermissionToGroup(
                                group,
                                permission
                            ) -> "Granted permission $permission to group $group"
                            else -> "Group $group not found"
                        }
                    )
                }.awaitSingle()
            }
        }

        command("remove", "Remove a permission from a group") {
            arg("group", "The group to modify")
            arg("permission", "The permission to grant to the group")

            onCall {
                val group = args["group"]
                val permission = args["permission"]

                if (group == Config.permissions.admin_group_name) {
                    channel().createEmbedCompat {
                        description("The admin groups cannot be modified")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbedCompat {
                    description(
                        when {
                            permissionService.removePermissionFromGroup(
                                group,
                                permission
                            ) -> "Removed permission $permission from group $group"
                            else -> "Group $group not found"
                        }
                    )
                }.awaitSingle()
            }
        }
    }
}
