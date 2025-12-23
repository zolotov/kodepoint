package me.zolotov.kodepoint

internal const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000

internal expect fun codepointsToString(vararg codepoints: Int): String
internal expect fun codepointOf(highSurrogate: Char, lowSurrogate: Char): Codepoint
internal expect fun highSurrogate(codepoint: Int): Char
internal expect fun lowSurrogate(codepoint: Int): Char
internal expect fun isLetter(codepoint: Int): Boolean
internal expect fun isDigit(codepoint: Int): Boolean
internal expect fun isLetterOrDigit(codepoint: Int): Boolean
internal expect fun isUpperCase(codepoint: Int): Boolean
internal expect fun isLowerCase(codepoint: Int): Boolean
internal expect fun toLowerCase(codepoint: Int): Int
internal expect fun toUpperCase(codepoint: Int): Int
internal expect fun isSpaceChar(codepoint: Int): Boolean
internal expect fun isWhitespace(codepoint: Int): Boolean
internal expect fun isIdeographic(codepoint: Int): Boolean
internal expect fun isIdentifierIgnorable(codepoint: Int): Boolean
internal expect fun isUnicodeIdentifierStart(codepoint: Int): Boolean
internal expect fun isUnicodeIdentifierPart(codepoint: Int): Boolean
internal expect fun isJavaIdentifierStart(codepoint: Int): Boolean
internal expect fun isJavaIdentifierPart(codepoint: Int): Boolean
internal expect fun isISOControl(codepoint: Int): Boolean