package de.bigboot.ggtools.fang.commands.admin.group

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.Config
import kotlinx.coroutines.reactive.awaitSingle

class Permissions : CommandGroupSpec("permissions", "Manage group permissions") {
    override val build: CommandGroupBuilder.() -> Unit = {
        command("add", "Grant a permission to a group") {
            arg("group", "The group to modify")
            arg("permission", "The permission to grant to the group")

            onCall {
                val group = args["group"]
                val permission = args["permission"]

                if (group == Config.ADMIN_GROUP_NAME) {
                    channel().createEmbed { embed ->
                        embed.setDescription("The admin groups cannot be modified")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setDescription(
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

                if (group == Config.ADMIN_GROUP_NAME) {
                    channel().createEmbed { embed ->
                        embed.setDescription("The admin groups cannot be modified")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setDescription(
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