package me.zolotov.kodepoint.generator

/**
 * Builds property tables for all Unicode planes using byte indices for memory efficiency.
 * Returns a PropertyTableBuildResult with the global value lookup table.
 */
fun buildPropertyTables(characterProperties: IntArray): PropertyTableBuildResult {
    // Build global value lookup from all unique values
    val uniqueCharacterProperties = characterProperties.toSet().toList().sorted().toIntArray()
    println("  Global unique values: ${uniqueCharacterProperties.size}")

    val propertyToIndex = uniqueCharacterProperties.withIndex().associate { it.value to it.index }

    val latin1Properties = characterProperties.sliceArray(0..255)

    val planeResults = PLANES.map { plane ->
        val paddedSize = 1 shl plane.totalBits
        val properties = IntArray(paddedSize)
        characterProperties.copyInto(properties, 0, plane.startCodepoint, plane.endCodepoint + 1)
        if (plane.sparse) {
            val ranges = extractRanges(properties)
            PlaneTableResult.Sparse(plane, ranges).also {
                println("  ${plane.name}: sparse (${ranges.size} ranges), size=${it.size} bytes")
            }
        } else {
            // Convert to indices and use standard lookup table
            val indexedData = properties.map { propertyToIndex[it]!! }.toIntArray()
            val table = findOptimalLookupTable(indexedData, plane.totalBits, 1)
            PlaneTableResult.Table(plane, table).also {
                val dataSize = table.dataTable.size
                val indexSize = table.indexTable.size * 2
                println("  ${plane.name}: indexBits=${table.indexBits}, blockBits=${table.blockBits}, " +
                    "index=${indexSize}b, data=${dataSize}b, total=${table.totalSize}b")
            }
        }
    }

    return PropertyTableBuildResult(latin1Properties, planeResults, uniqueCharacterProperties)
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
