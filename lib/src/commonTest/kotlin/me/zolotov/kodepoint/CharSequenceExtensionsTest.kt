package me.zolotov.kodepoint

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharSequenceExtensionsTest {
    // Supplementary character: U+1F600 (😀) encoded as surrogate pair
    private val grinningFace = "\uD83D\uDE00"
    private val grinningFaceCodepoint = 0x1F600

    // Another supplementary: U+1F44D (👍)
    private val thumbsUp = "\uD83D\uDC4D"
    private val thumbsUpCodepoint = 0x1F44D

    // Lone surrogates for malformed tests
    private val loneHighSurrogate = '\uD83D'
    private val loneLowSurrogate = '\uDE00'

    @Test
    fun codePointAtAscii() {
        val text = "Hello"
        assertEquals('H'.code, text.codePointAt(0).codepoint)
        assertEquals('e'.code, text.codePointAt(1).codepoint)
        assertEquals('o'.code, text.codePointAt(4).codepoint)
    }

    @Test
    fun codePointAtSupplementary() {
        val text = "A${grinningFace}B"
        assertEquals('A'.code, text.codePointAt(0).codepoint)
        assertEquals(grinningFaceCodepoint, text.codePointAt(1).codepoint)
        // Index 2 is the low surrogate, but codePointAt should return just that char
        assertEquals(loneLowSurrogate.code, text.codePointAt(2).codepoint)
        assertEquals('B'.code, text.codePointAt(3).codepoint)
    }

    @Test
    fun codePointAtLoneHighSurrogate() {
        val text = "${loneHighSurrogate}A"
        assertEquals(loneHighSurrogate.code, text.codePointAt(0).codepoint)
        assertEquals('A'.code, text.codePointAt(1).codepoint)
    }

    @Test
    fun codePointAtLoneLowSurrogate() {
        val text = "A${loneLowSurrogate}"
        assertEquals('A'.code, text.codePointAt(0).codepoint)
        assertEquals(loneLowSurrogate.code, text.codePointAt(1).codepoint)
    }

    @Test
    fun codePointAtOutOfBounds() {
        val text = "Hello"
        assertFailsWith<IndexOutOfBoundsException> { text.codePointAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { text.codePointAt(5) }
        assertFailsWith<IndexOutOfBoundsException> { "".codePointAt(0) }
    }

    @Test
    fun codePointBeforeAscii() {
        val text = "Hello"
        assertEquals('H'.code, text.codePointBefore(1).codepoint)
        assertEquals('l'.code, text.codePointBefore(4).codepoint)
    }

    @Test
    fun codePointBeforeSupplementary() {
        val text = "A${grinningFace}B"
        assertEquals('A'.code, text.codePointBefore(1).codepoint)
        // Before index 3 (after the surrogate pair) should return the full codepoint
        assertEquals(grinningFaceCodepoint, text.codePointBefore(3).codepoint)
    }

    @Test
    fun codePointBeforeOutOfBounds() {
        val text = "Hello"
        assertFailsWith<IndexOutOfBoundsException> { text.codePointBefore(0) }
        assertFailsWith<IndexOutOfBoundsException> { text.codePointBefore(5) }
    }

    @Test
    fun forEachCodepointAscii() {
        val text = "ABC"
        val result = mutableListOf<Int>()
        text.forEachCodepoint { result.add(it.codepoint) }
        assertEquals(listOf('A'.code, 'B'.code, 'C'.code), result)
    }

    @Test
    fun forEachCodepointWithSupplementary() {
        val text = "A${grinningFace}B${thumbsUp}C"
        val result = mutableListOf<Int>()
        text.forEachCodepoint { result.add(it.codepoint) }
        assertEquals(
            listOf('A'.code, grinningFaceCodepoint, 'B'.code, thumbsUpCodepoint, 'C'.code),
            result
        )
    }

    @Test
    fun forEachCodepointWithLoneHighSurrogate() {
        // High surrogate followed by non-surrogate should yield both separately
        val text = "${loneHighSurrogate}A"
        val result = mutableListOf<Int>()
        text.forEachCodepoint { result.add(it.codepoint) }
        assertEquals(listOf(loneHighSurrogate.code, 'A'.code), result)
    }

    @Test
    fun forEachCodepointWithLoneLowSurrogate() {
        val text = "A${loneLowSurrogate}"
        val result = mutableListOf<Int>()
        text.forEachCodepoint { result.add(it.codepoint) }
        assertEquals(listOf('A'.code, loneLowSurrogate.code), result)
    }

    @Test
    fun forEachCodepointWithHighSurrogateAtEnd() {
        val text = "A${loneHighSurrogate}"
        val result = mutableListOf<Int>()
        text.forEachCodepoint { result.add(it.codepoint) }
        assertEquals(listOf('A'.code, loneHighSurrogate.code), result)
    }

    @Test
    fun forEachCodepointEmpty() {
        val result = mutableListOf<Int>()
        "".forEachCodepoint { result.add(it.codepoint) }
        assertEquals(emptyList(), result)
    }

    @Test
    fun forEachCodepointReversedAscii() {
        val text = "ABC"
        val result = mutableListOf<Int>()
        text.forEachCodepointReversed { result.add(it.codepoint) }
        assertEquals(listOf('C'.code, 'B'.code, 'A'.code), result)
    }

    @Test
    fun forEachCodepointReversedWithSupplementary() {
        val text = "A${grinningFace}B"
        val result = mutableListOf<Int>()
        text.forEachCodepointReversed { result.add(it.codepoint) }
        assertEquals(listOf('B'.code, grinningFaceCodepoint, 'A'.code), result)
    }

    @Test
    fun forEachCodepointReversedWithLoneLowSurrogate() {
        // Low surrogate preceded by non-surrogate should yield both separately
        val text = "A${loneLowSurrogate}"
        val result = mutableListOf<Int>()
        text.forEachCodepointReversed { result.add(it.codepoint) }
        assertEquals(listOf(loneLowSurrogate.code, 'A'.code), result)
    }

    @Test
    fun forEachCodepointReversedWithLowSurrogateAtStart() {
        val text = "${loneLowSurrogate}A"
        val result = mutableListOf<Int>()
        text.forEachCodepointReversed { result.add(it.codepoint) }
        assertEquals(listOf('A'.code, loneLowSurrogate.code), result)
    }

    @Test
    fun codepointsForwardAscii() {
        val text = "ABC"
        val result = text.codepoints().map { it.codepoint }.toList()
        assertEquals(listOf('A'.code, 'B'.code, 'C'.code), result)
    }

    @Test
    fun codepointsForwardWithSupplementary() {
        val text = "A${grinningFace}B"
        val result = text.codepoints().map { it.codepoint }.toList()
        assertEquals(listOf('A'.code, grinningFaceCodepoint, 'B'.code), result)
    }

    @Test
    fun codepointsForwardWithLoneHighSurrogate() {
        // This was the bug: lone high surrogate followed by non-low-surrogate was being skipped
        val text = "${loneHighSurrogate}A"
        val result = text.codepoints().map { it.codepoint }.toList()
        assertEquals(listOf(loneHighSurrogate.code, 'A'.code), result)
    }

    @Test
    fun codepointsForwardWithHighSurrogateAtEnd() {
        val text = "A${loneHighSurrogate}"
        val result = text.codepoints().map { it.codepoint }.toList()
        assertEquals(listOf('A'.code, loneHighSurrogate.code), result)
    }

    @Test
    fun codepointsBackwardAscii() {
        val text = "ABC"
        val result = text.codepointsReversed().map { it.codepoint }.toList()
        assertEquals(listOf('C'.code, 'B'.code, 'A'.code), result)
    }

    @Test
    fun codepointsBackwardWithSupplementary() {
        val text = "A${grinningFace}B"
        val result = text.codepointsReversed().map { it.codepoint }.toList()
        assertEquals(listOf('B'.code, grinningFaceCodepoint, 'A'.code), result)
    }

    @Test
    fun codepointsBackwardWithLoneLowSurrogate() {
        // This was the bug: lone low surrogate preceded by non-high-surrogate was being skipped
        val text = "A${loneLowSurrogate}"
        val result = text.codepointsReversed().map { it.codepoint }.toList()
        assertEquals(listOf(loneLowSurrogate.code, 'A'.code), result)
    }

    @Test
    fun codepointsBackwardWithLowSurrogateAtStart() {
        val text = "${loneLowSurrogate}A"
        val result = text.codepointsReversed().map { it.codepoint }.toList()
        assertEquals(listOf('A'.code, loneLowSurrogate.code), result)
    }

    @Test
    fun codepointsEmpty() {
        val result = "".codepoints().toList()
        assertEquals(emptyList(), result)
    }
}
