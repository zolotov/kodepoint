package me.zolotov.kodepoint

import kotlin.jvm.JvmInline

/**
 * A value class representing a Unicode code point with efficient property lookup.
 *
 * This class provides access to Unicode properties such as case information,
 * character categories, and script information in a compact and efficient manner.
 */
@JvmInline
value class Codepoint(val codepoint: Int) {
    val charCount: Int
        get() = if (codepoint < MIN_SUPPLEMENTARY_CODE_POINT) 1 else 2

    internal fun isBmpCodePoint(): Boolean = codepoint ushr 16 == 0
    fun isLetter(): Boolean = TODO()
    fun isDigit(): Boolean = TODO()
    fun isLetterOrDigit(): Boolean = TODO()
    fun isUpperCase(): Boolean = TODO()
    fun isLowerCase(): Boolean = TODO()
    fun toLowerCase(): Codepoint = TODO()
    fun toUpperCase(): Codepoint = TODO()

    /**
     * Converts this code point to its case-folded equivalent for case-insensitive comparison.
     * Case folding is more comprehensive than simple lowercase conversion and uses the
     * official Unicode case folding mappings.
     * Returns the same code point if no case folding mapping exists.
     */
    fun toCaseFolded(): Codepoint = TODO()

    fun isSpaceChar(): Boolean = TODO()
    fun isWhitespace(): Boolean = TODO()

    fun isIdeographic(): Boolean = TODO()
    fun getUnicodeScript(): UnicodeScript = TODO()

    fun isIdentifierIgnorable(): Boolean = TODO()
    fun isUnicodeIdentifierStart(): Boolean = TODO()
    fun isUnicodeIdentifierPart(): Boolean = TODO()
    fun isJavaIdentifierStart(): Boolean = TODO()
    fun isJavaIdentifierPart(): Boolean = TODO()

    fun isISOControl(): Boolean {
        return codepoint in 0x00..0x1F || // 0000..001F    ; Common # Cc  [32] <control-0000>..<control-001F>
                codepoint in 0x7F..0x9F // 007F..009F    ; Common # Cc  [33] <control-007F>..<control-009F>
    }

    fun asString(): String {
        return codePointsToStringPlatformSpecific(codepoint)
    }

    override fun toString(): String = "Codepoint(0x${codepoint.toString(16).uppercase()})"

    companion object {
        fun fromChars(highSurrogate: Char, lowSurrogate: Char): Codepoint =
            codepointOfPlatformSpecific(highSurrogate, lowSurrogate)
    }
}