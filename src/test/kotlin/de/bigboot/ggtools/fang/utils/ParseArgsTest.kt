package de.bigboot.ggtools.fang.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ParseArgsTest {
    @Test
    fun blankYieldsEmptyArgs() {
        assertTrue(parseArgs("").isEmpty())
    }

    @Test
    fun whitespacesOnlyYeildsEmptyArgs() {
        assertTrue(parseArgs("  \t \n").isEmpty())
    }

    @Test
    fun normalTokens() {
        assertEquals(listOf("a", "bee", "cee"), parseArgs("a\tbee  cee"))
    }

    @Test
    fun doubleQuotes() {
        assertEquals(listOf("hello world"), parseArgs("\"hello world\""))
    }

    @Test
    fun singleQuotes() {
        assertEquals(listOf("hello world"), parseArgs("'hello world'"))
    }

    @Test
    fun escapedDoubleQuotes() {
        assertEquals(listOf("\"hello world\""), parseArgs("\"\\\"hello world\\\""))
    }

    @Test
    fun noEscapeWithinSingleQuotes() {
        assertEquals(listOf("hello \\\" world"), parseArgs("'hello \\\" world'"))
    }

    @Test
    fun backToBackQuotedStringsShouldFormSingleToken() {
        assertEquals(listOf("foobarbaz"), parseArgs("\"foo\"'bar'baz"))
        assertEquals(listOf("three four"), parseArgs("\"three\"' 'four"))
    }

    @Test
    fun escapedSpacesDoNotBreakUpTokens() {
        assertEquals(listOf("three four"), parseArgs("three\\ four"))
    }

    @Test
    fun emptyDoubleQuotesYieldsListOfEmptyString() {
        assertEquals(listOf(""), parseArgs("\"\""))
    }

    @Test
    fun emptySingleQuotesYieldsListOfEmptyString() {
        assertEquals(listOf(""), parseArgs("''"))
    }

    @Test
    fun twoPairsEmptySingleQuotesYieldsListOfTwoEmptyStringWhenSeparated() {
        assertEquals(listOf("", ""), parseArgs("'' ''"))
    }

    @Test
    fun twoPairsEmptySingleQuotesYieldsListOfOneEmptyStringWhenNotSeparated() {
        assertEquals(listOf(""), parseArgs("''''"))
    }
}
