package me.zolotov.kodepoint.graphemes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The examples of the README section "Grapheme clusters & emoji", so the documented results stay true. */
class ReadmeExamplesTest {
    // "Hi 👋🏽 👨‍👩‍👧 🇺🇦"
    private val text = "Hi 👋🏽 👨‍👩‍👧 🇺🇦"

    @Test
    fun segmentation() {
        assertEquals(21, text.length)
        assertEquals(8, text.graphemeClusterCount())
        assertEquals(
            listOf("H", "i", " ", "👋🏽", " ", "👨‍👩‍👧", " ", "🇺🇦"),
            text.graphemeClusters().toList()
        )
        assertEquals(7, text.nextGraphemeClusterBoundary(3))
        assertEquals(3, text.previousGraphemeClusterBoundary(7))
        assertFalse(text.isGraphemeClusterBoundary(4))
    }

    @Test
    fun emoji() {
        val found = buildList { text.forEachEmoji { start, end, kind -> add("$start..<$end $kind") } }
        assertEquals(listOf("3..<7 MODIFIER", "8..<16 ZWJ", "17..<21 FLAG"), found)
        assertEquals("waving hand: medium skin tone", Emoji.lookup("👋🏽")?.name)
        assertEquals("☺️", Emoji.lookup("☺")?.fullyQualified)
        assertEquals(EmojiStatus.UNQUALIFIED, Emoji.lookup("☺")?.status)
        assertFalse(Emoji.isRgi("🇦🇦"))
    }
}
