package me.zolotov.kodepoint.generator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UcdDiffTest {
    private val v16 = UnicodeVersion("16.0.0")
    private val v17 = UnicodeVersion("17.0.0")

    private fun emptyUnicodeData() = UnicodeData(Array(MAX_CODEPOINT + 1) { CharacterData(it) }, emptyMap())

    @Test
    fun identicalDataProducesEmptyDiff() {
        val diff = computeUcdDiff(v16, emptyUnicodeData(), v17, emptyUnicodeData())
        assertTrue(diff.isEmpty)
        assertEquals("# UCD diff: 16.0.0 -> 17.0.0\n# <property> <codepoint>: values differ between the two releases\n", diff.toText())
    }

    @Test
    fun newlyAssignedLetterShowsUpInEveryAffectedProperty() {
        val newer = emptyUnicodeData()
        val cp = 0x1E5D0
        newer.characters[cp].apply {
            category = GeneralCategory.Lo
            isLetter = true
            isIdStart = true
            isIdContinue = true
            isJavaIdentifierStart = true
            isJavaIdentifierPart = true
        }
        val newerWithScript = UnicodeData(newer.characters, mapOf(cp to "Ol_Onal"))

        val diff = computeUcdDiff(v16, emptyUnicodeData(), v17, newerWithScript)

        val affected = diff.changes.filterValues { it.isNotEmpty() }.mapValues { it.value.single() }
        assertEquals(
            setOf(
                "isLetter", "isLetterOrDigit", "isUnicodeIdentifierStart", "isUnicodeIdentifierPart",
                "isJavaIdentifierStart", "isJavaIdentifierPart", "getScript", "getCategory"
            ),
            affected.keys
        )
        assertTrue(affected.values.all { it == cp })
        assertTrue(diff.toText().lines().contains("isLetter 1E5D0"))
    }
}
