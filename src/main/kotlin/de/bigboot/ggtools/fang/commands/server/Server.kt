package de.bigboot.ggtools.fang.commands.server

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.api.agent.model.AdminPWRequest
import de.bigboot.ggtools.fang.api.agent.model.KillRequest
import de.bigboot.ggtools.fang.api.agent.model.StartRequest
import de.bigboot.ggtools.fang.service.ServerService
import de.bigboot.ggtools.fang.utils.*
import discord4j.core.`object`.reaction.ReactionEmoji
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject
import reactor.core.publisher.Mono

class Server : CommandGroupSpec("server", "Commands for controlling servers") {
    val serverService by inject<ServerService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("list", "List all servers") {
            onCall {
                {addEmbedCompat {
                    title("Servers")
                    description(serverService.getAllServers()
                        .joinToString { "${it.name} -> ${it.url}" })
                 }}
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
                    return@onCall {addEmbedCompat {
                        description("Unknown server $server.")
                    }}
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
                    return@onCall {addEmbedCompat {
                        description("open ${response.openUrl}")
                    }}
                } else if (response.error != null) {
                    return@onCall {addEmbedCompat {
                        description("Couldn't start the server: ${response.error}.")
                     }}
                }

                return@onCall {addEmbedCompat {
                    description("A unknow error happened :pensive:")
                }}
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
                    return@onCall {addEmbedCompat {
                        description("Unknown server $server.")
                    }}
                }

                val response = client.kill(
                    KillRequest(
                        instanceId
                    )
                )

                if (response.error != null) {
                    return@onCall {addEmbedCompat {
                        description("Couldn't kill the server: ${response.error}.")
                    }}
                } else {
                    return@onCall {addEmbedCompat {
                        description("Server killed")
                    }}
                }
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
                    return@onCall {addEmbedCompat {
                        description("Unknown server $server.")
                    }}
                }

                val response = client.getAdminPW(
                    AdminPWRequest(
                        instanceID
                    )
                )

                if (response.adminPW == null) {
                    return@onCall {addEmbedCompat {
                        description("No admin password available, are you sure the instance is running?")
                    }}
                }

                val privateMessage = author()
                    .privateChannel
                    ?.awaitSingle()
                    ?.createMessageCompat {
                        content("Admin password for this instance: ${response.adminPW}")
                    }
                    ?.onErrorResume { Mono.empty() }
                    ?.awaitFirstOrNull()

                if (privateMessage == null) {
                    return@onCall {addEmbedCompat {
                        description("Could not send a DM.\nMake sure you can receive direct messages from server members.")
                    }}
                }
                else {
                    return@onCall {addEmbedCompat {
                        description("Sent you the admin password!")
                    }}

                }
            }
        }
    }
}
