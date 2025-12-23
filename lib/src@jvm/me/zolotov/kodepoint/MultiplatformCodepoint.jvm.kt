package me.zolotov.kodepoint


internal actual fun codepointsToString(vararg codepoints: Int): String = java.lang.String(codepoints, 0, codepoints.size).toString()

internal actual fun codepointOf(highSurrogate: Char, lowSurrogate: Char): Codepoint =
    Codepoint(Character.toCodePoint(highSurrogate, lowSurrogate))

internal actual fun highSurrogate(codepoint: Int): Char = Character.highSurrogate(codepoint)

internal actual fun lowSurrogate(codepoint: Int): Char = Character.lowSurrogate(codepoint)

internal actual fun isLetter(codepoint: Int): Boolean = Character.isLetter(codepoint)
internal actual fun isDigit(codepoint: Int): Boolean = Character.isDigit(codepoint)
internal actual fun isLetterOrDigit(codepoint: Int): Boolean = Character.isLetterOrDigit(codepoint)
internal actual fun isUpperCase(codepoint: Int): Boolean = Character.isUpperCase(codepoint)
internal actual fun isLowerCase(codepoint: Int): Boolean = Character.isLowerCase(codepoint)
internal actual fun toLowerCase(codepoint: Int): Int = Character.toLowerCase(codepoint)
internal actual fun toUpperCase(codepoint: Int): Int = Character.toUpperCase(codepoint)
internal actual fun isSpaceChar(codepoint: Int): Boolean = Character.isSpaceChar(codepoint)
internal actual fun isWhitespace(codepoint: Int): Boolean = Character.isWhitespace(codepoint)
internal actual fun isIdeographic(codepoint: Int): Boolean = Character.isIdeographic(codepoint)
internal actual fun isIdentifierIgnorable(codepoint: Int): Boolean = Character.isIdentifierIgnorable(codepoint)
internal actual fun isUnicodeIdentifierStart(codepoint: Int): Boolean = Character.isUnicodeIdentifierStart(codepoint)
internal actual fun isUnicodeIdentifierPart(codepoint: Int): Boolean = Character.isUnicodeIdentifierPart(codepoint)
internal actual fun isJavaIdentifierStart(codepoint: Int): Boolean = Character.isJavaIdentifierStart(codepoint)
internal actual fun isJavaIdentifierPart(codepoint: Int): Boolean = Character.isJavaIdentifierPart(codepoint)
internal actual fun isISOControl(codepoint: Int): Boolean = Character.isISOControl(codepoint)