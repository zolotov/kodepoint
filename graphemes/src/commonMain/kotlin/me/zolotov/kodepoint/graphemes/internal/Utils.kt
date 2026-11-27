package me.zolotov.kodepoint.graphemes.internal

/**
 * Binary search over ranges stored as triplets `[start, end, value]`, sorted by start.
 * Returns the value of the range containing [key], or [defaultValue].
 */
internal fun binarySearchRange(key: Int, ranges: IntArray, defaultValue: Int): Int {
    var low = 0
    var high = ranges.size / 3 - 1
    while (low <= high) {
        val mid = (low + high) ushr 1
        val index = mid * 3
        when {
            key < ranges[index] -> high = mid - 1
            key > ranges[index + 1] -> low = mid + 1
            else -> return ranges[index + 2]
        }
    }
    return defaultValue
}
