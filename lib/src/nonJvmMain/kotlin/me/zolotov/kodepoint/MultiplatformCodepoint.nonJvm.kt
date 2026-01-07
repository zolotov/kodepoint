package me.zolotov.kodepoint

import me.zolotov.kodepoint.script.UnicodeScript
import me.zolotov.kodepoint.unicode.Codepoints

private const val MIN_HIGH_SURROGATE = 0xD800
private const val MIN_LOW_SURROGATE = 0xDC00
private const val SURROGATE_DECODE_OFFSET =
    MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE shl 10) - MIN_LOW_SURROGATE
private const val HIGH_SURROGATE_ENCODE_OFFSET = (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))

internal actual fun codepointsToString(vararg codepoints: Int): String = buildString(capacity = codepoints.size * 2) {
    for (codePoint in codepoints) {
        appendCodePoint(Codepoint(codePoint))
    }
}

internal actual fun codepointOf(highSurrogate: Char, lowSurrogate: Char): Codepoint =
    Codepoint((highSurrogate.code shl 10) + lowSurrogate.code + SURROGATE_DECODE_OFFSET)

internal actual fun highSurrogate(codepoint: Int): Char = ((codepoint ushr 10) + HIGH_SURROGATE_ENCODE_OFFSET).toChar()
internal actual fun lowSurrogate(codepoint: Int): Char = ((codepoint and 0x3FF) + MIN_LOW_SURROGATE).toChar()

internal actual fun isLetter(codepoint: Int): Boolean = Codepoints.isLetter(codepoint)
internal actual fun isDigit(codepoint: Int): Boolean = Codepoints.isDigit(codepoint)
internal actual fun isLetterOrDigit(codepoint: Int): Boolean = Codepoints.isLetterOrDigit(codepoint)
internal actual fun isUpperCase(codepoint: Int): Boolean = Codepoints.isUpperCase(codepoint)
internal actual fun isLowerCase(codepoint: Int): Boolean = Codepoints.isLowerCase(codepoint)
internal actual fun toLowerCase(codepoint: Int): Int = Codepoints.toLowerCase(codepoint)
internal actual fun toUpperCase(codepoint: Int): Int = Codepoints.toUpperCase(codepoint)
internal actual fun isSpaceChar(codepoint: Int): Boolean = Codepoints.isSpaceChar(codepoint)
internal actual fun isWhitespace(codepoint: Int): Boolean = Codepoints.isWhitespace(codepoint)
internal actual fun isIdeographic(codepoint: Int): Boolean = Codepoints.isIdeographic(codepoint)
internal actual fun isIdentifierIgnorable(codepoint: Int): Boolean = Codepoints.isIdentifierIgnorable(codepoint)
internal actual fun isUnicodeIdentifierStart(codepoint: Int): Boolean = Codepoints.isUnicodeIdentifierStart(codepoint)
internal actual fun isUnicodeIdentifierPart(codepoint: Int): Boolean = Codepoints.isUnicodeIdentifierPart(codepoint)
internal actual fun isJavaIdentifierStart(codepoint: Int): Boolean = Codepoints.isJavaIdentifierStart(codepoint)
internal actual fun isJavaIdentifierPart(codepoint: Int): Boolean = Codepoints.isJavaIdentifierPart(codepoint)
internal actual fun isISOControl(codepoint: Int): Boolean = Codepoints.isISOControl(codepoint)
internal actual fun getUnicodeScript(codepoint: Int): UnicodeScript = Codepoints.getUnicodeScript(codepoint)
