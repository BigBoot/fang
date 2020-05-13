package de.bigboot.ggtools.fang.utils

import java.util.*

@Suppress("ComplexMethod")
fun parseArgs(string: CharSequence): List<String> {
    val tokens: MutableList<String> = ArrayList()
    var escaping = false
    var quoting = false
    var lastCloseQuoteIndex = Int.MIN_VALUE
    var current = StringBuilder()
    var quoteChar = ' '

    for (i in string.indices) {
        val c = string[i]

        when {
            escaping -> {
                current.append(c)
                escaping = false
            }

            c == '\\' && !(quoting && quoteChar == '\'') -> {
                escaping = true
            }

            quoting && c == quoteChar -> {
                quoting = false
                lastCloseQuoteIndex = i
            }

            !quoting && (c == '\'' || c == '"') -> {
                quoting = true; quoteChar = c
            }

            !quoting && Character.isWhitespace(c) -> {
                if (current.isNotEmpty() || lastCloseQuoteIndex == i - 1) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                }
            }

            else -> current.append(c)
        }
    }
    if (current.isNotEmpty() || lastCloseQuoteIndex == string.length - 1) {
        tokens.add(current.toString())
    }
    return tokens
}
