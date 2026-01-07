package me.zolotov.kodepoint.unicode

import me.zolotov.kodepoint.generated.CharacterData
import me.zolotov.kodepoint.generated.ScriptData
import me.zolotov.kodepoint.internal.asciiToLowerCase
import me.zolotov.kodepoint.internal.asciiToUpperCase
import me.zolotov.kodepoint.internal.isAscii
import me.zolotov.kodepoint.script.UnicodeScript

object Codepoints {
    fun isLetter(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLetter(properties)
    }

    fun isDigit(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isDigit(properties)
    }

    fun isLetterOrDigit(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLetterOrDigit(properties)
    }

    fun isUpperCase(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUpperCase(properties)
    }

    fun isLowerCase(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLowerCase(properties)
    }

    fun toLowerCase(codepoint: Int): Int {
        if (isAscii(codepoint)) {
            return asciiToLowerCase(codepoint)
        }

        val properties = CharacterData.getProperties(codepoint)

        // Check for large delta flag first - avoids redundant isUpperCase check
        if (CharacterData.hasLargeLowercaseDelta(properties)) {
            return CharacterData.getLargeLowercase(codepoint)
        }

        val delta = CharacterData.getCaseDelta(properties)
        // If we have a non-zero delta with toLowerCase direction, use it
        return if (delta != 0 && CharacterData.isDeltaToLowercase(properties)) {
            codepoint + delta
        } else {
            codepoint
        }
    }

    fun toUpperCase(codepoint: Int): Int {
        if (isAscii(codepoint)) {
            return asciiToUpperCase(codepoint)
        }

        val properties = CharacterData.getProperties(codepoint)

        // Special handling for titlecase letters (Lt)
        if (CharacterData.isTitleCase(properties)) {
            // Latin Extended-B titlecase letters: 01C5, 01C8, 01CB, 01F2
            return when (codepoint) {
                0x01C5, 0x01C8, 0x01CB, 0x01F2 -> codepoint - 1
                else -> codepoint // Greek titlecase letters have no uppercase
            }
        }

        if (CharacterData.hasLargeUppercaseDelta(properties)) {
            return CharacterData.getLargeUppercase(codepoint)
        }

        val delta = CharacterData.getCaseDelta(properties)
        // If we have a non-zero delta with toUpperCase direction, use it
        return if (delta != 0 && !CharacterData.isDeltaToLowercase(properties)) {
            codepoint + delta
        } else {
            codepoint
        }
    }

    fun isSpaceChar(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isSpaceChar(properties)
    }

    fun isWhitespace(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isWhitespace(properties)
    }

    fun isIdeographic(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isIdeographic(properties)
    }

    fun isIdentifierIgnorable(codepoint: Int): Boolean {
        // 0x00-0x08, 0x0E-0x1B are identifier-ignorable in ASCII
        // 0x7F-0x9F are identifier-ignorable control characters
        if (codepoint <= 0x08 || (codepoint in 0x0E..0x1B) || (codepoint in 0x7F..0x9F)) {
            return true
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isFormatChar(properties)
    }

    fun isUnicodeIdentifierStart(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUnicodeIdentifierStart(properties)
    }

    fun isUnicodeIdentifierPart(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUnicodeIdentifierPart(properties)
    }

    fun isJavaIdentifierStart(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isJavaIdentifierStart(properties)
    }

    fun isJavaIdentifierPart(codepoint: Int): Boolean {
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isJavaIdentifierPart(properties)
    }

    fun isISOControl(codepoint: Int): Boolean = codepoint in 0x00..0x1F || codepoint in 0x7F..0x9F

    fun getUnicodeScript(codepoint: Int): UnicodeScript = ScriptData.getScript(codepoint)
}