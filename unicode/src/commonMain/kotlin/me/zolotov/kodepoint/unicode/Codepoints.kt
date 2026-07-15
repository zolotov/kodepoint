package me.zolotov.kodepoint.unicode

import me.zolotov.kodepoint.category.Category
import me.zolotov.kodepoint.generated.CharacterData
import me.zolotov.kodepoint.generated.ScriptData
import me.zolotov.kodepoint.internal.asciiToLowerCase
import me.zolotov.kodepoint.internal.asciiToUpperCase
import me.zolotov.kodepoint.internal.binarySearchRange
import me.zolotov.kodepoint.internal.isAscii
import me.zolotov.kodepoint.script.UnicodeScript

/**
 * Unicode character property functions.
 */
object Codepoints {
    private const val ASCII_SPACE = 0x20

    // 128-bit ASCII membership bitmaps split into two Longs (codepoints 0..63 and 64..127).
    // A branchless bit test replaces chains of range checks whose branches are
    // data-dependent and mispredict on mixed ASCII input.
    private const val ASCII_ID_IGNORABLE_LO = 0x0FFFC1FFL // 0x00..0x08, 0x0E..0x1B
    private const val ASCII_ID_IGNORABLE_HI = Long.MIN_VALUE // 0x7F
    private const val ASCII_UNICODE_ID_PART_LO = 0x03FF000000000000L // 0-9
    private const val ASCII_UNICODE_ID_PART_HI = 0x07FFFFFE87FFFFFEL // A-Z, _, a-z
    private const val ASCII_JAVA_ID_START_LO = 0x0000001000000000L // $
    private const val ASCII_JAVA_ID_START_HI = 0x07FFFFFE87FFFFFEL // A-Z, _, a-z
    private const val ASCII_JAVA_ID_PART_LO = 0x03FF00100FFFC1FFL // 0-9, $, 0x00..0x08, 0x0E..0x1B
    private const val ASCII_JAVA_ID_PART_HI = -0x7800000178000002L // A-Z, _, a-z, 0x7F

    // Helper functions for property interpretation
    private fun getCategoryCode(props: Int): Int =
        (props and CharacterData.CATEGORY_MASK) ushr CharacterData.CATEGORY_SHIFT

    private fun getCaseDelta(props: Int): Int {
        // Decode signed 10-bit case delta from packed properties.
        // Values 0x000-0x1FF are positive (0 to 511), 0x200-0x3FF are negative (-512 to -1)
        val delta = props and CharacterData.CASE_DELTA_MASK
        return if (delta >= 0x200) delta - 0x400 else delta
    }

    private fun isDeltaToLowercase(props: Int): Boolean =
        (props and CharacterData.DELTA_TO_LOWERCASE_BIT) != 0

    private fun hasLargeLowercaseDelta(props: Int): Boolean =
        (props and CharacterData.HAS_LARGE_LOWERCASE_DELTA_BIT) != 0

    private fun hasLargeUppercaseDelta(props: Int): Boolean =
        (props and CharacterData.HAS_LARGE_UPPERCASE_DELTA_BIT) != 0

    private fun isAsciiUppercaseLetter(codepoint: Int): Boolean =
        (codepoint - 'A'.code).toUInt() <= 25u

    private fun isAsciiLowercaseLetter(codepoint: Int): Boolean =
        (codepoint - 'a'.code).toUInt() <= 25u

    private fun isAsciiLetter(codepoint: Int): Boolean =
        isAsciiUppercaseLetter(codepoint) || isAsciiLowercaseLetter(codepoint)

    private fun isAsciiDigit(codepoint: Int): Boolean =
        (codepoint - '0'.code).toUInt() <= 9u

    private fun isAsciiWhitespace(codepoint: Int): Boolean =
        codepoint == ASCII_SPACE || (codepoint - 0x09).toUInt() <= 4u

    // Long shifts use only the low 6 bits of the distance, so codepoints 64..127 index into `hi` directly.
    private fun asciiBitTest(lo: Long, hi: Long, codepoint: Int): Boolean =
        ((if (codepoint < 64) lo else hi) ushr codepoint) and 1L != 0L

    private fun isAsciiIdentifierIgnorable(codepoint: Int): Boolean =
        asciiBitTest(ASCII_ID_IGNORABLE_LO, ASCII_ID_IGNORABLE_HI, codepoint)

    private fun isAsciiUnicodeIdentifierPart(codepoint: Int): Boolean =
        asciiBitTest(ASCII_UNICODE_ID_PART_LO, ASCII_UNICODE_ID_PART_HI, codepoint)

    private fun isAsciiJavaIdentifierStart(codepoint: Int): Boolean =
        asciiBitTest(ASCII_JAVA_ID_START_LO, ASCII_JAVA_ID_START_HI, codepoint)

    private fun isAsciiJavaIdentifierPart(codepoint: Int): Boolean =
        asciiBitTest(ASCII_JAVA_ID_PART_LO, ASCII_JAVA_ID_PART_HI, codepoint)

    private fun getAsciiUnicodeScript(codepoint: Int): UnicodeScript =
        if (isAsciiLetter(codepoint)) UnicodeScript.LATIN else UnicodeScript.COMMON

