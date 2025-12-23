package me.zolotov.kodepoint

import me.zolotov.kodepoint.unicode.CodepointFunctions

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

internal actual fun highSurrogate(codepoint: Int): Char =
    ((codepoint ushr 10) + HIGH_SURROGATE_ENCODE_OFFSET).toChar()

internal actual fun lowSurrogate(codepoint: Int): Char =
    ((codepoint and 0x3FF) + MIN_LOW_SURROGATE).toChar()

internal actual fun isLetter(codepoint: Int): Boolean = CodepointFunctions.isLetter(codepoint)
internal actual fun isDigit(codepoint: Int): Boolean = CodepointFunctions.isDigit(codepoint)
internal actual fun isLetterOrDigit(codepoint: Int): Boolean = CodepointFunctions.isLetterOrDigit(codepoint)
internal actual fun isUpperCase(codepoint: Int): Boolean = CodepointFunctions.isUpperCase(codepoint)
internal actual fun isLowerCase(codepoint: Int): Boolean = CodepointFunctions.isLowerCase(codepoint)
internal actual fun toLowerCase(codepoint: Int): Int = CodepointFunctions.toLowerCase(codepoint)
internal actual fun toUpperCase(codepoint: Int): Int = CodepointFunctions.toUpperCase(codepoint)
internal actual fun isSpaceChar(codepoint: Int): Boolean = CodepointFunctions.isSpaceChar(codepoint)
internal actual fun isWhitespace(codepoint: Int): Boolean = CodepointFunctions.isWhitespace(codepoint)
internal actual fun isIdeographic(codepoint: Int): Boolean = CodepointFunctions.isIdeographic(codepoint)
internal actual fun isIdentifierIgnorable(codepoint: Int): Boolean = CodepointFunctions.isIdentifierIgnorable(codepoint)
internal actual fun isUnicodeIdentifierStart(codepoint: Int): Boolean =
    CodepointFunctions.isUnicodeIdentifierStart(codepoint)

internal actual fun isUnicodeIdentifierPart(codepoint: Int): Boolean =
    CodepointFunctions.isUnicodeIdentifierPart(codepoint)

internal actual fun isJavaIdentifierStart(codepoint: Int): Boolean = CodepointFunctions.isJavaIdentifierStart(codepoint)
internal actual fun isJavaIdentifierPart(codepoint: Int): Boolean = CodepointFunctions.isJavaIdentifierPart(codepoint)
internal actual fun isISOControl(codepoint: Int): Boolean = CodepointFunctions.isISOControl(codepoint)