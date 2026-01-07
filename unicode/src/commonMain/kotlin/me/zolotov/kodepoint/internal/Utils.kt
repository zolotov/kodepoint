package me.zolotov.kodepoint.internal

internal fun isAscii(codepoint: Int): Boolean = codepoint < 0x80

/**
 * Fast ASCII lowercase conversion.
 * Converts A-Z to a-z, leaves other characters unchanged.
 */
internal fun asciiToLowerCase(codepoint: Int): Int =
    if ((codepoint - 'A'.code).toUInt() <= 25u) codepoint + 32 else codepoint

/**
 * Fast ASCII uppercase conversion.
 * Converts a-z to A-Z, leaves other characters unchanged.
 */
internal fun asciiToUpperCase(codepoint: Int): Int =
    if ((codepoint - 'a'.code).toUInt() <= 25u) codepoint - 32 else codepoint
