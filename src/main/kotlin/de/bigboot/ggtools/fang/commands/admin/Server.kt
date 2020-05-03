package de.bigboot.ggtools.fang.commands.admin

import de.bigboot.ggtools.fang.CommandGroupBuilder
import de.bigboot.ggtools.fang.CommandGroupSpec
import kotlinx.coroutines.reactive.awaitSingle

class Server : CommandGroupSpec("server", "Commands for managing servers") {
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

                if (serverManager.checkServer(name, url, apiKey)) {
                    serverManager.addServer(name, url, apiKey)
                    msg.edit { edit ->
                        edit.setEmbed {
                            it.setDescription("Adding $name to the list of servers -> Success")
                        }
                    }.awaitSingle()
                } else {
                    msg.edit { edit ->
                        edit.setEmbed {
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

                if (serverManager.getClient(name) != null) {
                    serverManager.removeServer(name)
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