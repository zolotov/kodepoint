package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.graphemes.internal.isBoundary
import me.zolotov.kodepoint.graphemes.internal.nextBoundary
import me.zolotov.kodepoint.graphemes.internal.nextBoundaryFromBoundary
import me.zolotov.kodepoint.graphemes.internal.previousBoundary
import me.zolotov.kodepoint.graphemes.internal.previousBoundaryWithHint
import me.zolotov.kodepoint.graphemes.internal.unpackBoundary
import me.zolotov.kodepoint.graphemes.internal.unpackRunStart

/*
 * Extended grapheme clusters (UAX #29) of a CharSequence, by the rules of Unicode
 * [Graphemes.UNICODE_VERSION] on every target. All indices are UTF-16 `Char` indices; a cluster
 * boundary never splits a surrogate pair. A lone surrogate counts as one code point, as in
 * `forEachCodepoint`.
 */

/**
 * Calls [action] with the `start` (inclusive) and `end` (exclusive) index of every grapheme
 * cluster, left to right. Allocates nothing.
 *
 * ```
 * "é👨‍👩‍👧".forEachGraphemeCluster { start, end -> println("$start..<$end") }  // 0..<2, 2..<10
 * ```
 */
inline fun CharSequence.forEachGraphemeCluster(action: (start: Int, end: Int) -> Unit) {
    var start = 0
    val length = length
    while (start < length) {
        val end = nextBoundaryFromBoundary(this, start)
        action(start, end)
        start = end
    }
}

/**
 * Calls [action] with the same clusters as [forEachGraphemeCluster], right to left. Allocates nothing.
 */
inline fun CharSequence.forEachGraphemeClusterReversed(action: (start: Int, end: Int) -> Unit) {
    var end = length
    var runStart = -1
    while (end > 0) {
        val packed = previousBoundaryWithHint(this, end, runStart)
        val start = unpackBoundary(packed)
        runStart = unpackRunStart(packed)
        action(start, end)
        end = start
    }
}

/** The number of grapheme clusters, the characters a user perceives: `"🇺🇦🇩🇪".graphemeClusterCount() == 2`. */
fun CharSequence.graphemeClusterCount(): Int {
    var count = 0
    forEachGraphemeCluster { _, _ -> count++ }
    return count
}

/**
 * Whether a grapheme cluster starts or ends at [index] in `0..length`. Indices 0 and [length] are
 * always boundaries.
 *
 * @throws IndexOutOfBoundsException if [index] is outside `0..length`.
 */
fun CharSequence.isGraphemeClusterBoundary(index: Int): Boolean = isBoundary(this, index)

/**
 * The smallest grapheme-cluster boundary greater than [index]; [length] for [length].
 *
 * @throws IndexOutOfBoundsException if [index] is outside `0..length`.
 */
fun CharSequence.nextGraphemeClusterBoundary(index: Int): Int = nextBoundary(this, index)

/**
 * The largest grapheme-cluster boundary less than [index]; 0 for 0.
 *
 * @throws IndexOutOfBoundsException if [index] is outside `0..length`.
 */
fun CharSequence.previousGraphemeClusterBoundary(index: Int): Int = previousBoundary(this, index)

/**
 * The grapheme clusters as strings, computed lazily. Allocates one string per cluster; use
 * [forEachGraphemeCluster] to avoid that.
 */
fun CharSequence.graphemeClusters(): Sequence<String> {
    val text = this
    return sequence {
        var start = 0
        while (start < text.length) {
            val end = nextBoundaryFromBoundary(text, start)
            yield(text.subSequence(start, end).toString())
            start = end
        }
    }
}
