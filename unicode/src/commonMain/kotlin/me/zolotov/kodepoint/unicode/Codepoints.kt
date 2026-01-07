package me.zolotov.kodepoint.unicode

import me.zolotov.kodepoint.generated.CharacterData
import me.zolotov.kodepoint.generated.ScriptData
import me.zolotov.kodepoint.script.UnicodeScript

object Codepoints {
    fun getUnicodeScript(codepoint: Int): UnicodeScript = ScriptData.getScript(codepoint)

    fun isLetter(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z, a-z using bit-twiddling
        // (cp or 0x20) maps A-Z to a-z, then unsigned range check
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLetter(properties)
    }

    fun isDigit(codepoint: Int): Boolean {
        // ASCII fast-path: 0-9 using unsigned range check
        if (isAscii(codepoint)) {
            return (codepoint - '0'.code).toUInt() <= 9u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isDigit(properties)
    }

    fun isLetterOrDigit(codepoint: Int): Boolean {
        // ASCII fast-path using bit-twiddling
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u ||
                   (codepoint - '0'.code).toUInt() <= 9u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLetter(properties) || CharacterData.isDigit(properties)
    }

    fun isUpperCase(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z using unsigned range check
        if (isAscii(codepoint)) {
            return (codepoint - 'A'.code).toUInt() <= 25u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUpperCase(properties)
    }

    fun isLowerCase(codepoint: Int): Boolean {
        // ASCII fast-path: a-z using unsigned range check
        if (isAscii(codepoint)) {
            return (codepoint - 'a'.code).toUInt() <= 25u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isLowerCase(properties)
    }

    fun toLowerCase(codepoint: Int): Int {
        // ASCII fast-path: A-Z -> a-z (add 32)
        if (isAscii(codepoint)) {
            return if ((codepoint - 'A'.code).toUInt() <= 25u) codepoint + 32 else codepoint
        }

        // Check properties first (fast table lookup)
        val properties = CharacterData.getProperties(codepoint)

        // Check for large delta flag first - avoids redundant isUpperCase check
        if (CharacterData.hasLargeLowercaseDelta(properties)) {
            return CharacterData.getLargeLowercase(codepoint)
        }

        val delta = CharacterData.getCaseDelta(properties)

        // If we have a non-zero delta with toLowerCase direction, use it
        if (delta != 0 && CharacterData.isDeltaToLowercase(properties)) {
            return codepoint + delta
        }

        return codepoint
    }

    fun toUpperCase(codepoint: Int): Int {
        // ASCII fast-path: a-z -> A-Z (subtract 32)
        if (isAscii(codepoint)) {
            return if ((codepoint - 'a'.code).toUInt() <= 25u) codepoint - 32 else codepoint
        }

        // Check properties first (fast table lookup)
        val properties = CharacterData.getProperties(codepoint)

        // Special handling for titlecase letters (Lt)
        if (CharacterData.isTitleCase(properties)) {
            // Latin Extended-B titlecase letters: 01C5, 01C8, 01CB, 01F2
            return when (codepoint) {
                0x01C5, 0x01C8, 0x01CB, 0x01F2 -> codepoint - 1
                else -> codepoint // Greek titlecase letters have no uppercase
            }
        }

        // Check for large delta flag first - avoids redundant isLowerCase check
        if (CharacterData.hasLargeUppercaseDelta(properties)) {
            return CharacterData.getLargeUppercase(codepoint)
        }

        val delta = CharacterData.getCaseDelta(properties)

        // If we have a non-zero delta with toUpperCase direction, use it
        if (delta != 0 && !CharacterData.isDeltaToLowercase(properties)) {
            return codepoint + delta
        }

        return codepoint
    }

    fun isSpaceChar(codepoint: Int): Boolean {
        // ASCII fast-path: only space (0x20) is a space char in ASCII
        // (Tab, newline, etc. are control characters, not space separators)
        if (isAscii(codepoint)) {
            return codepoint == ' '.code
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isSpaceChar(properties)
    }

    fun isWhitespace(codepoint: Int): Boolean {
        // ASCII fast-path: HT(9), LF(10), VT(11), FF(12), CR(13), space(32)
        if (isAscii(codepoint)) {
            return codepoint == ' '.code || codepoint in 0x09..0x0D
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isWhitespace(properties)
    }

    fun isIdeographic(codepoint: Int): Boolean {
        // ASCII fast-path: no ideographic characters in ASCII
        if (isAscii(codepoint)) {
            return false
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isIdeographic(properties)
    }

    fun isIdentifierIgnorable(codepoint: Int): Boolean {
        // ASCII fast-path: 0x00-0x08, 0x0E-0x1B, 0x7F
        if (isAscii(codepoint)) {
            return codepoint in 0x00..0x08 || codepoint in 0x0E..0x1B || codepoint == 0x7F
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isFormatChar(properties) || codepoint in 0x80..0x9F
    }

    fun isUnicodeIdentifierStart(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z, a-z using bit-twiddling
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUnicodeIdentifierStart(properties)
    }

    fun isUnicodeIdentifierPart(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z, a-z, 0-9, _ (plus some control chars that are identifier ignorable)
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u ||
                   (codepoint - '0'.code).toUInt() <= 9u ||
                   codepoint == '_'.code ||
                   codepoint <= 0x08 ||
                   (codepoint - 0x0E).toUInt() <= (0x1B - 0x0E).toUInt() ||
                   codepoint == 0x7F
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isUnicodeIdentifierPart(properties)
    }

    fun isJavaIdentifierStart(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z, a-z, $, _
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u ||
                   codepoint == '$'.code ||
                   codepoint == '_'.code
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isJavaIdentifierStart(properties)
    }

    fun isJavaIdentifierPart(codepoint: Int): Boolean {
        // ASCII fast-path: A-Z, a-z, 0-9, $, _ (plus some control chars that are identifier ignorable)
        if (isAscii(codepoint)) {
            return ((codepoint or 0x20) - 'a'.code).toUInt() <= 25u ||
                   (codepoint - '0'.code).toUInt() <= 9u ||
                   codepoint == '$'.code ||
                   codepoint == '_'.code ||
                   codepoint <= 0x08 ||
                   (codepoint - 0x0E).toUInt() <= (0x1B - 0x0E).toUInt() ||
                   codepoint == 0x7F
        }
        val properties = CharacterData.getProperties(codepoint)
        return CharacterData.isJavaIdentifierPart(properties)
    }

    fun isISOControl(codepoint: Int): Boolean {
        return codepoint in 0x00..0x1F || codepoint in 0x7F..0x9F
    }

    private fun isAscii(codepoint: Int): Boolean = codepoint <= 0x7F
}