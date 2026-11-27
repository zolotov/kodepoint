package me.zolotov.kodepoint.graphemes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EmojiTest {
    private fun emoji(text: String): List<Triple<Int, Int, EmojiSequenceKind>> =
        buildList { text.forEachEmoji { start, end, kind -> add(Triple(start, end, kind)) } }

    @Test
    fun kinds() {
        val examples = mapOf(
            "\uD83C\uDDFA\uD83C\uDDE6" to EmojiSequenceKind.FLAG,
            "#\uFE0F\u20E3" to EmojiSequenceKind.KEYCAP,
            "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F" to EmojiSequenceKind.TAG,
            "\uD83D\uDC4B\uD83C\uDFFD" to EmojiSequenceKind.MODIFIER,
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67" to EmojiSequenceKind.ZWJ,
            "\uD83D\uDE00" to EmojiSequenceKind.BASIC,
            "\u263A\uFE0F" to EmojiSequenceKind.BASIC,
        )
        for ((text, kind) in examples) {
            assertEquals(listOf(Triple(0, text.length, kind)), emoji(text), text)
            assertTrue(text.isEmojiSequence(), text)
        }
    }

    @Test
    fun emojiInText() {
        val text = "Hi \uD83D\uDC4B\uD83C\uDFFD, \u00E1 \uD83C\uDDFA\uD83C\uDDE6!"
        assertEquals(
            listOf(Triple(3, 7, EmojiSequenceKind.MODIFIER), Triple(11, 15, EmojiSequenceKind.FLAG)),
            emoji(text)
        )
        assertEquals(
            listOf(Triple(3, 7, EmojiSequenceKind.MODIFIER), Triple(12, 16, EmojiSequenceKind.FLAG)),
            emoji("Hi \uD83D\uDC4B\uD83C\uDFFD, a\u0301 \uD83C\uDDFA\uD83C\uDDE6!") // decomposed
        )
    }

    @Test
    fun notWellFormed() {
        assertEquals(emptyList(), emoji("abc"))
        assertEquals(emptyList(), emoji("\uD83D\uDE00\u0301")) // a mark after the emoji
        assertEquals(emptyList(), emoji("\u263A\uFE0E")) // text presentation selector
        assertEquals(emptyList(), emoji("1\u20E3")) // keycap without U+FE0F
        assertEquals(emptyList(), emoji("\uD83C\uDFF4\uDB40\uDC67")) // tag sequence without its end
        assertFalse("".isEmojiSequence())
        assertFalse("a".isEmojiSequence())
        assertFalse("\uD83D\uDE00\uD83D\uDE00".isEmojiSequence())
        assertFalse("\uD83D\uDE00\u200D".isEmojiSequence())
    }

    @Test
    fun textPresentationCharactersAreEmojiCharacters() {
        // UTS #51 ED-3: the digits, # and * are emoji characters; forEachEmoji follows the grammar.
        assertEquals(listOf(Triple(0, 1, EmojiSequenceKind.BASIC)), emoji("1"))
        assertFalse(Emoji.isRgi("1"))
    }

    @Test
    fun lookup() {
        val grinning = Emoji.lookup("\uD83D\uDE00")!!
        assertEquals("\uD83D\uDE00", grinning.fullyQualified)
        assertEquals(EmojiStatus.FULLY_QUALIFIED, grinning.status)
        assertEquals("grinning face", grinning.name)
        assertEquals(EmojiGroup.SMILEYS_AND_EMOTION, grinning.group)
        assertEquals("face-smiling", grinning.subgroup)
        assertEquals("1.0", grinning.version)
        assertSame(grinning, Emoji.lookup(StringBuilder("\uD83D\uDE00")))

        val unqualified = Emoji.lookup("\u263A")!!
        assertEquals(EmojiStatus.UNQUALIFIED, unqualified.status)
        assertEquals("\u263A\uFE0F", unqualified.fullyQualified)
        assertEquals("smiling face", unqualified.name)
        assertEquals(EmojiStatus.FULLY_QUALIFIED, Emoji.lookup("\u263A\uFE0F")!!.status)

        val component = Emoji.lookup("\uD83C\uDFFB")!!
        assertEquals(EmojiStatus.COMPONENT, component.status)
        assertEquals(EmojiGroup.COMPONENT, component.group)
        assertEquals("light skin tone", component.name)

        val flag = Emoji.lookup("\uD83C\uDDFA\uD83C\uDDE6")!!
        assertEquals("flag: Ukraine", flag.name)
        assertEquals(EmojiGroup.FLAGS, flag.group)

        assertNull(Emoji.lookup("a"))
        assertNull(Emoji.lookup(""))
        assertNull(Emoji.lookup("\uD83C\uDDE6\uD83C\uDDE6"))
        assertNull(Emoji.lookup("\uD83D\uDE00\uD83D\uDE00"))
    }

    @Test
    fun rgi() {
        assertTrue(Emoji.isRgi("\uD83D\uDE00"))
        assertTrue(Emoji.isRgi("\u263A\uFE0F"))
        assertFalse(Emoji.isRgi("\u263A"))
        assertTrue(Emoji.isRgi("\uD83C\uDFFB"))
        assertFalse(Emoji.isRgi("\uD83C\uDDE6\uD83C\uDDE6"))
        assertFalse(Emoji.isRgi("a"))
    }

    @Test
    fun all() {
        val all = Emoji.all()
        assertEquals(3972, all.size)
        assertEquals(3963, all.count { it.status == EmojiStatus.FULLY_QUALIFIED })
        assertEquals(9, all.count { it.status == EmojiStatus.COMPONENT })
        assertEquals("grinning face", all.first().name)
        assertEquals("flag: Wales", all.last().name)
        assertEquals(EmojiGroup.entries.toSet(), all.map { it.group }.toSet())
        for (info in all) assertSame(info, Emoji.lookup(info.fullyQualified), info.name)
        assertEquals("18.0", Emoji.VERSION)
    }
}
