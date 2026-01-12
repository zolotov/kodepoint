package me.zolotov.kodepoint.generator

data class CharacterData(
    val codepoint: Int,
    var category: GeneralCategory = GeneralCategory.Cn,
    var upperCase: Int = -1,      // Simple uppercase mapping (-1 = none)
    var lowerCase: Int = -1,      // Simple lowercase mapping (-1 = none)
    var caseFold: Int = -1,       // Case fold mapping (-1 = none)
    var isIdStart: Boolean = false,
    var isIdContinue: Boolean = false,
    var isXidStart: Boolean = false,
    var isXidContinue: Boolean = false,
    var hasSpecialCase: Boolean = false,  // Multi-char case mapping
    var isWhitespace: Boolean = false,    // White_Space property
    var isIdeographic: Boolean = false,   // Ideographic property
    var isOtherLowercase: Boolean = false, // Other_Lowercase property
    var isOtherUppercase: Boolean = false, // Other_Uppercase property

    // Computed boolean properties, derived from category
    var isLetter: Boolean = false,
    var isDigit: Boolean = false,
    var isUpperCase: Boolean = false,
    var isLowerCase: Boolean = false,
    var isTitleCase: Boolean = false,
    var isSpaceChar: Boolean = false,
    var isISOControl: Boolean = false,
    var isFormatChar: Boolean = false,

    // Computed boolean properties, derived from JVM Character class
    var isJavaIdentifierStart: Boolean = false,
    var isJavaIdentifierPart: Boolean = false
)

data class RangeValue(
    val startCodepoint: Int,
    val endCodepoint: Int,
    val value: Int
)

/**
 * Information about a Unicode plane for code generation.
 *
 * @property name Short name of the plane (e.g., "BMP", "SMP")
 * @property className Generated class name for this plane's character data
 * @property startCodepoint First codepoint in this plane's range
 * @property endCodepoint Last codepoint in this plane's range
 * @property sparse Whether to consider sparse (binary search) encoding for this plane.
 *   When true, the generator compares sparse vs. table encoding sizes and picks the smaller one.
 *   When false, table encoding is always used for O(1) lookup performance.
 *
 *   Trade-off: Sparse encoding saves memory but has O(log n) lookup time.
 *   Table encoding uses more memory but provides O(1) lookup.
 *   For frequently accessed planes like BMP, table encoding is preferred.
 */
data class PlaneInfo(
    val name: String,
    val className: String,
    val startCodepoint: Int,
    val endCodepoint: Int,
    val sparse: Boolean,
) {
    /** Number of bits needed to address all codepoints in this plane's range. */
    val totalBits: Int = 32 - (endCodepoint - startCodepoint).countLeadingZeroBits()
}

sealed interface PlaneTableResult {
    val plane: PlaneInfo
    val size: Int

    data class Table(override val plane: PlaneInfo, val table: LookupTable) : PlaneTableResult {
        override val size: Int = table.totalSize
    }

    data class Sparse(override val plane: PlaneInfo, val ranges: List<RangeValue>) : PlaneTableResult {
        override val size: Int = ranges.size * 12  // 4 bytes start + 4 bytes end + 4 bytes properties
    }
}