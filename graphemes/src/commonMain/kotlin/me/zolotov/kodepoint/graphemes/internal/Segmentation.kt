package me.zolotov.kodepoint.graphemes.internal

import me.zolotov.kodepoint.graphemes.generated.GraphemeData
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.EXTENDED_PICTOGRAPHIC_BIT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_CONTROL
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_CR
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_EXTEND
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_L
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_LF
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_LV
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_LVT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_MASK
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_PREPEND
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_REGIONAL_INDICATOR
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_SPACING_MARK
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_T
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_V
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.GCB_ZWJ
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_CONSONANT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_EXTEND
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_LINKER
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.INCB_MASK

/*
 * UAX #29 extended grapheme clusters, rules GB1–GB13 and GB999 as of Unicode 18.0:
 *
 *   GB3   CR × LF
 *   GB4   (Control | CR | LF) ÷
 *   GB5   ÷ (Control | CR | LF)
 *   GB6   L × (L | V | LV | LVT)
 *   GB7   (LV | V) × (V | T)
 *   GB8   (LVT | T) × T
 *   GB9   × (Extend | ZWJ)
 *   GB9a  × SpacingMark
 *   GB9b  Prepend ×
 *   GB9c  ConjunctLinker ConjunctExtender* × LinkingConsonant      (InCB=Linker, InCB=Extend, InCB=Consonant)
 *   GB11  ExtPict Extend* ZWJ × ExtPict
 *   GB12  ^ (RI RI)* RI × RI
 *   GB13  [^RI] (RI RI)* RI × RI
 *   GB999 Any ÷ Any
 *
 * [pairRule] decides every rule that only needs the two code points around a position. GB9c, GB11
 * and GB12/13 also look further back; for them it returns which context to check, and the caller
 * answers from a running state (forward) or by scanning back (at an arbitrary index).
 *
 * Forward segmentation restarts at every boundary with an empty state. That is exact because no
 * GB9c, GB11 or GB12/13 context can reach across a boundary: the chains they look back over consist
 * of characters that GB9 or GB9c themselves join (the generator checks the data for this), and a
 * boundary inside a run of regional indicators always has an even number of them before it.
 */

internal const val BREAK = 0
internal const val NO_BREAK = 1

/** GB9c: joined if the text before ends with `ConjunctLinker ConjunctExtender*`. */
internal const val CHECK_CONJUNCT = 2

/** GB11: joined if the ZWJ before the position follows `ExtPict Extend*`. */
internal const val CHECK_EMOJI_ZWJ = 3

/** GB12/13: joined if an odd number of regional indicators precedes the position. */
internal const val CHECK_REGIONAL_INDICATORS = 4

private const val CR = 0x0D
private const val LF = 0x0A
private const val ZWJ = 0x200D

internal fun properties(codepoint: Int): Int = GraphemeData.getProperties(codepoint)

/** The break status between two code points with packed properties [before] and [after]. */
internal fun pairRule(before: Int, after: Int): Int {
    val b = before and GCB_MASK
    val a = after and GCB_MASK
    // GB3, GB4, GB5
    if (b == GCB_CR) return if (a == GCB_LF) NO_BREAK else BREAK
    if (b == GCB_LF || b == GCB_CONTROL || a == GCB_CR || a == GCB_LF || a == GCB_CONTROL) return BREAK
    // GB6, GB7, GB8
    when (b) {
        GCB_L -> if (a == GCB_L || a == GCB_V || a == GCB_LV || a == GCB_LVT) return NO_BREAK
        GCB_LV, GCB_V -> if (a == GCB_V || a == GCB_T) return NO_BREAK
        GCB_LVT, GCB_T -> if (a == GCB_T) return NO_BREAK
    }
    // GB9, GB9a, GB9b
    if (a == GCB_EXTEND || a == GCB_ZWJ || a == GCB_SPACING_MARK || b == GCB_PREPEND) return NO_BREAK
    // GB9c
    if (after and INCB_MASK == INCB_CONSONANT) {
        when (before and INCB_MASK) {
            INCB_LINKER -> return NO_BREAK
            INCB_EXTEND -> return CHECK_CONJUNCT
        }
    }
    // GB11
    if (b == GCB_ZWJ && after and EXTENDED_PICTOGRAPHIC_BIT != 0) return CHECK_EMOJI_ZWJ
    // GB12, GB13
    if (b == GCB_REGIONAL_INDICATOR && a == GCB_REGIONAL_INDICATOR) return CHECK_REGIONAL_INDICATORS
    // GB999
    return BREAK
}

