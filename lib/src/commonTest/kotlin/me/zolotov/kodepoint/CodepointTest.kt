package me.zolotov.kodepoint

import me.zolotov.kodepoint.script.UnicodeScript
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodepointTest {

    @Test
    fun testIsLetter() {
        assertTrue(Codepoint('A'.code).isLetter())
        assertTrue(Codepoint('a'.code).isLetter())
        assertFalse(Codepoint('0'.code).isLetter())
        assertFalse(Codepoint(' '.code).isLetter())
    }

    @Test
    fun testIsDigit() {
        assertTrue(Codepoint('0'.code).isDigit())
        assertTrue(Codepoint('9'.code).isDigit())
        assertFalse(Codepoint('A'.code).isDigit())
        assertFalse(Codepoint(' '.code).isDigit())
    }

    @Test
    fun testIsLetterOrDigit() {
        assertTrue(Codepoint('A'.code).isLetterOrDigit())
        assertTrue(Codepoint('0'.code).isLetterOrDigit())
        assertFalse(Codepoint(' '.code).isLetterOrDigit())
        assertFalse(Codepoint('!'.code).isLetterOrDigit())
    }

    @Test
    fun testIsUpperCase() {
        assertTrue(Codepoint('A'.code).isUpperCase())
        assertFalse(Codepoint('a'.code).isUpperCase())
        assertFalse(Codepoint('0'.code).isUpperCase())
    }

    @Test
    fun testIsLowerCase() {
        assertTrue(Codepoint('a'.code).isLowerCase())
        assertFalse(Codepoint('A'.code).isLowerCase())
        assertFalse(Codepoint('0'.code).isLowerCase())
    }

    @Test
    fun testToLowerCase() {
        assertEquals('a'.code, Codepoint('A'.code).toLowerCase().codepoint)
        assertEquals('a'.code, Codepoint('a'.code).toLowerCase().codepoint)
        assertEquals('0'.code, Codepoint('0'.code).toLowerCase().codepoint)
    }

    @Test
    fun testToUpperCase() {
        assertEquals('A'.code, Codepoint('a'.code).toUpperCase().codepoint)
        assertEquals('A'.code, Codepoint('A'.code).toUpperCase().codepoint)
        assertEquals('0'.code, Codepoint('0'.code).toUpperCase().codepoint)
    }

    @Test
    fun testIsWhitespace() {
        assertTrue(Codepoint(' '.code).isWhitespace())
        assertTrue(Codepoint('\t'.code).isWhitespace())
        assertTrue(Codepoint('\n'.code).isWhitespace())
        assertTrue(Codepoint('\r'.code).isWhitespace())
        assertFalse(Codepoint('A'.code).isWhitespace())
    }

    @Test
    fun testIsSpaceChar() {
        assertTrue(Codepoint(' '.code).isSpaceChar())
        assertFalse(Codepoint('\t'.code).isSpaceChar())
        assertFalse(Codepoint('A'.code).isSpaceChar())
    }

    @Test
    fun testIsUnicodeIdentifierStart() {
        assertTrue(Codepoint('A'.code).isUnicodeIdentifierStart())
        assertTrue(Codepoint('a'.code).isUnicodeIdentifierStart())
        assertFalse(Codepoint('0'.code).isUnicodeIdentifierStart())
        assertFalse(Codepoint(' '.code).isUnicodeIdentifierStart())
    }

    @Test
    fun testIsUnicodeIdentifierPart() {
        assertTrue(Codepoint('A'.code).isUnicodeIdentifierPart())
        assertTrue(Codepoint('a'.code).isUnicodeIdentifierPart())
        assertTrue(Codepoint('0'.code).isUnicodeIdentifierPart())
        assertFalse(Codepoint(' '.code).isUnicodeIdentifierPart())
    }

    @Test
    fun testIsJavaIdentifierStart() {
        assertTrue(Codepoint('A'.code).isJavaIdentifierStart())
        assertTrue(Codepoint('a'.code).isJavaIdentifierStart())
        assertFalse(Codepoint('0'.code).isJavaIdentifierStart())
        assertFalse(Codepoint(' '.code).isJavaIdentifierStart())
    }

    @Test
    fun testIsJavaIdentifierPart() {
        assertTrue(Codepoint('A'.code).isJavaIdentifierPart())
        assertTrue(Codepoint('a'.code).isJavaIdentifierPart())
        assertTrue(Codepoint('0'.code).isJavaIdentifierPart())
        assertFalse(Codepoint(' '.code).isJavaIdentifierPart())
    }

    @Test
    fun testScriptProperties() {
        // Latin characters
        assertEquals(UnicodeScript.LATIN, Codepoint('A'.code).getUnicodeScript())
        assertEquals(UnicodeScript.LATIN, Codepoint('z'.code).getUnicodeScript())

        // Common (punctuation, etc.)
        assertEquals(UnicodeScript.COMMON, Codepoint(' '.code).getUnicodeScript())
        assertEquals(UnicodeScript.COMMON, Codepoint('0'.code).getUnicodeScript())

        // Japanese
        assertEquals(UnicodeScript.HIRAGANA, Codepoint('あ'.code).getUnicodeScript())
        assertEquals(UnicodeScript.KATAKANA, Codepoint('ア'.code).getUnicodeScript())
        assertEquals(UnicodeScript.HAN, Codepoint('漢'.code).getUnicodeScript())

        // Greek
        assertEquals(UnicodeScript.GREEK, Codepoint('α'.code).getUnicodeScript())
        assertEquals(UnicodeScript.GREEK, Codepoint('Ω'.code).getUnicodeScript())

        // Cyrillic
        assertEquals(UnicodeScript.CYRILLIC, Codepoint('А'.code).getUnicodeScript())
        assertEquals(UnicodeScript.CYRILLIC, Codepoint('я'.code).getUnicodeScript())

        // Arabic
        assertEquals(UnicodeScript.ARABIC, Codepoint('ا'.code).getUnicodeScript())

        // Other properties
        assertFalse(Codepoint('A'.code).isIdeographic())
        assertTrue(Codepoint('漢'.code).isIdeographic())
        assertFalse(Codepoint('A'.code).isIdentifierIgnorable())
    }

    @Test
    fun testAsString() {
        assertEquals("A", Codepoint('A'.code).asString())
        assertEquals("0", Codepoint('0'.code).asString())
        assertEquals(" ", Codepoint(' '.code).asString())
    }

    @Test
    fun testInvalidCodePoints() {
        val invalidCp = Codepoint(-1)
        assertFalse(invalidCp.isLetter())
        assertFalse(invalidCp.isDigit())
        assertFalse(invalidCp.isUpperCase())
        assertFalse(invalidCp.isLowerCase())
    }

    @Test
    fun testSurrogateHandling() {
        val highSurrogate = Codepoint(0xD800)
        val lowSurrogate = Codepoint(0xDC00)

        assertFalse(highSurrogate.isLetter())
        assertFalse(lowSurrogate.isLetter())
    }

    @Test
    fun testFullAlphabetCaseConversion() {
        for (c in 'A'..'Z') {
            val upperCp = Codepoint(c.code)
            val lowerCp = upperCp.toLowerCase()

            assertTrue(upperCp.isUpperCase())
            assertFalse(upperCp.isLowerCase())
            assertTrue(upperCp.isLetter())

            assertTrue(lowerCp.isLowerCase())
            assertFalse(lowerCp.isUpperCase())
            assertTrue(lowerCp.isLetter())

            assertEquals(c.lowercaseChar().code, lowerCp.codepoint)
            assertEquals(c.code, lowerCp.toUpperCase().codepoint)
        }

        for (c in 'a'..'z') {
            val lowerCp = Codepoint(c.code)
            val upperCp = lowerCp.toUpperCase()

            assertTrue(lowerCp.isLowerCase())
            assertFalse(lowerCp.isUpperCase())
            assertTrue(lowerCp.isLetter())

            assertTrue(upperCp.isUpperCase())
            assertFalse(upperCp.isLowerCase())
            assertTrue(upperCp.isLetter())

            assertEquals(c.uppercaseChar().code, upperCp.codepoint)
            assertEquals(c.code, upperCp.toLowerCase().codepoint)
        }
    }
}