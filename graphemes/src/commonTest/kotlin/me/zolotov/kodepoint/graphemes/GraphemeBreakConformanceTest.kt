package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.appendCodePoint
import me.zolotov.kodepoint.forEachCodepoint
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs the official `GraphemeBreakTest.txt` cases (generated into [GraphemeBreakTestData]) through
 * every segmentation API, then checks that the APIs agree with each other on longer texts.
 */
class GraphemeBreakConformanceTest {
    private class Case(val lineNumber: Int, val source: String, val text: String, val boundaries: List<Int>)

    private val cases: List<Case> = GraphemeBreakTestData.CASES.split('\u001F').filter { it.isNotEmpty() }.map { line ->
        val lineNumber = line.substringBefore(':').toInt()
        val source = line.substringAfter(':')
        val text = StringBuilder()
        val boundaries = mutableListOf<Int>()
        for (token in source.split(' ').filter { it.isNotEmpty() }) {
            when (token) {
                "÷" -> boundaries += text.length
                "×" -> Unit
                else -> text.appendCodePoint(token.toInt(16))
            }
        }
        Case(lineNumber, source, text.toString(), boundaries)
    }

    @Test
    fun allCasesAreGenerated() {
        assertEquals(GraphemeBreakTestData.CASE_COUNT, cases.size)
    }

    @Test
    fun forwardIteration() = forEachCase { case ->
        assertEquals(case.boundaries, forwardBoundaries(case.text), case.describe())
    }

    @Test
    fun reversedIteration() = forEachCase { case ->
        assertEquals(case.boundaries, reversedBoundaries(case.text), case.describe())
    }

    @Test
    fun boundaryTest() = forEachCase { case ->
        assertEquals(case.boundaries, boundaryTestBoundaries(case.text), case.describe())
    }

    @Test
    fun nextBoundary() = forEachCase { case ->
        assertEquals(case.boundaries, nextBoundaries(case.text), case.describe())
    }

    @Test
    fun previousBoundary() = forEachCase { case ->
        assertEquals(case.boundaries, previousBoundaries(case.text), case.describe())
    }

    @Test
    fun streamingSegmenter() = forEachCase { case ->
        assertEquals(case.boundaries, segmenterBoundaries(case.text, GraphemeClusterSegmenter()), case.describe())
    }

    @Test
    fun segmenterResetStartsANewText() {
        val segmenter = GraphemeClusterSegmenter()
        for (case in cases) {
            segmenter.reset()
            assertEquals(case.boundaries, segmenterBoundaries(case.text, segmenter), case.describe())
        }
    }

    @Test
    fun allCasesConcatenated() {
        assertApisAgree(cases.joinToString("") { it.text })
    }

    @Test
    fun randomTexts() {
        // One code point of every class the test file uses, plus lone surrogates.
        val alphabet = buildSet {
            cases.forEach { case -> case.text.forEachCodepoint { add(it.codepoint) } }
            add(0xD800)
            add(0xDC00)
        }.sorted()
        val random = Random(20261127)
        repeat(1_000) {
            val text = buildString {
                repeat(random.nextInt(1, 40)) { appendCodePoint(alphabet[random.nextInt(alphabet.size)]) }
            }
            assertApisAgree(text)
        }
    }

    @Test
    fun longRuns() {
        val regionalIndicators = (1..201).joinToString("") { if (it % 2 == 0) "\uD83C\uDDE6" else "\uD83C\uDDFA" }
        assertEquals(101, regionalIndicators.graphemeClusterCount())
        assertApisAgree(regionalIndicators)
        assertApisAgree("a" + regionalIndicators)
        assertApisAgree("\u0600" + regionalIndicators + "\u0308")

        val marks = "a" + "\u0308".repeat(1_000)
        assertEquals(1, marks.graphemeClusterCount())
        assertApisAgree(marks + marks)

        val pictographicChain = "\uD83D\uDC4D" + "\u0308".repeat(500) + "\u200D\uD83D\uDC4D"
        assertEquals(1, pictographicChain.graphemeClusterCount())
        assertApisAgree(pictographicChain)

        val conjunct = "\u0915" + "\u094D\u0300".repeat(200) + "\u0915"
        assertEquals(1, conjunct.graphemeClusterCount())
        assertApisAgree(conjunct)
    }

    private fun assertApisAgree(text: String) {
        val forward = forwardBoundaries(text)
        val message = "boundaries of ${text.hex()}"
        assertEquals(forward, reversedBoundaries(text), message)
        assertEquals(forward, boundaryTestBoundaries(text), message)
        assertEquals(forward, nextBoundaries(text), message)
        assertEquals(forward, previousBoundaries(text), message)
        assertEquals(forward, segmenterBoundaries(text, GraphemeClusterSegmenter()), message)
        assertEquals(forward.size - 1, text.graphemeClusterCount(), message)
        assertEquals(text, text.graphemeClusters().joinToString(""), message)
        assertEquals(forward.zipWithNext { a, b -> text.substring(a, b) }, text.graphemeClusters().toList(), message)
    }

    private fun forEachCase(check: (Case) -> Unit) {
        assertEquals(GraphemeBreakTestData.CASE_COUNT, cases.size)
        cases.forEach(check)
    }

    private fun Case.describe() = "GraphemeBreakTest.txt line $lineNumber: $source"

    private fun String.hex() = buildList { forEachCodepoint { add(it.codepoint.toString(16).uppercase()) } }.toString()

    private fun forwardBoundaries(text: String): List<Int> = buildList {
        add(0)
        text.forEachGraphemeCluster { start, end ->
            assertEquals(last(), start)
            add(end)
        }
    }

    private fun reversedBoundaries(text: String): List<Int> = buildList {
        add(text.length)
        text.forEachGraphemeClusterReversed { start, end ->
            assertEquals(last(), end)
            add(start)
        }
    }.reversed()

    private fun boundaryTestBoundaries(text: String): List<Int> =
        (0..text.length).filter { text.isGraphemeClusterBoundary(it) }

    private fun nextBoundaries(text: String): List<Int> = buildList {
        var index = 0
        add(index)
        while (index < text.length) {
            index = text.nextGraphemeClusterBoundary(index)
            add(index)
        }
        assertEquals(text.length, text.nextGraphemeClusterBoundary(text.length))
        // From inside a cluster, the next boundary is the end of that cluster.
        for (i in 0 until text.length) {
            assertEquals(first { it > i }, text.nextGraphemeClusterBoundary(i), "next boundary after $i")
        }
    }

    private fun previousBoundaries(text: String): List<Int> {
        val boundaries = buildList {
            var index = text.length
            add(index)
            while (index > 0) {
                index = text.previousGraphemeClusterBoundary(index)
                add(index)
            }
        }.reversed()
        assertEquals(0, text.previousGraphemeClusterBoundary(0))
        for (i in 1..text.length) {
            assertEquals(boundaries.last { it < i }, text.previousGraphemeClusterBoundary(i), "previous boundary before $i")
        }
        return boundaries
    }

    private fun segmenterBoundaries(text: String, segmenter: GraphemeClusterSegmenter): List<Int> = buildList {
        var index = 0
        text.forEachCodepoint { codepoint ->
            if (segmenter.isBoundaryBefore(codepoint)) add(index)
            index += codepoint.charCount
        }
        add(text.length)
    }.distinct()
}