internal fun charCount(codepoint: Int): Int = if (codepoint >= 0x10000) 2 else 1

/** The code point starting at [index], or the lone surrogate there; like `codePointAt`, without checks. */
internal fun codepointAt(text: CharSequence, index: Int): Int {
    val high = text[index]
    if (high.isHighSurrogate() && index + 1 < text.length) {
        val low = text[index + 1]
        if (low.isLowSurrogate()) return toCodepoint(high, low)
    }
    return high.code
}

/** The code point ending at [index], or the lone surrogate there; like `codePointBefore`, without checks. */
internal fun codepointBefore(text: CharSequence, index: Int): Int {
    val low = text[index - 1]
    if (low.isLowSurrogate() && index - 2 >= 0) {
        val high = text[index - 2]
        if (high.isHighSurrogate()) return toCodepoint(high, low)
    }
    return low.code
}

private fun toCodepoint(high: Char, low: Char): Int =
    ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000

/** True if [index] splits a surrogate pair, so it is no code point boundary. */
internal fun isInsideSurrogatePair(text: CharSequence, index: Int): Boolean =
    index > 0 && index < text.length && text[index].isLowSurrogate() && text[index - 1].isHighSurrogate()

internal fun checkBoundaryIndex(text: CharSequence, index: Int) {
    if (index < 0 || index > text.length) {
        throw IndexOutOfBoundsException("Index out of range: $index, size: ${text.length}")
    }
}

/**
 * The end of the grapheme cluster starting at [start], which must be a boundary below `text.length`.
 */
@PublishedApi
internal fun nextBoundaryFromBoundary(text: CharSequence, start: Int): Int {
    val length = text.length
    var codepoint = codepointAt(text, start)
    var index = start + charCount(codepoint)
    if (index >= length) return length
    // Fast path: two ASCII code points always break, except CR LF, so no table is read.
    if (codepoint < 0x80) {
        val next = text[index].code
        if (next < 0x80) return if (codepoint == CR && next == LF) index + 1 else index
    }

    var before = properties(codepoint)
    // Suffix of the text so far matches `ConjunctLinker ConjunctExtender*` (GB9c).
    var conjunct = before and INCB_MASK == INCB_LINKER
    // Suffix matches `ExtPict Extend*` (GB11).
    var pictographic = before and EXTENDED_PICTOGRAPHIC_BIT != 0
    // Suffix matches `ExtPict Extend* ZWJ` (GB11).
    var zwjAfterPictographic = false
    // Suffix is a run of an odd number of regional indicators (GB12/13).
    var oddRegionalIndicators = before and GCB_MASK == GCB_REGIONAL_INDICATOR

    while (index < length) {
        codepoint = codepointAt(text, index)
        val after = properties(codepoint)
        val joined = when (pairRule(before, after)) {
            NO_BREAK -> true
            BREAK -> false
            CHECK_CONJUNCT -> conjunct
            CHECK_EMOJI_ZWJ -> zwjAfterPictographic
            else -> oddRegionalIndicators
        }
        if (!joined) return index

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
        index += charCount(codepoint)
    }
    return length
}

/** Whether [index], in `1 until text.length` and not inside a surrogate pair, is a boundary. */
internal fun isInteriorBoundary(text: CharSequence, index: Int): Boolean {
    val beforeCodepoint = codepointBefore(text, index)
    val afterCodepoint = codepointAt(text, index)
    if (beforeCodepoint < 0x80 && afterCodepoint < 0x80) return beforeCodepoint != CR || afterCodepoint != LF
    return when (pairRule(properties(beforeCodepoint), properties(afterCodepoint))) {
        BREAK -> true
        NO_BREAK -> false
        CHECK_CONJUNCT -> !endsWithConjunctLinker(text, index)
        // U+200D is a single UTF-16 unit.
        CHECK_EMOJI_ZWJ -> !endsWithPictographic(text, index - 1)
        else -> regionalIndicatorsBefore(text, index) % 2 == 0
    }
}