    private fun isLetterSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_LETTER_BIT) != 0
    }

    private fun isDigitSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_DIGIT_BIT) != 0
    }

    private fun isLetterOrDigitSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and (CharacterData.IS_LETTER_BIT or CharacterData.IS_DIGIT_BIT)) != 0
    }

    private fun isUpperCaseSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_UPPERCASE_BIT) != 0
    }

    private fun isLowerCaseSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_LOWERCASE_BIT) != 0
    }

    private fun toLowerCaseSlow(codepoint: Int): Int {
        val props = CharacterData.getProperties(codepoint)

        if (hasLargeLowercaseDelta(props)) {
            return codepoint + binarySearchRange(codepoint, CharacterData.largeLowercaseRanges, 0)
        }

        val delta = getCaseDelta(props)
        return if (delta != 0 && isDeltaToLowercase(props)) {
            codepoint + delta
        } else {
            codepoint
        }
    }

    private fun toUpperCaseSlow(codepoint: Int): Int {
        val props = CharacterData.getProperties(codepoint)

        // Special handling for titlecase letters (Lt) - these map to their uppercase variants
        // U+01C5 Dž -> U+01C4 DŽ, U+01C8 Lj -> U+01C7 LJ
        // U+01CB Nj -> U+01CA NJ, U+01F2 Dz -> U+01F1 DZ
        if (getCategoryCode(props) == CharacterData.CAT_LT) {
            return when (codepoint) {
                0x01C5, 0x01C8, 0x01CB, 0x01F2 -> codepoint - 1
                else -> codepoint
            }
        }

        if (hasLargeUppercaseDelta(props)) {
            return codepoint + binarySearchRange(codepoint, CharacterData.largeUppercaseRanges, 0)
        }

        val delta = getCaseDelta(props)
        return if (delta != 0 && !isDeltaToLowercase(props)) {
            codepoint + delta
        } else {
            codepoint
        }
    }

    private fun isSpaceCharSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_SPACE_CHAR_BIT) != 0
    }

    private fun isWhitespaceSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_WHITESPACE_BIT) != 0
    }

    private fun isIdeographicSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_IDEOGRAPHIC_BIT) != 0
    }

    private fun isIdentifierIgnorableSlow(codepoint: Int): Boolean {
        if (codepoint in 0x7F..0x9F) {
            return true
        }
        // Also ignorable: Format characters (category Cf)
        val props = CharacterData.getProperties(codepoint)
        return getCategoryCode(props) == CharacterData.CAT_CF
    }

    private fun isUnicodeIdentifierStartSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_UNICODE_ID_START_BIT) != 0
    }

    private fun isUnicodeIdentifierPartSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_UNICODE_ID_PART_BIT) != 0
    }

    private fun isJavaIdentifierStartSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_JAVA_ID_START_BIT) != 0
    }

    private fun isJavaIdentifierPartSlow(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_JAVA_ID_PART_BIT) != 0
    }

    fun isLetter(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiLetter(codepoint) else isLetterSlow(codepoint)

    fun isDigit(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiDigit(codepoint) else isDigitSlow(codepoint)

    fun isLetterOrDigit(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiLetter(codepoint) || isAsciiDigit(codepoint) else isLetterOrDigitSlow(codepoint)

    fun isUpperCase(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiUppercaseLetter(codepoint) else isUpperCaseSlow(codepoint)

    fun isLowerCase(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiLowercaseLetter(codepoint) else isLowerCaseSlow(codepoint)

    fun toLowerCase(codepoint: Int): Int =
        if (isAscii(codepoint)) asciiToLowerCase(codepoint) else toLowerCaseSlow(codepoint)

    fun toUpperCase(codepoint: Int): Int =
        if (isAscii(codepoint)) asciiToUpperCase(codepoint) else toUpperCaseSlow(codepoint)

    fun isSpaceChar(codepoint: Int): Boolean =
        if (isAscii(codepoint)) codepoint == ASCII_SPACE else isSpaceCharSlow(codepoint)

    fun isWhitespace(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiWhitespace(codepoint) else isWhitespaceSlow(codepoint)

    fun isIdeographic(codepoint: Int): Boolean =
        if (isAscii(codepoint)) false else isIdeographicSlow(codepoint)

    fun isIdentifierIgnorable(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiIdentifierIgnorable(codepoint) else isIdentifierIgnorableSlow(codepoint)

    fun isUnicodeIdentifierStart(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiLetter(codepoint) else isUnicodeIdentifierStartSlow(codepoint)

    fun isUnicodeIdentifierPart(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiUnicodeIdentifierPart(codepoint) else isUnicodeIdentifierPartSlow(codepoint)

    fun isJavaIdentifierStart(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiJavaIdentifierStart(codepoint) else isJavaIdentifierStartSlow(codepoint)

    fun isJavaIdentifierPart(codepoint: Int): Boolean =
        if (isAscii(codepoint)) isAsciiJavaIdentifierPart(codepoint) else isJavaIdentifierPartSlow(codepoint)

    fun isISOControl(codepoint: Int): Boolean =
        // C0 control codes (U+0000..U+001F) and C1 control codes (U+007F..U+009F)
        codepoint in 0x00..0x1F || codepoint in 0x7F..0x9F

    fun getUnicodeScript(codepoint: Int): UnicodeScript =
        if (isAscii(codepoint)) getAsciiUnicodeScript(codepoint) else ScriptData.getScript(codepoint)

    fun getCategory(codepoint: Int): Category =
        // Category.ordinal matches the category codes packed into CharacterData.
        Category.entries[getCategoryCode(CharacterData.getProperties(codepoint))]
}
