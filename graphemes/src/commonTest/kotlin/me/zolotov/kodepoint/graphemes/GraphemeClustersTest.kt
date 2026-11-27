package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.Codepoint
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphemeClustersTest {
    @Test
    fun examples() {
        assertEquals(0, "".graphemeClusterCount())
        assertEquals(3, "abc".graphemeClusterCount())
        assertEquals(1, "\r\n".graphemeClusterCount())
        assertEquals(1, "e\u0301".graphemeClusterCount())
        assertEquals(1, "\u00E9".graphemeClusterCount())
        assertEquals(1, "\uD83D\uDC4B\uD83C\uDFFD".graphemeClusterCount()) // 👋🏽
        assertEquals(1, "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67".graphemeClusterCount()) // 👨‍👩‍👧
        assertEquals(2, "\uD83C\uDDFA\uD83C\uDDE6\uD83C\uDDE9\uD83C\uDDEA".graphemeClusterCount()) // 🇺🇦🇩🇪
        assertEquals(2, "\uD83C\uDDFA\uD83C\uDDE6\uD83C\uDDE9".graphemeClusterCount()) // 🇺🇦🇩
        assertEquals(1, "\u0915\u094D\u0937".graphemeClusterCount()) // क्ष
        assertEquals(1, "\u1CF5\u0915".graphemeClusterCount()) // GB9c with a Linker that is not Extend
        assertEquals(1, "\u1100\u1161\u11A8".graphemeClusterCount())
        assertEquals(3, "a\uD800b".graphemeClusterCount())
    }

    @Test
    fun clusterRanges() {
        val text = "e\u0301x\r\n\uD83C\uDDFA\uD83C\uDDE6"
        val ranges = buildList { text.forEachGraphemeCluster { start, end -> add(start to end) } }
        assertEquals(listOf(0 to 2, 2 to 3, 3 to 5, 5 to 9), ranges)
        val reversed = buildList { text.forEachGraphemeClusterReversed { start, end -> add(start to end) } }
        assertEquals(ranges.reversed(), reversed)
        assertEquals(listOf("e\u0301", "x", "\r\n", "\uD83C\uDDFA\uD83C\uDDE6"), text.graphemeClusters().toList())
    }

    @Test
    fun boundaryQueries() {
        val text = "a\uD83D\uDC4B\uD83C\uDFFDb" // a 👋🏽 b
        assertContentEquals(
            listOf(true, true, false, false, false, true, true),
            (0..text.length).map { text.isGraphemeClusterBoundary(it) }
        )
        assertEquals(5, text.nextGraphemeClusterBoundary(1))
        assertEquals(5, text.nextGraphemeClusterBoundary(3))
        assertEquals(6, text.nextGraphemeClusterBoundary(5))
        assertEquals(6, text.nextGraphemeClusterBoundary(6))
        assertEquals(1, text.previousGraphemeClusterBoundary(5))
        assertEquals(1, text.previousGraphemeClusterBoundary(4))
        assertEquals(0, text.previousGraphemeClusterBoundary(1))
        assertEquals(0, text.previousGraphemeClusterBoundary(0))
        assertEquals(0, "".nextGraphemeClusterBoundary(0))
        assertEquals(0, "".previousGraphemeClusterBoundary(0))
        assertTrue("".isGraphemeClusterBoundary(0))
    }

    @Test
    fun indexValidation() {
        val text = "abc"
        assertFailsWith<IndexOutOfBoundsException> { text.isGraphemeClusterBoundary(-1) }
        assertFailsWith<IndexOutOfBoundsException> { text.isGraphemeClusterBoundary(4) }
        assertFailsWith<IndexOutOfBoundsException> { text.nextGraphemeClusterBoundary(text.length + 1) }
        assertFailsWith<IndexOutOfBoundsException> { text.nextGraphemeClusterBoundary(-1) }
        assertFailsWith<IndexOutOfBoundsException> { text.previousGraphemeClusterBoundary(-1) }
        assertFailsWith<IndexOutOfBoundsException> { text.previousGraphemeClusterBoundary(text.length + 1) }
    }

    @Test
    fun segmenter() {
        val segmenter = GraphemeClusterSegmenter()
        val codepoints = listOf(0x65, 0x301, 0x78, 0x0D, 0x0A, 0x1F1FA, 0x1F1E6, 0x1F1E9)
        assertEquals(
            listOf(true, false, true, true, false, true, false, true),
            codepoints.map { segmenter.isBoundaryBefore(Codepoint(it)) }
        )
        segmenter.reset()
        assertTrue(segmenter.isBoundaryBefore(Codepoint(0x301)))
        assertFalse(segmenter.isBoundaryBefore(Codepoint(0x301)))
    }

    @Test
    fun segmenterAfterAsciiRun() {
        // ASCII controls break before marks; ASCII letters do not.
        val segmenter = GraphemeClusterSegmenter()
        assertEquals(
            listOf(true, true, true, true, false, true, true),
            listOf(0x61, 0x62, 0x09, 0x63, 0x301, 0x01, 0x301).map { segmenter.isBoundaryBefore(Codepoint(it)) }
        )
    }

    @Test
    fun unicodeVersion() {
        assertEquals("18.0.0", Graphemes.UNICODE_VERSION)
    }
}
