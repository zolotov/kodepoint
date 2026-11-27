package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.Codepoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spot checks that run on every target; the JVM test compares every code point with the data files. */
class PropertiesTest {
    @Test
    fun graphemeClusterBreak() {
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint('a'.code).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.CR, Codepoint(0x0D).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.LF, Codepoint(0x0A).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.CONTROL, Codepoint(0x00).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.EXTEND, Codepoint(0x0308).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.ZWJ, Codepoint(0x200D).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.REGIONAL_INDICATOR, Codepoint(0x1F1E6).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.PREPEND, Codepoint(0x0600).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.SPACING_MARK, Codepoint(0x0903).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.L, Codepoint(0x1100).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.V, Codepoint(0x1161).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.T, Codepoint(0x11A8).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.LV, Codepoint(0xAC00).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.LVT, Codepoint(0xAC01).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.EXTEND, Codepoint(0xE0067).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint(0xD800).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint(-1).getGraphemeClusterBreak())
        assertEquals(GraphemeClusterBreak.OTHER, Codepoint(0x110000).getGraphemeClusterBreak())
    }

    @Test
    fun indicConjunctBreak() {
        assertEquals(IndicConjunctBreak.LINKER, Codepoint(0x094D).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.LINKER, Codepoint(0x1CF5).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.CONSONANT, Codepoint(0x0915).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.EXTEND, Codepoint(0x0308).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.NONE, Codepoint('a'.code).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.NONE, Codepoint(-1).getIndicConjunctBreak())
        assertEquals(IndicConjunctBreak.NONE, Codepoint(0x110000).getIndicConjunctBreak())
    }

    @Test
    fun emojiProperties() {
        val grinning = Codepoint(0x1F600)
        assertTrue(grinning.isEmoji())
        assertTrue(grinning.isEmojiPresentation())
        assertTrue(grinning.isExtendedPictographic())
        assertFalse(grinning.isEmojiModifierBase())

        val digit = Codepoint('1'.code)
        assertTrue(digit.isEmoji())
        assertFalse(digit.isEmojiPresentation())
        assertTrue(digit.isEmojiComponent())
        assertFalse(digit.isExtendedPictographic())

        assertTrue(Codepoint(0x1F3FD).isEmojiModifier())
        assertTrue(Codepoint(0x1F44B).isEmojiModifierBase())
        assertTrue(Codepoint(0xA9).isExtendedPictographic())
        assertTrue(Codepoint(0x1FAFF).isExtendedPictographic()) // unassigned, reserved for pictographs
        assertFalse(Codepoint(0x1FAFF).isEmoji())

        for (outside in listOf(-1, 0x110000)) {
            val codepoint = Codepoint(outside)
            assertFalse(codepoint.isEmoji())
            assertFalse(codepoint.isEmojiPresentation())
            assertFalse(codepoint.isEmojiModifier())
            assertFalse(codepoint.isEmojiModifierBase())
            assertFalse(codepoint.isEmojiComponent())
            assertFalse(codepoint.isExtendedPictographic())
        }
    }
}
