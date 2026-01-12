package me.zolotov.kodepoint

import kotlin.test.Test
import kotlin.test.assertEquals

class AppendableExtensionsTest {

    @Test
    fun appendCodePointAscii() {
        val sb = StringBuilder()
        sb.appendCodePoint(Codepoint('A'.code))
        assertEquals("A", sb.toString())
    }

    @Test
    fun appendCodePointBmp() {
        val sb = StringBuilder()
        sb.appendCodePoint(Codepoint(0x00E9)) // é (Latin Small Letter E with Acute)
        assertEquals("\u00E9", sb.toString())
    }

    @Test
    fun appendCodePointSupplementary() {
        val sb = StringBuilder()
        sb.appendCodePoint(Codepoint(0x1F600)) // Grinning Face
        assertEquals("\uD83D\uDE00", sb.toString())
        assertEquals(2, sb.length) // Surrogate pair = 2 chars
    }

    @Test
    fun appendCodePointChaining() {
        val result = StringBuilder()
            .appendCodePoint(Codepoint('H'.code))
            .appendCodePoint(Codepoint('i'.code))
            .appendCodePoint(Codepoint(0x1F44B)) // Waving Hand
            .toString()
        assertEquals("Hi\uD83D\uDC4B", result)
    }

    @Test
    fun appendCodePointToExistingContent() {
        val sb = StringBuilder("Hello ")
        sb.appendCodePoint(Codepoint(0x1F30D)) // Earth Globe Europe-Africa
        assertEquals("Hello \uD83C\uDF0D", sb.toString())
    }

    @Test
    fun appendCodePointMultipleSupplementary() {
        val sb = StringBuilder()
        sb.appendCodePoint(Codepoint(0x1F600)) // Grinning Face
        sb.appendCodePoint(Codepoint(0x1F44D)) // Thumbs Up
        sb.appendCodePoint(Codepoint(0x1F389)) // Party Popper
        assertEquals("\uD83D\uDE00\uD83D\uDC4D\uD83C\uDF89", sb.toString())
        assertEquals(6, sb.length) // 3 supplementary chars = 6 UTF-16 code units
    }

    @Test
    fun appendCodePointBmpBoundary() {
        val sb = StringBuilder()
        // U+FFFF is the last BMP codepoint
        sb.appendCodePoint(Codepoint(0xFFFF))
        assertEquals(1, sb.length)

        // U+10000 is the first supplementary codepoint
        sb.appendCodePoint(Codepoint(0x10000))
        assertEquals(3, sb.length) // 1 + 2 (surrogate pair)
    }

    @Test
    fun appendCodePointMaxCodepoint() {
        val sb = StringBuilder()
        sb.appendCodePoint(Codepoint(0x10FFFF)) // Maximum valid Unicode codepoint
        assertEquals(2, sb.length)
        assertEquals("\uDBFF\uDFFF", sb.toString())
    }

    @Test
    fun appendCodePointRoundTrip() {
        // Verify that appending and then reading back gives the same codepoint
        val originalCodepoints = listOf(
            0x0041,   // A
            0x00E9,   // é
            0x4E2D,   // Chinese character
            0x1F600,  // Grinning Face
            0x10FFFF  // Max codepoint
        )

        val sb = StringBuilder()
        for (cp in originalCodepoints) {
            sb.appendCodePoint(Codepoint(cp))
        }

        val readBack = mutableListOf<Int>()
        sb.forEachCodepoint { readBack.add(it.codepoint) }

        assertEquals(originalCodepoints, readBack)
    }
}
