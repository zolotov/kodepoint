package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.Codepoint
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.EXTENDED_PICTOGRAPHIC_BIT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_EXTEND
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_MASK
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_REGIONAL_INDICATOR
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_ZWJ
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_EXTEND
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_LINKER
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_MASK
import me.zolotov.kodepoint.graphemes.internal.BREAK
import me.zolotov.kodepoint.graphemes.internal.CHECK_CONJUNCT
import me.zolotov.kodepoint.graphemes.internal.CHECK_EMOJI_ZWJ
import me.zolotov.kodepoint.graphemes.internal.NO_BREAK
import me.zolotov.kodepoint.graphemes.internal.pairRule
import me.zolotov.kodepoint.graphemes.internal.properties

/**
 * Finds grapheme-cluster boundaries in text that arrives one code point at a time and is not a
 * `CharSequence`, such as terminal output or an editor buffer. Feed every code point in order to
 * [isBoundaryBefore]; it keeps the state GB9c, GB11 and GB12/13 need and allocates nothing.
 *
 * ```
 * val segmenter = GraphemeClusterSegmenter()
 * "éx".forEachCodepoint { cp -> print(segmenter.isBoundaryBefore(cp)) }  // true false true
 * ```
 *
 * The end of the text is always a boundary. An instance is not thread-safe.
 */
class GraphemeClusterSegmenter {
    /** Packed properties of the previous code point, [START] at the start of the text, or [ASCII]. */
    private var before = START
    private var beforeCodepoint = -1
    private var conjunct = false
    private var pictographic = false
    private var zwjAfterPictographic = false
    private var oddRegionalIndicators = false

    /**
     * Whether a grapheme cluster boundary comes before [next], the next code point of the text.
     * Returns true for the first code point after construction or [reset].
     */
    fun isBoundaryBefore(next: Codepoint): Boolean {
        val codepoint = next.codepoint
        val previous = beforeCodepoint
        beforeCodepoint = codepoint
        // Fast path: ASCII after ASCII breaks unless it is CR LF. No ASCII code point takes part in
        // GB9c, GB11 or GB12/13, so the context is empty afterwards, and no table is read.
        if (codepoint in 0 until 0x80 && before != START && previous in 0 until 0x80) {
            before = ASCII
            return previous != 0x0D || codepoint != 0x0A
        }

        val after = properties(codepoint)
        val boundary = when (before) {
            START -> true
            else -> {
                if (before == ASCII) {
                    before = properties(previous)
                    conjunct = false
                    pictographic = false
                    zwjAfterPictographic = false
                    oddRegionalIndicators = false
                }
                when (pairRule(before, after)) {
                    NO_BREAK -> false
                    BREAK -> true
                    CHECK_CONJUNCT -> !conjunct
                    CHECK_EMOJI_ZWJ -> !zwjAfterPictographic
                    else -> !oddRegionalIndicators
                }
            }
        }
        val gcb = after and GCB_MASK
        conjunct = when (after and INCB_MASK) {
            INCB_LINKER -> true
            INCB_EXTEND -> conjunct
            else -> false
        }
        zwjAfterPictographic = gcb == GCB_ZWJ && pictographic
        pictographic = after and EXTENDED_PICTOGRAPHIC_BIT != 0 || pictographic && gcb == GCB_EXTEND
        oddRegionalIndicators = gcb == GCB_REGIONAL_INDICATOR && !oddRegionalIndicators
        before = after
        return boundary
    }

    /** Starts a new text: the next code point begins a cluster. */
    fun reset() {
        before = START
        beforeCodepoint = -1
        conjunct = false
        pictographic = false
        zwjAfterPictographic = false
        oddRegionalIndicators = false
    }

    private companion object {
        private const val START = -1

        /** The previous code point is ASCII and followed ASCII; its properties are read when needed. */
        private const val ASCII = -2
    }
}
