package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.Codepoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/** Compares every property with the data files for all 1,114,112 code points. */
class PropertiesValidationTest {
    private val allCodepoints = 0..0x10FFFF

    private fun enumName(value: String): String =
        value.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

    @Test
    fun graphemeClusterBreak() {
        val file = "GraphemeBreakProperty.txt"
        assertEquals(listOf(listOf("Other")), DataFiles.missing(file))
        val expected = Array(0x110000) { GraphemeClusterBreak.OTHER }
        for ((range, fields) in DataFiles.ranges(file)) {
            val value = GraphemeClusterBreak.valueOf(enumName(fields[0]))
            for (cp in range) expected[cp] = value
        }
        assertAll("Grapheme_Cluster_Break") { cp -> expected[cp] to Codepoint(cp).getGraphemeClusterBreak() }
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint(-1).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint(0x110000).getGraphemeClusterBreak())
    }

    @Test
    fun indicConjunctBreak() {
        val file = "DerivedCoreProperties.txt"
        assertEquals(listOf(listOf("InCB", "None")), DataFiles.missing(file).filter { it.first() == "InCB" })
        val expected = Array(0x110000) { IndicConjunctBreak.NONE }
        for ((range, fields) in DataFiles.ranges(file)) {
            if (fields[0] != "InCB") continue
            val value = IndicConjunctBreak.valueOf(fields[1].uppercase())
            for (cp in range) expected[cp] = value
        }
        assertAll("Indic_Conjunct_Break") { cp -> expected[cp] to Codepoint(cp).getIndicConjunctBreak() }
        assertEquals(IndicConjunctBreak.NONE, Codepoint(-1).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.NONE, Codepoint(0x110000).getIndicConjunctBreak())
    }

    @Test
    fun emojiProperties() {
        val predicates: Map<String, (Codepoint) -> Boolean> = mapOf(
            "Extended_Pictographic" to { it.isExtendedPictographic() },
            "Emoji" to { it.isEmoji() },
            "Emoji_Presentation" to { it.isEmojiPresentation() },
            "Emoji_Modifier" to { it.isEmojiModifier() },
            "Emoji_Modifier_Base" to { it.isEmojiModifierBase() },
            "Emoji_Component" to { it.isEmojiComponent() },
        )
        val ranges = DataFiles.ranges("emoji-data.txt")
        for ((property, predicate) in predicates) {
            val expected = BooleanArray(0x110000)
            var listed = 0
            for ((range, fields) in ranges) {
                if (fields[0] != property) continue
                for (cp in range) expected[cp] = true
                listed += range.count()
            }
            check(listed > 0) { "$property is not in emoji-data.txt" }
            assertAll(property) { cp -> expected[cp] to predicate(Codepoint(cp)) }
            assertEquals(false, predicate(Codepoint(-1)), "$property of -1")
            assertEquals(false, predicate(Codepoint(0x110000)), "$property of 0x110000")
        }
    }

    private fun <T> assertAll(property: String, values: (Int) -> Pair<T, T>) {
        val mismatches = allCodepoints.mapNotNull { cp ->
            val (expected, actual) = values(cp)
            if (expected == actual) null else "U+%04X: expected %s, was %s".format(cp, expected, actual)
        }
        if (mismatches.isNotEmpty()) {
            fail("$property differs from the data for ${mismatches.size} code points:\n" + mismatches.take(20).joinToString("\n"))
        }
    }
}
