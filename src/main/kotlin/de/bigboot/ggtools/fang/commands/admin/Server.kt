package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import de.bigboot.ggtools.fang.service.ServerService
import kotlinx.coroutines.reactive.awaitSingle
import org.koin.core.inject

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

                val msg = channel().createEmbed {
                    it.setDescription("Adding $name to the list of servers")
                }.awaitSingle()

                if (serverService.checkServer(name, url, apiKey)) {
                    serverService.addServer(name, url, apiKey)
                    msg.edit { edit ->
                        edit.addEmbed {
                            it.setDescription("Adding $name to the list of servers -> Success")
                        }
                    }.awaitSingle()
                } else {
                    msg.edit { edit ->
                        edit.addEmbed {
                            it.setDescription("Adding $name to the list of servers -> Failed")
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
                    channel().createEmbed {
                        it.setDescription("$name removed from the list of servers.")
                    }.awaitSingle()
                } else {
                    channel().createEmbed {
                        it.setDescription("$name not the list of servers.")
                    }.awaitSingle()
                }
            }
        }
    }
}
