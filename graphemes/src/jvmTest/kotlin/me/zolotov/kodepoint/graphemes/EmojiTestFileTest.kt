package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.appendCodePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Checks [Emoji] and the emoji grammar against every line of `emoji-test.txt`. */
class EmojiTestFileTest {
    private class Line(
        val codepoints: List<Int>,
        val status: EmojiStatus,
        val name: String,
        val version: String,
        val group: String,
        val subgroup: String,
        /** The fully-qualified (or component) line this line follows in the file. */
        val fullyQualified: String,
    ) {
        val text = buildString { codepoints.forEach { appendCodePoint(it) } }
    }

    private val lines: List<Line> = run {
        var group = ""
        var subgroup = ""
        var fullyQualified = ""
        DataFiles.lines("emoji-test.txt").mapNotNull { line ->
            when {
                line.startsWith("# group:") -> group = line.substringAfter(':').trim()
                line.startsWith("# subgroup:") -> subgroup = line.substringAfter(':').trim()
                line.isBlank() || line.startsWith("#") -> Unit
                else -> {
                    val codepoints = line.substringBefore(';').trim().split(' ').filter { it.isNotEmpty() }.map { it.toInt(16) }
                    val status = EmojiStatus.valueOf(line.substringAfter(';').substringBefore('#').trim().uppercase().replace('-', '_'))
                    // "# 😀 E1.0 grinning face"
                    val comment = line.substringAfter('#').trim()
                    val afterEmoji = comment.substringAfter(' ')
                    val version = afterEmoji.substringBefore(' ').removePrefix("E")
                    val name = afterEmoji.substringAfter(' ')
                    val text = buildString { codepoints.forEach { appendCodePoint(it) } }
                    if (status == EmojiStatus.FULLY_QUALIFIED || status == EmojiStatus.COMPONENT) fullyQualified = text
                    return@mapNotNull Line(codepoints, status, name, version, group, subgroup, fullyQualified)
                }
            }
            null
        }
    }

    @Test
    fun statusCounts() {
        assertEquals(5244, lines.size)
        val counts = lines.groupingBy { it.status }.eachCount()
        assertEquals(3963, counts[EmojiStatus.FULLY_QUALIFIED])
        assertEquals(1029, counts[EmojiStatus.MINIMALLY_QUALIFIED])
        assertEquals(243, counts[EmojiStatus.UNQUALIFIED])
        assertEquals(9, counts[EmojiStatus.COMPONENT])
    }

    @Test
    fun lookupEveryLine() {
        for (line in lines) {
            val info = assertNotNull(Emoji.lookup(line.text), line.name)
            assertEquals(line.status, info.status, line.name)
            assertEquals(line.name, info.name)
            assertEquals(line.group, info.group.label, line.name)
            assertEquals(line.subgroup, info.subgroup, line.name)
            assertEquals(line.version, info.version, line.name)
            assertEquals(line.fullyQualified, info.fullyQualified, line.name)
        }
    }

    @Test
    fun allInFileOrder() {
        val expected = lines.filter { it.status == EmojiStatus.FULLY_QUALIFIED || it.status == EmojiStatus.COMPONENT }
        assertEquals(3972, expected.size)
        assertEquals(expected.map { it.text }, Emoji.all().map { it.fullyQualified })
        assertEquals(expected.map { it.status }, Emoji.all().map { it.status })
        assertEquals(lines.map { it.group }.distinct(), EmojiGroup.entries.map { it.label })
    }

    @Test
    fun rgiSet() {
        for (line in lines) {
            val rgi = line.status == EmojiStatus.FULLY_QUALIFIED || line.status == EmojiStatus.COMPONENT
            assertEquals(rgi, Emoji.isRgi(line.text), "${line.status} ${line.name}")
        }
        // Pairs of regional indicators that name no region are flags by the grammar but not RGI.
        val rgiFlags = lines.filter { it.codepoints.size == 2 && it.codepoints.all { cp -> cp in 0x1F1E6..0x1F1FF } }
            .map { it.text }.toSet()
        var nonRgiFlags = 0
        for (first in 0x1F1E6..0x1F1FF) for (second in 0x1F1E6..0x1F1FF) {
            val flag = buildString { appendCodePoint(first); appendCodePoint(second) }
            if (flag in rgiFlags) continue
            nonRgiFlags++
            assertFalse(Emoji.isRgi(flag))
            assertEquals(null, Emoji.lookup(flag))
            assertTrue(flag.isEmojiSequence())
        }
        assertTrue(nonRgiFlags > 0)
    }

    @Test
    fun everyFullyQualifiedEmojiIsOneClusterOfTheRightKind() {
        for (line in lines.filter { it.status == EmojiStatus.FULLY_QUALIFIED || it.status == EmojiStatus.COMPONENT }) {
            val text = line.text
            assertEquals(1, text.graphemeClusterCount(), line.name)
            val reported = buildList { text.forEachEmoji { start, end, kind -> add(Triple(start, end, kind)) } }
            assertEquals(listOf(Triple(0, text.length, expectedKind(line.codepoints))), reported, line.name)
            assertTrue(text.isEmojiSequence(), line.name)
        }
    }

    @Test
    fun kindExamples() {
        val examples = mapOf(
            listOf(0x1F1FA, 0x1F1E6) to EmojiSequenceKind.FLAG,
            listOf(0x0023, 0xFE0F, 0x20E3) to EmojiSequenceKind.KEYCAP,
            listOf(0x1F3F4, 0xE0067, 0xE0062, 0xE0065, 0xE006E, 0xE0067, 0xE007F) to EmojiSequenceKind.TAG,
            listOf(0x1F44B, 0x1F3FD) to EmojiSequenceKind.MODIFIER,
            listOf(0x1F468, 0x200D, 0x1F469, 0x200D, 0x1F467) to EmojiSequenceKind.ZWJ,
            listOf(0x1F600) to EmojiSequenceKind.BASIC,
        )
        for ((codepoints, kind) in examples) {
            val line = lines.single { it.codepoints == codepoints }
            assertEquals(EmojiStatus.FULLY_QUALIFIED, line.status)
            val text = line.text
            val reported = buildList { text.forEachEmoji { start, end, k -> add(Triple(start, end, k)) } }
            assertEquals(listOf(Triple(0, text.length, kind)), reported)
        }
    }

    /** The kind by UTS #51, decided from the code points alone. */
    private fun expectedKind(codepoints: List<Int>): EmojiSequenceKind = when {
        0x200D in codepoints -> EmojiSequenceKind.ZWJ
        codepoints.any { it in 0xE0020..0xE007F } -> EmojiSequenceKind.TAG
        codepoints.size == 2 && codepoints.all { it in 0x1F1E6..0x1F1FF } -> EmojiSequenceKind.FLAG
        codepoints.lastOrNull() == 0x20E3 -> EmojiSequenceKind.KEYCAP
        codepoints.size == 2 && codepoints[1] in 0x1F3FB..0x1F3FF -> EmojiSequenceKind.MODIFIER
        else -> EmojiSequenceKind.BASIC
    }
}
