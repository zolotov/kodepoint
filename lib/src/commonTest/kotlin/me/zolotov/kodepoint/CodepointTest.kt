package me.zolotov.kodepoint

import me.zolotov.kodepoint.eaw.EastAsianWidth
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
    fun testCharCountBmp() {
        // BMP characters (U+0000 to U+FFFF) have charCount of 1
        assertEquals(1, Codepoint(0x0000).charCount)
        assertEquals(1, Codepoint('A'.code).charCount)
        assertEquals(1, Codepoint(0x00E9).charCount) // é
        assertEquals(1, Codepoint(0x4E2D).charCount) // 中
        assertEquals(1, Codepoint(0xFFFF).charCount) // Last BMP codepoint
    }

    @Test
    fun testCharCountSupplementary() {
        // Supplementary characters (U+10000 and above) have charCount of 2
        assertEquals(2, Codepoint(0x10000).charCount) // First supplementary
        assertEquals(2, Codepoint(0x1F600).charCount) // 😀
        assertEquals(2, Codepoint(0x1F44D).charCount) // 👍
        assertEquals(2, Codepoint(0x10FFFF).charCount) // Maximum codepoint
    }

    @Test
    fun testCharCountPlaneBoundaries() {
        // Test boundary between BMP and supplementary planes
        assertEquals(1, Codepoint(0xFFFF).charCount)   // Last BMP
        assertEquals(2, Codepoint(0x10000).charCount)  // First supplementary (SMP)
        assertEquals(2, Codepoint(0x1FFFF).charCount)  // Last SMP
        assertEquals(2, Codepoint(0x20000).charCount)  // First SIP
    }

    @Test
    fun testFromCharsBasic() {
        // U+1F600 (😀) = D83D DE00
        val cp = Codepoint.fromChars('\uD83D', '\uDE00')
        assertEquals(0x1F600, cp.codepoint)
    }

    @Test
    fun testFromCharsThumbsUp() {
        // U+1F44D (👍) = D83D DC4D
        val cp = Codepoint.fromChars('\uD83D', '\uDC4D')
        assertEquals(0x1F44D, cp.codepoint)
    }

    @Test
    fun testFromCharsFirstSupplementary() {
        // U+10000 = D800 DC00
        val cp = Codepoint.fromChars('\uD800', '\uDC00')
        assertEquals(0x10000, cp.codepoint)
    }

    @Test
    fun testFromCharsLastSupplementary() {
        // U+10FFFF = DBFF DFFF
        val cp = Codepoint.fromChars('\uDBFF', '\uDFFF')
        assertEquals(0x10FFFF, cp.codepoint)
    }

    @Test
    fun testFromCharsRoundTrip() {
        // Verify that asString and fromChars are consistent
        val originalCp = Codepoint(0x1F600)
        val str = originalCp.asString()
        assertEquals(2, str.length)
        val reconstructed = Codepoint.fromChars(str[0], str[1])
        assertEquals(originalCp.codepoint, reconstructed.codepoint)
    }

    @Test
    fun testSupplementaryCharacterIsLetter() {
        // Mathematical Bold Capital A (U+1D400) - is a letter
        assertTrue(Codepoint(0x1D400).isLetter())
        // Mathematical Bold Small A (U+1D41A)
        assertTrue(Codepoint(0x1D41A).isLetter())
        // Emoji (U+1F600) - not a letter
        assertFalse(Codepoint(0x1F600).isLetter())
    }

    @Test
    fun testSupplementaryCharacterCaseConversion() {
        // Deseret Capital Letter Long I (U+10400) -> Deseret Small Letter Long I (U+10428)
        val deseretUppercase = Codepoint(0x10400)
        val deseretLowercase = Codepoint(0x10428)
        assertTrue(deseretUppercase.isUpperCase())
        assertTrue(deseretLowercase.isLowerCase())
        assertEquals(0x10428, deseretUppercase.toLowerCase().codepoint)
        assertEquals(0x10400, deseretLowercase.toUpperCase().codepoint)
    }

    @Test
    fun testCjkIdeograph() {
        // CJK Unified Ideograph Extension B (U+20000)
        val cjkExtB = Codepoint(0x20000)
        assertTrue(cjkExtB.isLetter())
        assertTrue(cjkExtB.isIdeographic())
        assertEquals(UnicodeScript.HAN, cjkExtB.getUnicodeScript())
    }

    @Test
    fun testEmojiProperties() {
        val grinningFace = Codepoint(0x1F600)
        assertFalse(grinningFace.isLetter())
        assertFalse(grinningFace.isDigit())
        assertFalse(grinningFace.isWhitespace())
        assertFalse(grinningFace.isIdeographic())
        assertEquals(UnicodeScript.COMMON, grinningFace.getUnicodeScript())
    }

    @Test
    fun testBmpBoundary() {
        val lastBmp = Codepoint(0xFFFF)
        val firstSupplementary = Codepoint(0x10000)

        assertEquals(1, lastBmp.charCount)
        assertEquals(2, firstSupplementary.charCount)
    }

    @Test
    fun testSmpBoundary() {
        // Last codepoint of SMP (Supplementary Multilingual Plane)
        val lastSmp = Codepoint(0x1FFFF)
        // First codepoint of SIP (Supplementary Ideographic Plane)
        val firstSip = Codepoint(0x20000)

        assertEquals(2, lastSmp.charCount)
        assertEquals(2, firstSip.charCount)
    }

    @Test
    fun testMaxCodepoint() {
        val maxCp = Codepoint(0x10FFFF)
        assertEquals(2, maxCp.charCount)
        // Should not throw or crash
        assertFalse(maxCp.isLetter())
    }

    @Test
    fun testAsStringSupplementary() {
        val cp = Codepoint(0x1F600)
        val str = cp.asString()
        assertEquals(2, str.length)
        assertEquals('\uD83D', str[0])
        assertEquals('\uDE00', str[1])
    }

    @Test
    fun testAsStringMaxCodepoint() {
        val cp = Codepoint(0x10FFFF)
        val str = cp.asString()
        assertEquals(2, str.length)
        assertEquals('\uDBFF', str[0])
        assertEquals('\uDFFF', str[1])
    }

    @Test
    fun testToStringFormat() {
        assertEquals("Codepoint(0x41)", Codepoint('A'.code).toString())
        assertEquals("Codepoint(0x1F600)", Codepoint(0x1F600).toString())
        assertEquals("Codepoint(0x10FFFF)", Codepoint(0x10FFFF).toString())
    }

    @Test
    fun testToStringLowercaseHexIsUppercase() {
        // Verify hex digits are uppercase
        val cp = Codepoint(0xABCD)
        assertTrue(cp.toString().contains("ABCD"))
        assertFalse(cp.toString().contains("abcd"))
    }

    @Test
    fun testGetEastAsianWidth() {
        // Narrow: ASCII letters and digits
        assertEquals(EastAsianWidth.NARROW, Codepoint('A'.code).getEastAsianWidth())
        assertEquals(EastAsianWidth.NARROW, Codepoint('z'.code).getEastAsianWidth())
        assertEquals(EastAsianWidth.NARROW, Codepoint('0'.code).getEastAsianWidth())

        // Wide: CJK unified ideographs
        assertEquals(EastAsianWidth.WIDE, Codepoint('中'.code).getEastAsianWidth())  // U+4E2D
        assertEquals(EastAsianWidth.WIDE, Codepoint('漢'.code).getEastAsianWidth())  // U+6F22
        assertEquals(EastAsianWidth.WIDE, Codepoint('あ'.code).getEastAsianWidth())  // U+3042 Hiragana
        assertEquals(EastAsianWidth.WIDE, Codepoint('ア'.code).getEastAsianWidth())  // U+30A2 Katakana
        assertEquals(EastAsianWidth.WIDE, Codepoint('한'.code).getEastAsianWidth())  // U+D55C Hangul

        // Neutral: Latin-1 control characters and currency symbols without East Asian width
        assertEquals(EastAsianWidth.NEUTRAL, Codepoint(0x0000).getEastAsianWidth())  // NUL

        // Ambiguous: Greek and Cyrillic letters (context-dependent)
        assertEquals(EastAsianWidth.AMBIGUOUS, Codepoint('α'.code).getEastAsianWidth())  // U+03B1 Greek
        assertEquals(EastAsianWidth.AMBIGUOUS, Codepoint('А'.code).getEastAsianWidth())  // U+0410 Cyrillic

        // Fullwidth: Fullwidth Latin capital letters (U+FF01..U+FF60)
        assertEquals(EastAsianWidth.FULLWIDTH, Codepoint(0xFF21).getEastAsianWidth())  // FULLWIDTH LATIN CAPITAL A
        assertEquals(EastAsianWidth.FULLWIDTH, Codepoint(0xFF01).getEastAsianWidth())  // FULLWIDTH EXCLAMATION MARK

        // Halfwidth: Halfwidth Katakana (U+FF65..U+FF9F)
        assertEquals(EastAsianWidth.HALFWIDTH, Codepoint(0xFF65).getEastAsianWidth())  // HALFWIDTH KATAKANA MIDDLE DOT
        assertEquals(EastAsianWidth.HALFWIDTH, Codepoint(0xFF71).getEastAsianWidth())  // HALFWIDTH KATAKANA LETTER A

        // columns property
        assertEquals(1, Codepoint('A'.code).getEastAsianWidth().columns)
        assertEquals(2, Codepoint('中'.code).getEastAsianWidth().columns)
        assertEquals(2, Codepoint(0xFF21).getEastAsianWidth().columns)  // FULLWIDTH
        assertEquals(1, Codepoint(0xFF65).getEastAsianWidth().columns)  // HALFWIDTH

        // Supplementary plane: CJK Extension B (U+20000) — Wide
        assertEquals(EastAsianWidth.WIDE, Codepoint(0x20000).getEastAsianWidth())
        // Supplementary plane: emoji (U+1F600 😀) — Wide
        assertEquals(EastAsianWidth.WIDE, Codepoint(0x1F600).getEastAsianWidth())
    }

    @Test
    fun testIsISOControl() {
        // C0 control codes (U+0000..U+001F)
        assertTrue(Codepoint(0x0000).isISOControl()) // NUL
        assertTrue(Codepoint(0x001F).isISOControl()) // Unit Separator
        assertFalse(Codepoint(0x0020).isISOControl()) // Space - not a control

        // C1 control codes (U+007F..U+009F)
        assertTrue(Codepoint(0x007F).isISOControl()) // DEL
        assertTrue(Codepoint(0x009F).isISOControl()) // APC
        assertFalse(Codepoint(0x00A0).isISOControl()) // NBSP - not a control
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