@PublishedApi
internal fun isBoundary(text: CharSequence, index: Int): Boolean {
    checkBoundaryIndex(text, index)
    if (index == 0 || index == text.length) return true
    if (isInsideSurrogatePair(text, index)) return false
    return isInteriorBoundary(text, index)
}

/** Text before [end] matches `ConjunctLinker ConjunctExtender*`. */
private fun endsWithConjunctLinker(text: CharSequence, end: Int): Boolean {
    var index = end
    while (index > 0) {
        val codepoint = codepointBefore(text, index)
        when (properties(codepoint) and INCB_MASK) {
            INCB_LINKER -> return true
            INCB_EXTEND -> index -= charCount(codepoint)
            else -> return false
        }
    }
    return false
}

/** Text before [end] matches `ExtPict Extend*`. */
private fun endsWithPictographic(text: CharSequence, end: Int): Boolean {
    var index = end
    while (index > 0) {
        val codepoint = codepointBefore(text, index)
        val props = properties(codepoint)
        if (props and EXTENDED_PICTOGRAPHIC_BIT != 0) return true
        if (props and GCB_MASK != GCB_EXTEND) return false
        index -= charCount(codepoint)
    }
    return false
}

/** Index where the run of regional indicators ending at [end] starts; [end] if there is none. */
private fun regionalIndicatorRunStart(text: CharSequence, end: Int): Int {
    var index = end
    while (index > 0) {
        val codepoint = codepointBefore(text, index)
        if (properties(codepoint) and GCB_MASK != GCB_REGIONAL_INDICATOR) break
        index -= charCount(codepoint)
    }
    return index
}

/** Regional indicators are U+1F1E6..U+1F1FF, two UTF-16 units each (checked by the generator). */
private fun regionalIndicatorsBefore(text: CharSequence, end: Int): Int =
    (end - regionalIndicatorRunStart(text, end)) / 2

private fun isRegionalIndicator(codepoint: Int): Boolean = codepoint in 0x1F1E6..0x1F1FF

/**
 * The largest boundary below [index], which must be in `1..text.length`.
 *
 * [runStartHint] is the start of the run of regional indicators before [index] when the caller knows
 * it, or -1. The result packs the boundary into the high 32 bits and the run start used, or -1,
 * into the low 32 bits, so that walking backwards over a long run of regional indicators stays
 * linear instead of rescanning the run for every flag.
 */
@PublishedApi
internal fun previousBoundaryWithHint(text: CharSequence, index: Int, runStartHint: Int): Long {
    var position = index
    var runStart = -1
    if (index >= 2 && isRegionalIndicator(codepointBefore(text, index))) {
        runStart = if (runStartHint in 0 until index) runStartHint else regionalIndicatorRunStart(text, index)
        val count = (index - runStart) / 2
        // Between two regional indicators, the position after the m-th one of the run is a boundary
        // iff m is even; the largest such position below `index` has m < count.
        val m = (count - 1) and 1.inv()
        if (m >= 2) return pack(runStart + 2 * m, runStart)
        if (runStart == 0 || isInteriorBoundary(text, runStart)) return pack(runStart, runStart)
        position = runStart
    }
    // Every code point boundary below `position`, nearest first.
    var candidate = position - 1
    while (candidate > 0) {
        if (!isInsideSurrogatePair(text, candidate) && isInteriorBoundary(text, candidate)) return pack(candidate, runStart)
        candidate--
    }
    return pack(0, runStart)
}

private fun pack(boundary: Int, runStart: Int): Long =
    (boundary.toLong() shl 32) or (runStart.toLong() and 0xFFFFFFFFL)

@PublishedApi
internal fun unpackBoundary(packed: Long): Int = (packed ushr 32).toInt()

@PublishedApi
internal fun unpackRunStart(packed: Long): Int = packed.toInt()

@PublishedApi
internal fun previousBoundary(text: CharSequence, index: Int): Int {
    checkBoundaryIndex(text, index)
    if (index == 0) return 0
    return unpackBoundary(previousBoundaryWithHint(text, index, -1))
}

@PublishedApi
internal fun nextBoundary(text: CharSequence, index: Int): Int {
    checkBoundaryIndex(text, index)
    val length = text.length
    if (index == length) return length
    // The cluster containing `index` starts at the largest boundary at or below it.
    val start = if (index == 0 || isBoundary(text, index)) index else previousBoundary(text, index)
    return nextBoundaryFromBoundary(text, start)
}
