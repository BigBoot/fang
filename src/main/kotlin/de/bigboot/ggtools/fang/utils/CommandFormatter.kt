package de.bigboot.ggtools.fang.utils

import de.bigboot.ggtools.fang.Argument
import de.bigboot.ggtools.fang.Command

private fun buildCommandTree(command: Command, tree: StringBuilder, prefix: String, last: Boolean): StringBuilder {
    val branch = when (last) {
        true -> "└── "
        false -> "├── "
    }

    tree.appendln("$prefix$branch${command.name}")

    if (command is Command.Group) {
        command.commands.values.forEachIndexed { i, subcommand ->
            val sublast = command.commands.size - 1 == i

            val subprefix = prefix + when {
                last -> "    "
                else -> "│   "
            }
            buildCommandTree(subcommand, tree, subprefix, sublast)
        }

        if (!last) {
            tree.appendln("$prefix│   ")
        }
    }

    return tree
}

fun formatCommandTree(commands: Collection<Command>): String {
    val tree = StringBuilder()
    commands.forEachIndexed { i, subcommand ->
        val last = commands.size - 1 == i
        buildCommandTree(subcommand, tree, "", last)
    }

    return tree.toString()
}

private fun formatArg(arg: Argument) = when {
    arg.optional -> "[${arg.name}]"
    else -> "<${arg.name}>"
}

fun formatCommandHelp(name: String, command: Command): String {
    return when (command) {
        is Command.Invokable -> "$name ${command.args.joinToString(" ") { formatArg(it) }}"
        is Command.Group -> "$name <subcommand>"
    }
}