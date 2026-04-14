package me.zolotov.kodepoint.generator

/**
 * Maps EastAsianWidth.txt abbreviations to ordinals of the EastAsianWidth enum.
 * Order must match the enum declaration in EastAsianWidth.kt exactly.
 */
val EAW_ABBREV_TO_ID = mapOf(
    "N" to 0,   // NEUTRAL
    "Na" to 1,  // NARROW
    "W" to 2,   // WIDE
    "F" to 3,   // FULLWIDTH
    "H" to 4,   // HALFWIDTH
    "A" to 5,   // AMBIGUOUS
)

/**
 * Builds lookup tables for Unicode East Asian Width data.
 *
 * Uses the same plane structure as [buildScriptTables]: a direct byte array for Latin-1,
 * a two-level block table for the BMP, and binary-search sparse ranges for supplementary planes.
 */
fun buildEastAsianWidthTables(eastAsianWidths: Map<Int, String>): EastAsianWidthBuildResult {
    val latin1WidthIds = IntArray(256) { cp ->
        val abbrev = eastAsianWidths[cp] ?: "N"
        EAW_ABBREV_TO_ID[abbrev] ?: 0
    }

    val planeResults = mutableListOf<EastAsianWidthPlaneResult>()

    for (plane in PLANES) {
        val paddedSize = 1 shl plane.totalBits

        val widthData = IntArray(paddedSize) { offset ->
            val cp = plane.startCodepoint + offset
            if (cp <= plane.endCodepoint && cp <= MAX_CODEPOINT) {
                val abbrev = eastAsianWidths[cp] ?: "N"
                EAW_ABBREV_TO_ID[abbrev] ?: 0
            } else {
                0  // NEUTRAL for out-of-range padding
            }
        }

        if (plane.sparse) {
            val ranges = extractRanges(widthData)
            planeResults.add(EastAsianWidthPlaneResult.Sparse(plane, ranges))
        } else {
            val result = findOptimalLookupTable(widthData, plane.totalBits, 1)
            planeResults.add(EastAsianWidthPlaneResult.Table(plane, result))
        }
    }

    return EastAsianWidthBuildResult(planeResults, latin1WidthIds)
}

sealed interface EastAsianWidthPlaneResult {
    val plane: PlaneInfo
    val size: Int

    data class Table(override val plane: PlaneInfo, val table: LookupTable) : EastAsianWidthPlaneResult {
        override val size: Int = table.totalSize
    }

    data class Sparse(override val plane: PlaneInfo, val ranges: List<RangeValue>) : EastAsianWidthPlaneResult {
        override val size: Int = ranges.size * 9  // 4 bytes start + 4 bytes end + 1 byte id
    }
}

class EastAsianWidthBuildResult(
    val planeResults: List<EastAsianWidthPlaneResult>,
    val latin1WidthIds: IntArray,
)
