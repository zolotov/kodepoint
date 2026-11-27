package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.forEachCodepoint
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/** The iteration and boundary APIs must not allocate per cluster or code point. */
class AllocationTest {
    private val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean

    /** About 1 MB of mixed text: ASCII, combining marks, CJK, ZWJ emoji, flags, Hangul and Devanagari. */
    private val text: String = buildString {
        val sample = "Hello, world! e\u0301 \u00E9 \u4E16\u754C " +
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67 \uD83C\uDDFA\uD83C\uDDE6\uD83C\uDDE9\uD83C\uDDEA " +
            "\uD83D\uDC4B\uD83C\uDFFD \u1100\u1161\u11A8 \u0915\u094D\u0937\r\n"
        while (length < 1_000_000) append(sample)
    }

    private var sink = 0

    private fun allocatedBy(block: () -> Unit): Long {
        repeat(5) { block() } // warm up, so the JIT has compiled and inlined the hot path
        @Suppress("DEPRECATION") // Thread.threadId() needs JDK 19; the tests run on the JDK 11 API
        val thread = Thread.currentThread().id
        val before = threads.getThreadAllocatedBytes(thread)
        block()
        return threads.getThreadAllocatedBytes(thread) - before
    }

    private fun assertAllocatesLittle(api: String, block: () -> Unit) {
        val bytes = allocatedBy(block)
        assertTrue(bytes < 1024, "$api allocated $bytes bytes over ${text.length} chars")
    }

    @Test
    fun forward() = assertAllocatesLittle("forEachGraphemeCluster") {
        text.forEachGraphemeCluster { start, end -> sink += end - start }
    }

    @Test
    fun count() = assertAllocatesLittle("graphemeClusterCount") { sink += text.graphemeClusterCount() }

    @Test
    fun reversed() = assertAllocatesLittle("forEachGraphemeClusterReversed") {
        text.forEachGraphemeClusterReversed { start, end -> sink += end - start }
    }

    @Test
    fun boundaryQueries() = assertAllocatesLittle("boundary queries") {
        var index = 0
        while (index < text.length) {
            if (text.isGraphemeClusterBoundary(index)) sink++
            sink += text.nextGraphemeClusterBoundary(index) + text.previousGraphemeClusterBoundary(index)
            index += 7
        }
    }

    @Test
    fun segmenter() {
        val segmenter = GraphemeClusterSegmenter()
        assertAllocatesLittle("GraphemeClusterSegmenter") {
            segmenter.reset()
            text.forEachCodepoint { if (segmenter.isBoundaryBefore(it)) sink++ }
        }
    }

    @Test
    fun emoji() = assertAllocatesLittle("forEachEmoji") {
        text.forEachEmoji { start, _, kind -> sink += start + kind.ordinal }
    }
}
