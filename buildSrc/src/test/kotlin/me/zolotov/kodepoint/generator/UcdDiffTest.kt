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
        assertEquals(
            "# UCD diff: 16.0.0 -> 17.0.0\n# <property> <codepoint> <value in 16.0.0> <value in 17.0.0>\n",
            diff.toText()
        )
    }

    @Test
    fun newlyAssignedLetterShowsUpInEveryAffectedPropertyWithBothValues() {
        val newer = emptyUnicodeData()
        val cp = 0x1E5D0
        newer.characters[cp].apply {
            category = GeneralCategory.Lu
            lowerCase = 0x1E5D1
            isLetter = true
            isUpperCase = true
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
                "isLetter", "isLetterOrDigit", "isUpperCase", "toLowerCase", "isUnicodeIdentifierStart",
                "isUnicodeIdentifierPart", "isJavaIdentifierStart", "isJavaIdentifierPart", "getScript", "getCategory"
            ),
            affected.keys
        )
        assertTrue(affected.values.all { it.codepoint == cp })

        val lines = diff.toText().lines()
        assertTrue("isLetter 1E5D0 false true" in lines)
        assertTrue("toLowerCase 1E5D0 1E5D0 1E5D1" in lines)
        assertTrue("getCategory 1E5D0 Cn Lu" in lines)
        assertTrue("getScript 1E5D0 UNKNOWN OL_ONAL" in lines)
    }
}
