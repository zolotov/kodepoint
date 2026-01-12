package me.zolotov.kodepoint.unicode

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

    // Pre-computed masks for efficient multi-category checks
    private const val LETTER_MASK = (1 shl CharacterData.CAT_LU) or (1 shl CharacterData.CAT_LL) or
        (1 shl CharacterData.CAT_LT) or (1 shl CharacterData.CAT_LM) or (1 shl CharacterData.CAT_LO)
    private const val LETTER_OR_DIGIT_MASK = LETTER_MASK or (1 shl CharacterData.CAT_ND)
    private const val SPACE_CHAR_MASK = (1 shl CharacterData.CAT_ZS) or (1 shl CharacterData.CAT_ZL) or
        (1 shl CharacterData.CAT_ZP)

    // Helper functions for property interpretation
    private fun getCategory(props: Int): Int =
        (props and CharacterData.CATEGORY_MASK) ushr CharacterData.CATEGORY_SHIFT

    private fun getCaseDelta(props: Int): Int {
        val delta = props and CharacterData.CASE_DELTA_MASK
        return if (delta >= 0x200) delta - 0x400 else delta
    }

    private fun isDeltaToLowercase(props: Int): Boolean =
        (props and CharacterData.DELTA_TO_LOWERCASE_BIT) != 0

    private fun hasLargeLowercaseDelta(props: Int): Boolean =
        (props and CharacterData.HAS_LARGE_LOWERCASE_DELTA_BIT) != 0

    private fun hasLargeUppercaseDelta(props: Int): Boolean =
        (props and CharacterData.HAS_LARGE_UPPERCASE_DELTA_BIT) != 0

    fun isLetter(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return ((1 shl getCategory(props)) and LETTER_MASK) != 0
    }

    fun isDigit(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return getCategory(props) == CharacterData.CAT_ND
    }

    fun isLetterOrDigit(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return ((1 shl getCategory(props)) and LETTER_OR_DIGIT_MASK) != 0
    }

    fun isUpperCase(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return getCategory(props) == CharacterData.CAT_LU ||
            (props and CharacterData.IS_OTHER_UPPERCASE_BIT) != 0
    }

    fun isLowerCase(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return getCategory(props) == CharacterData.CAT_LL ||
            (props and CharacterData.IS_OTHER_LOWERCASE_BIT) != 0
    }

    fun toLowerCase(codepoint: Int): Int {
        if (isAscii(codepoint)) {
            return asciiToLowerCase(codepoint)
        }

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

    fun toUpperCase(codepoint: Int): Int {
        if (isAscii(codepoint)) {
            return asciiToUpperCase(codepoint)
        }

        val props = CharacterData.getProperties(codepoint)

        // Special handling for titlecase letters (Lt)
        if (getCategory(props) == CharacterData.CAT_LT) {
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

    fun isSpaceChar(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return ((1 shl getCategory(props)) and SPACE_CHAR_MASK) != 0
    }

    fun isWhitespace(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_WHITESPACE_BIT) != 0
    }

    fun isIdeographic(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_IDEOGRAPHIC_BIT) != 0
    }

    fun isIdentifierIgnorable(codepoint: Int): Boolean {
        if (codepoint <= 0x08 || (codepoint in 0x0E..0x1B) || (codepoint in 0x7F..0x9F)) {
            return true
        }
        val props = CharacterData.getProperties(codepoint)
        return getCategory(props) == CharacterData.CAT_CF
    }

    fun isUnicodeIdentifierStart(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_UNICODE_ID_START_BIT) != 0
    }

    fun isUnicodeIdentifierPart(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_UNICODE_ID_PART_BIT) != 0
    }

    fun isJavaIdentifierStart(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_JAVA_ID_START_BIT) != 0
    }

    fun isJavaIdentifierPart(codepoint: Int): Boolean {
        val props = CharacterData.getProperties(codepoint)
        return (props and CharacterData.IS_JAVA_ID_PART_BIT) != 0
    }

    fun isISOControl(codepoint: Int): Boolean =
        codepoint in 0x00..0x1F || codepoint in 0x7F..0x9F

    fun getUnicodeScript(codepoint: Int): UnicodeScript = ScriptData.getScript(codepoint)
}
