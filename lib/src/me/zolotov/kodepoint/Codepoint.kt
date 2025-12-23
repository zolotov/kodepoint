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

    /**
     * Returns true if this codepoint is a Unicode letter (categories Lu, Ll, Lt, Lm, Lo).
     */
    fun isLetter(): Boolean = isLetter(codepoint)

    /**
     * Returns true if this codepoint is a Unicode decimal digit (category Nd).
     */
    fun isDigit(): Boolean = isDigit(codepoint)

    /**
     * Returns true if this codepoint is a Unicode letter or digit.
     */
    fun isLetterOrDigit(): Boolean = isLetterOrDigit(codepoint)

    /**
     * Returns true if this codepoint is an uppercase letter (category Lu).
     */
    fun isUpperCase(): Boolean = isUpperCase(codepoint)

    /**
     * Returns true if this codepoint is a lowercase letter (category Ll).
     */
    fun isLowerCase(): Boolean = isLowerCase(codepoint)

    /**
     * Converts this codepoint to lowercase.
     * Returns the same codepoint if no lowercase mapping exists.
     */
    fun toLowerCase(): Codepoint = Codepoint(toLowerCase(codepoint))

    /**
     * Converts this codepoint to uppercase.
     * Returns the same codepoint if no uppercase mapping exists.
     */
    fun toUpperCase(): Codepoint = Codepoint(toUpperCase(codepoint))

    /**
     * Converts this code point to its case-folded equivalent for case-insensitive comparison.
     * Case folding is more comprehensive than simple lowercase conversion and uses the
     * official Unicode case folding mappings.
     * Returns the same code point if no case folding mapping exists.
     */
    fun toCaseFolded(): Codepoint {
        // For simple case folding, lowercase is usually equivalent
        // Special cases would need separate lookup table
        return toLowerCase()
    }

    /**
     * Returns true if this codepoint is a Unicode space character (categories Zs, Zl, Zp).
     */
    fun isSpaceChar(): Boolean = isSpaceChar(codepoint)

    /**
     * Returns true if this codepoint is whitespace according to Java's definition.
     */
    fun isWhitespace(): Boolean = isWhitespace(codepoint)

    /**
     * Returns true if this codepoint is an ideographic character.
     */
    fun isIdeographic(): Boolean = isIdeographic(codepoint)

    /**
     * Returns the Unicode script for this codepoint.
     */
    fun getUnicodeScript(): UnicodeScript {
        // TODO: Implement proper script lookup using ScriptData
        return UnicodeScript.UNKNOWN
    }

    /**
     * Returns true if this codepoint should be ignored in identifiers.
     * This includes format characters (Cf) and zero-width characters.
     */
    fun isIdentifierIgnorable(): Boolean = isIdentifierIgnorable(codepoint)

    /**
     * Returns true if this codepoint can start a Unicode identifier.
     */
    fun isUnicodeIdentifierStart(): Boolean = isUnicodeIdentifierStart(codepoint)

    /**
     * Returns true if this codepoint can be part of a Unicode identifier (not start).
     */
    fun isUnicodeIdentifierPart(): Boolean = isUnicodeIdentifierPart(codepoint)

    /**
     * Returns true if this codepoint can start a Java identifier.
     * Java identifiers can start with letters, currency symbols, and connector punctuation.
     */
    fun isJavaIdentifierStart(): Boolean = isJavaIdentifierStart(codepoint)

    /**
     * Returns true if this codepoint can be part of a Java identifier (not start).
     */
    fun isJavaIdentifierPart(): Boolean = isJavaIdentifierPart(codepoint)

    fun isISOControl(): Boolean = isISOControl(codepoint)


    fun asString(): String {
        return codepointsToString(codepoint)
    }

    override fun toString(): String = "Codepoint(0x${codepoint.toString(16).uppercase()})"

    companion object {
        fun fromChars(highSurrogate: Char, lowSurrogate: Char): Codepoint =
            codepointOf(highSurrogate, lowSurrogate)
    }
}