package me.zolotov.kodepoint.generator

class LookupTable(
    val indexTable: IntArray,
    val dataTable: IntArray,
    val totalSize: Int,
    val indexBits: Int,
    val blockBits: Int
)

/**
 * Finds the optimal 2-level decomposition for the given data.
 */
fun findOptimalLookupTable(data: IntArray, totalBits: Int, propertyBytes: Int): LookupTable {
    var best: LookupTable? = null

    for (indexBits in 6..14) {
        val blockBits = totalBits - indexBits
        if (blockBits !in 4..12) continue

        val result = buildTables(data, indexBits, blockBits, propertyBytes)
        if (best == null || result.totalSize < best.totalSize) {
            best = result
        }
    }

    return best ?: error("No valid decomposition found")
}

private fun buildTables(data: IntArray, indexBits: Int, blockBits: Int, propertyBytes: Int): LookupTable {
    val dedup = deduplicateBlocks(data, 1 shl indexBits)
    val indexTable = dedup.indices
    val dataTable = dedup.blocks

    val totalSize = indexTable.size * 2 + dataTable.size * propertyBytes

    return LookupTable(indexTable, dataTable, totalSize, indexBits, blockBits)
}

private class DeduplicationResult(
    val indices: IntArray,
    val blocks: IntArray
)

private fun deduplicateBlocks(data: IntArray, blockCount: Int): DeduplicationResult {
    val blockSize = data.size / blockCount
    val uniqueBlocks = mutableListOf<IntArray>()
    val blockMap = mutableMapOf<List<Int>, Int>()
    val indices = IntArray(blockCount)

    for (blockIdx in 0 until blockCount) {
        val start = blockIdx * blockSize
        val block = data.sliceArray(start until (start + blockSize))
        val blockList = block.toList()

        val existingIdx = blockMap[blockList]
        if (existingIdx != null) {
            indices[blockIdx] = existingIdx
        } else {
            val newIdx = uniqueBlocks.size
            uniqueBlocks.add(block)
            blockMap[blockList] = newIdx
            indices[blockIdx] = newIdx
        }
    }

    val flatBlocks = IntArray(uniqueBlocks.size * blockSize) { i ->
        uniqueBlocks[i / blockSize][i % blockSize]
    }

    return DeduplicationResult(indices, flatBlocks)
}