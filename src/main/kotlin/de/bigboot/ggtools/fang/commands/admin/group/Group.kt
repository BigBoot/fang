package de.bigboot.ggtools.fang.commands.admin.group

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.Config
import kotlinx.coroutines.reactive.awaitSingle

class Group : de.bigboot.ggtools.fang.CommandGroupSpec("group", "Commands for managing groups") {
    override val build: CommandGroupBuilder.() -> Unit = {
        group(Permissions())
        group(Users())

        command("list", "List all groups") {
            onCall {
                channel().createEmbed { embed ->
                    embed.setTitle("Groups")
                    embed.setDescription(permissionService.getGroups()
                        .joinToString("\n"))
                }.awaitSingle()
            }
        }

        command("show", "Show all permissions of a group") {
            arg("group", "the name of the group")

            onCall {
                val group = args["group"]
                channel().createEmbed { embed ->
                    embed.setTitle("Permissions for group $group")
                    embed.setDescription(
                        permissionService.getPermissions(group)
                            ?.joinToString("\n") ?: "Group not found")
                }.awaitSingle()
            }
        }

        command("add", "Create a new group") {
            arg("group", "the name of the group")

            onCall {
                val group = args["group"]

                channel().createEmbed { embed ->
                    embed.setDescription(when {
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

                if(group == Config.DEFAULT_GROUP_NAME || group == Config.ADMIN_GROUP_NAME) {
                    channel().createEmbed { embed ->
                        embed.setDescription("The default and admin groups cannot be deleted")
                    }.awaitSingle()

                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setDescription(when {
                        permissionService.removeGroup(group) -> "The group $group has been deleted"
                        else -> "The group $group was not found"
                    })
                }.awaitSingle()
            }
        }
    }
}