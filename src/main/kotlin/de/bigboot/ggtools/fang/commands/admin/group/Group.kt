package de.bigboot.ggtools.fang.commands.admin.group

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.Config
import de.bigboot.ggtools.fang.service.PermissionService
import de.bigboot.ggtools.fang.utils.createEmbedCompat
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Group : de.bigboot.ggtools.fang.CommandGroupSpec("group", "Commands for managing groups") {
    val permissionService by inject<PermissionService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        group(Permissions())
        group(Users())

        command("list", "List all groups") {
            onCall {
                channel().createEmbedCompat {
                    title("Groups")
                    description(permissionService.getGroups()
                        .joinToString("\n"))
                }.awaitSingle()
            }
        }

        command("show", "Show all permissions of a group") {
            arg("group", "the name of the group")

            onCall {
                val group = args["group"]
                channel().createEmbedCompat {
                    title("Permissions for group $group")
                    description(
                        permissionService.getPermissions(group)
                            ?.joinToString("\n") ?: "Group not found")
                }.awaitSingle()
            }
        }

        command("add", "Create a new group") {
            arg("group", "the name of the group")

            onCall {
                val group = args["group"]

                channel().createEmbedCompat {
                    description(when {
                        permissionService.addGroup(group) -> "The group $group has been created"
                        else -> "The group $group already exists"
                    })
                }.awaitSingle()
            }
        }

        command("remove", "Remove a group") {
            arg("group", "the name of the group")

            onCall {
                val group = args["group"]

                if (group == Config.permissions.default_group_name || group == Config.permissions.admin_group_name) {
                    channel().createEmbedCompat {
                        description("The default and admin groups cannot be deleted")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbedCompat {
                    description(when {
                        permissionService.removeGroup(group) -> "The group $group has been deleted"
                        else -> "The group $group was not found"
                    })
                }.awaitSingle()
            }
        }
    }
}
