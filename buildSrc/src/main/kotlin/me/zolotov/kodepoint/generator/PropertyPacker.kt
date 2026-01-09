package me.zolotov.kodepoint.generator

/**
 * Packs character properties into a 32-bit integer.
 *
 * Bit layout:
 * - bits 0-9:   Case delta (10 bits, signed)
 * - bit 10:     Delta is to lowercase (vs uppercase)
 * - bits 11-15: General category (5 bits)
 * - bit 16:     Is Other_Uppercase
 * - bit 17:     Is Other_Lowercase
 * - bit 18:     Is White_Space
 * - bit 19:     Is Ideographic
 * - bit 20:     Is Unicode ID_Start
 * - bit 21:     Is Unicode ID_Continue
 * - bit 22:     Is Java identifier start
 * - bit 23:     Is Java identifier part
 * - bit 24:     Has large lowercase delta
 * - bit 25:     Has large uppercase delta
 */
object PropertyPacker {
    const val CASE_DELTA_MASK = 0x3FF            // bits 0-9 (10 bits)
    const val DELTA_TO_LOWERCASE_BIT = 1 shl 10  // bit 10
    const val CATEGORY_SHIFT = 11
    const val IS_OTHER_UPPERCASE_BIT = 1 shl 16
    const val IS_OTHER_LOWERCASE_BIT = 1 shl 17
    const val IS_WHITESPACE_BIT = 1 shl 18
    const val IS_IDEOGRAPHIC_BIT = 1 shl 19
    const val IS_UNICODE_ID_START_BIT = 1 shl 20
    const val IS_UNICODE_ID_PART_BIT = 1 shl 21
    const val IS_JAVA_ID_START_BIT = 1 shl 22
    const val IS_JAVA_ID_PART_BIT = 1 shl 23
    const val HAS_LARGE_LOWERCASE_DELTA_BIT = 1 shl 24
    const val HAS_LARGE_UPPERCASE_DELTA_BIT = 1 shl 25

    const val MIN_DELTA = -512
    const val MAX_DELTA = 511

    fun pack(data: CharacterData): Int {
        var props: Int
        val (delta, isToLowercase) = when {
            data.lowerCase >= 0 -> data.lowerCase - data.codepoint to true
            data.upperCase >= 0 -> data.upperCase - data.codepoint to false
            else -> 0 to false
        }

        if (delta != 0 && (delta !in MIN_DELTA..MAX_DELTA)) {
            props = if (isToLowercase) {
                HAS_LARGE_LOWERCASE_DELTA_BIT
            } else {
                HAS_LARGE_UPPERCASE_DELTA_BIT
            }
        } else {
            props = delta and CASE_DELTA_MASK
            if (isToLowercase && delta != 0) {
                props = props or DELTA_TO_LOWERCASE_BIT
            }
        }
        props = props or (data.category.code shl CATEGORY_SHIFT)

        if (data.isOtherUppercase) props = props or IS_OTHER_UPPERCASE_BIT
        if (data.isOtherLowercase) props = props or IS_OTHER_LOWERCASE_BIT
        if (data.isWhitespace) props = props or IS_WHITESPACE_BIT
        if (data.isIdeographic) props = props or IS_IDEOGRAPHIC_BIT
        if (data.isIdStart) props = props or IS_UNICODE_ID_START_BIT
        if (data.isIdContinue) props = props or IS_UNICODE_ID_PART_BIT
        if (data.isJavaIdentifierStart) props = props or IS_JAVA_ID_START_BIT
        if (data.isJavaIdentifierPart) props = props or IS_JAVA_ID_PART_BIT

        return props
    }
}
