package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.Codepoint
import me.zolotov.kodepoint.graphemes.generated.GraphemeData

/** The Unicode data this module follows, on every target including the JVM. */
object Graphemes {
    /**
     * Version of the Unicode Character Database the grapheme-cluster and emoji tables were generated
     * from, e.g. `18.0.0`. Unlike `Codepoint` queries on the JVM, these tables do not follow the JDK.
     */
    const val UNICODE_VERSION: String = GraphemeData.UNICODE_VERSION
}

/**
 * The `Grapheme_Cluster_Break` property (UAX #29), which drives grapheme-cluster segmentation.
 */
enum class GraphemeClusterBreak {
    /** `Other` (XX): every code point not listed with another value. */
    OTHER,

    /** U+000D CARRIAGE RETURN. */
    CR,

    /** U+000A LINE FEED. */
    LF,

    /** Other controls, line and paragraph separators, and most format characters. */
    CONTROL,

    /** Combining marks, U+200C ZERO WIDTH NON-JOINER, emoji modifiers, tags and other extenders. */
    EXTEND,

    /** U+200D ZERO WIDTH JOINER. */
    ZWJ,

    /** U+1F1E6..U+1F1FF, the halves of flag emoji. */
    REGIONAL_INDICATOR,

    /** Characters that attach to what follows, such as U+0600 ARABIC NUMBER SIGN. */
    PREPEND,

    /** Spacing combining marks that stay in the cluster. */
    SPACING_MARK,

    /** Hangul leading consonant (choseong) jamo. */
    L,

    /** Hangul vowel (jungseong) jamo. */
    V,

    /** Hangul trailing consonant (jongseong) jamo. */
    T,

    /** Precomposed Hangul syllables of the form LV. */
    LV,

    /** Precomposed Hangul syllables of the form LVT. */
    LVT,
}

/**
 * The `Indic_Conjunct_Break` property, which rule GB9c uses to keep conjuncts such as क्ष in one cluster.
 */
enum class IndicConjunctBreak {
    /** Not part of a conjunct. */
    NONE,

    /** A virama or other conjunct linker, e.g. U+094D DEVANAGARI SIGN VIRAMA. */
    LINKER,

    /** A consonant a linker can join, e.g. U+0915 DEVANAGARI LETTER KA. */
    CONSONANT,

    /** A mark that may stand between a linker and the next consonant. */
    EXTEND,
}

private fun Codepoint.graphemeProperties(): Int = GraphemeData.getProperties(codepoint)

/** The `Grapheme_Cluster_Break` value; [GraphemeClusterBreak.OTHER] outside U+0000..U+10FFFF. */
fun Codepoint.getGraphemeClusterBreak(): GraphemeClusterBreak =
    GraphemeClusterBreak.entries[graphemeProperties() and GraphemeData.GCB_MASK]

/** The `Indic_Conjunct_Break` value; [IndicConjunctBreak.NONE] outside U+0000..U+10FFFF. */
fun Codepoint.getIndicConjunctBreak(): IndicConjunctBreak =
    IndicConjunctBreak.entries[(graphemeProperties() and GraphemeData.INCB_MASK) ushr GraphemeData.INCB_SHIFT]

/** `Extended_Pictographic`: pictographs, including the ones not yet used as emoji. */
fun Codepoint.isExtendedPictographic(): Boolean =
    graphemeProperties() and GraphemeData.EXTENDED_PICTOGRAPHIC_BIT != 0

/**
 * The UTS #51 `Emoji` property. It includes characters shown as text by default, such as the digits,
 * `#`, `*`, `©` and `®`; see [isEmojiPresentation].
 */
fun Codepoint.isEmoji(): Boolean = graphemeProperties() and GraphemeData.EMOJI_BIT != 0

/** `Emoji_Presentation`: displayed as emoji, not text, without a variation selector. */
fun Codepoint.isEmojiPresentation(): Boolean = graphemeProperties() and GraphemeData.EMOJI_PRESENTATION_BIT != 0

/** `Emoji_Modifier`: the five skin-tone modifiers U+1F3FB..U+1F3FF. */
fun Codepoint.isEmojiModifier(): Boolean = graphemeProperties() and GraphemeData.EMOJI_MODIFIER_BIT != 0

/** `Emoji_Modifier_Base`: emoji a skin-tone modifier may follow, such as 👋 U+1F44B. */
fun Codepoint.isEmojiModifierBase(): Boolean = graphemeProperties() and GraphemeData.EMOJI_MODIFIER_BASE_BIT != 0

/**
 * `Emoji_Component`: characters that appear in emoji sequences, such as keycap bases, U+FE0F,
 * U+20E3, regional indicators, skin-tone modifiers, hair components and tags.
 */
fun Codepoint.isEmojiComponent(): Boolean = graphemeProperties() and GraphemeData.EMOJI_COMPONENT_BIT != 0
