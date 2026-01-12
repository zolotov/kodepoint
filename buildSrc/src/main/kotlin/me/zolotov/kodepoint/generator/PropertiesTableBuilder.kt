package me.zolotov.kodepoint.generator

/**
 * Builds property tables for all Unicode planes.
 */
fun buildPropertyTables(characterProperties: IntArray): List<PlaneTableResult> {
    return PLANES.map { plane ->
        val paddedSize = 1 shl plane.totalBits
        val properties = IntArray(paddedSize)
        characterProperties.copyInto(properties, 0, plane.startCodepoint, plane.endCodepoint + 1)
        if (plane.sparse) {
            val ranges = extractRanges(properties)
            PlaneTableResult.Sparse(plane, ranges).also {
                println("  ${plane.name}: sparse (${ranges.size} ranges), size=${it.size} bytes")
            }
        } else {
            val table = findOptimalLookupTable(properties, plane.totalBits, 4)
            PlaneTableResult.Table(plane, table).also {
                println("  ${plane.name}: indexBits=${table.indexBits}, blockBits=${table.blockBits}, size=${table.totalSize} bytes")
            }
        }
    }
}

/**
 * Extracts ranges of properties with the same value (for sparse encoding).
 */
fun extractRanges(values: IntArray): List<RangeValue> {
    return buildList {
        var i = 0
        while (i < values.size) {
            val props = values[i]
            if (props != 0) {
                val startOffset = i
                while (i < values.size && values[i] == props) {
                    i++
                }
                add(RangeValue(startOffset, i - 1, props))
            } else {
                i++
            }
        }
    }
}