package de.bigboot.ggtools.fang.commands.admin.group

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.utils.findMember
import kotlinx.coroutines.reactive.awaitSingle

class Users : CommandGroupSpec("users", "Commands for managing users") {
    override val build: CommandGroupBuilder.() -> Unit = {
        command("show", "Show details about a user") {
            arg("user", "The user")

            onCall {
                val userName = args["user"]

                val user = guild().findMember(userName)

                if (user == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("User $userName not found")
                    }.awaitSingle()
                    return@onCall
                }

                val groups = permissionService.getGroupsByUser(user.id.asLong())

                channel().createEmbed { embed ->
                    embed.setTitle("Permissions of ${user.displayName}")
                    groups.forEach { group ->
                        embed.addField(group.first, group.second.joinToString("\n").ifBlank { "-" }, false)
                    }
                    if (groups.isEmpty()) {
                        embed.setDescription("User <@${user.id.asString()}> doesn't have any permissions")
                    }
                }.awaitSingle()
                return@onCall
            }
        }

        command("add", "Add a user to a group") {
            arg("user", "the user to add to the group")
            arg("group", "the group to add the user to")

            onCall {
                val userName = args["user"]
                val group = args["group"]

                val user = guild().findMember(userName)

                if (user == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("User $userName not found")
                    }.awaitSingle()
                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setDescription(
                        when {
                            permissionService.addUserToGroup(
                                user.id.asLong(),
                                group
                            ) -> "<@${user.id.asString()}> has been added to group $group"
                            else -> "Group $group not found"
                        }
                    )
                }.awaitSingle()
            }
        }

        command("remove", "Remove a user from a group") {
            arg("user", "the user to remove to the group")
            arg("group", "the group to remove the user from")

            onCall {
                val userName = args["user"]
                val group = args["group"]

                val user = guild().findMember(userName)

                if (user == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("User $userName not found")
                    }.awaitSingle()
                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setDescription(
                        when {
                            permissionService.removeUserFromGroup(user.id.asLong(), group) -> "<@${user.id.asString()}> has been removed from the group $group"
                            else -> "Group $group not found"
                        }
                    )
                }.awaitSingle()
            }
        }

        command("list", "List all users of a group") {
            arg("group", "the group to remove the user from")

            onCall {
                val group = args["group"]
                val users = permissionService.getUsersByGroup(group)

                if (users == null) {
                    channel().createEmbed { embed ->
                        embed.setDescription("Group $group not found")
                    }.awaitSingle()
                    return@onCall
                }

                channel().createEmbed { embed ->
                    embed.setTitle("Users in group: $group")
                    embed.setDescription(users.joinToString("\n"))
                }.awaitSingle()
            }
        }
    }
}
