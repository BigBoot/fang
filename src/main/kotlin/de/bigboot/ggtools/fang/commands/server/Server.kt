package de.bigboot.ggtools.fang.commands.server

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.api.model.AdminPWRequest
import de.bigboot.ggtools.fang.api.model.KillRequest
import de.bigboot.ggtools.fang.api.model.StartRequest
import discord4j.core.`object`.reaction.ReactionEmoji
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono

class Server : CommandGroupSpec("server", "Commands for controlling servers") {
    override val build: CommandGroupBuilder.() -> Unit = {
        command("list", "List all servers") {
            onCall {
                channel().createEmbed { embed ->
                    embed.setTitle("Servers")
                    embed.setDescription(serverService.getAllServers()
                        .joinToString { "${it.name} -> ${it.url}" })
                }.awaitSingle()
            }
        }

        command("start", "Start a server") {
            arg("server", "The server to start")
            arg("map", "The map", true)
            arg("max_players", "The max number of players", true)
            arg("creature1", "The first creature of the loadout", true)
            arg("creature2", "The first creature of the loadout", true)
            arg("creature3", "The first creature of the loadout", true)

            onCall {
                val server = args["server"]
                val map = args.optional("map") ?: "lv_canyon"
                val maxPlayers = args.optional("max_players")?.toIntOrNull()
                val creature1 = args.optional("creature1")
                val creature2 = args.optional("creature2")
                val creature3 = args.optional("creature3")

                val client = serverService.getClient(server)

                if (client == null) {
                    channel().createEmbed {
                        it.setDescription("Unknown server $server.")
                    }.awaitSingle()

                    return@onCall
                }

                val request = StartRequest(
                    map = map,
                    maxPlayers = maxPlayers,
                    creature0 = creature1,
                    creature1 = creature2,
                    creature2 = creature3
                )

                val response = client.start(request)

                if (response.openUrl != null) {
                    channel().createEmbed {
                        it.setDescription("open: ${response.openUrl}")
                    }.awaitSingle()
                } else if (response.error != null) {
                    channel().createEmbed {
                        it.setDescription("Couldn't start the server: ${response.error}.")
                    }.awaitSingle()
                }
            }
        }

        command("kill", "Kill a server") {
            arg("server", "The server to kill")
            arg("instance_id", "The instance to kill, will default to the first instance", true)

            onCall {
                val server = args["server"]
                val instanceId = args.optional("instance_id")?.toIntOrNull() ?: 0

                val client = serverService.getClient(server)

                if (client == null) {
                    channel().createEmbed {
                        it.setDescription("Unknown server $server.")
                    }.awaitSingle()

                    return@onCall
                }

                val response = client.kill(
                    KillRequest(
                        instanceId
                    )
                )

                if (response.error != null) {
                    channel().createEmbed {
                        it.setDescription("Couldn't kill the server: ${response.error}.")
                    }.awaitSingle()
                } else {
                    channel().createEmbed {
                        it.setDescription("Server killed")
                    }.awaitSingle()
                }
            }
        }

        command("players", "List current players in a server") {
            arg("server", "The name of the server, use `list_servers` to get a list of servers")
            arg("instance_id", "The id of the instance, optional if the server only has a single instance", true)

            onCall {
                val server = args["server"]
                val instanceID = args.optional("instance_id")?.toIntOrNull() ?: 0

                val client = serverService.getClient(server)

                if (client == null) {
                    channel().createEmbed {
                        it.setDescription("Unknown server $server.")
                    }.awaitSingle()

                    return@onCall
                }

                val response = client.getPlayers(instanceID)
                channel().createEmbed {
                    it.setDescription("Players:\n" + response.joinToString("\n") { player ->
                        "${player.name} -> ${player.hero ?: "Selecting"}"
                    })
                }.awaitSingle()
            }
        }

        command("admin_pw", "Get the admin password for a running instance.") {
            arg("server", "The name of the server")
            arg("instance_id", "The id of the instance, optional if the server only has a single instance", true)

            onCall {
                val server = args["server"]
                val instanceID = args.optional("instance_id")?.toIntOrNull() ?: 0

                val client = serverService.getClient(server)

                if (client == null) {
                    channel().createEmbed {
                        it.setDescription("Unknown server $server.")
                    }.awaitSingle()

                    return@onCall
                }

                val response = client.getAdminPW(
                    AdminPWRequest(
                        instanceID
                    )
                )

                if (response.adminPW == null) {
                    channel().createEmbed {
                        it.setDescription("No admin password available, are you sure the instance is running?")
                    }.awaitSingle()

                    return@onCall
                }

                val privateMessage = message.author.orElse(null)
                    ?.privateChannel
                    ?.awaitSingle()
                    ?.createMessage {
                        it.setContent("Admin password for this instance: ${response.adminPW}")
                    }
                    ?.onErrorResume { Mono.empty() }
                    ?.awaitFirstOrNull()

                if (privateMessage != null) {
                    message.addReaction(ReactionEmoji.unicode("\uD83D\uDC4C")).awaitFirstOrNull()
                } else {
                    channel().createEmbed {
                        it.setDescription("Could not send a DM.\nMake sure you can receive direct messages from server members.")
                    }.awaitSingle()
                }
            }
        }
    }
}
