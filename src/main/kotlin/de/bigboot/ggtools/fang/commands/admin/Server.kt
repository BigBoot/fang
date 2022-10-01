package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.service.ServerService
import de.bigboot.ggtools.fang.utils.addEmbedCompat
import de.bigboot.ggtools.fang.utils.createEmbedCompat
import de.bigboot.ggtools.fang.utils.editCompat
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.component.inject

class Server : CommandGroupSpec("server", "Commands for managing servers") {
    private val serverService by inject<ServerService>()

    override val build: CommandGroupBuilder.() -> Unit = {
        command("add", "Add a server") {
            arg("name", "How to address this server when issuing commands")
            arg("url", "The url to the server")
            arg("api_key", "The api_key of the server")

            onCall {
                val name = args["name"]
                val url = args["url"]
                val apiKey = args["api_key"]

                val msg = channel().createEmbedCompat {
                    description("Adding $name to the list of servers")
                }.awaitSingle()

                if (serverService.checkServer(name, url, apiKey)) {
                    serverService.addServer(name, url, apiKey)
                    msg.editCompat {
                        addEmbedCompat {
                            description("Adding $name to the list of servers -> Success")
                        }
                    }.awaitSingle()
                } else {
                    msg.editCompat {
                        addEmbedCompat {
                            description("Adding $name to the list of servers -> Failed")
                        }
                    }.awaitSingle()
                }
            }
        }

        command("remove", "Remove server") {
            arg("name", "The name of the server to remove")

            onCall {
                val name = args["name"]

                if (serverService.getClient(name) != null) {
                    serverService.removeServer(name)
                    channel().createEmbedCompat {
                        description("$name removed from the list of servers.")
                    }.awaitSingle()
                } else {
                    channel().createEmbedCompat {
                        description("$name not the list of servers.")
                    }.awaitSingle()
                }
            }
        }
    }
}
