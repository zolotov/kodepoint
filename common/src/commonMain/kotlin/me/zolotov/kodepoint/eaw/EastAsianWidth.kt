package me.zolotov.kodepoint.eaw

/**
 * Unicode East Asian Width property values as defined in UAX #11.
 *
 * East Asian Width categorizes characters by their typical display width in East Asian
 * contexts, particularly for fixed-width terminal rendering where CJK characters
 * occupy two columns and Latin characters occupy one.
 *
 * @see <a href="https://www.unicode.org/reports/tr11/">UAX #11: East Asian Width</a>
 */
enum class EastAsianWidth {
    /** N — Neutral. Not specific to East Asian scripts; typically 1 column wide. */
    NEUTRAL,

    /** Na — Narrow. Typical Western narrow character (e.g. ASCII letters); 1 column wide. */
    NARROW,

    /** W — Wide. East Asian wide character (e.g. CJK ideographs, Hangul); 2 columns wide. */
    WIDE,

    /** F — Fullwidth. Compatibility fullwidth form of a Narrow character; 2 columns wide. */
    FULLWIDTH,

    /** H — Halfwidth. Compatibility halfwidth form of a Wide character; 1 column wide. */
    HALFWIDTH,

    /** A — Ambiguous. Width depends on the context (locale/font); conventionally 1 column. */
    AMBIGUOUS;

    /**
     * The number of terminal columns this character occupies.
     *
     * Returns 2 for [WIDE] and [FULLWIDTH]; 1 for all other values.
     * For [AMBIGUOUS], returns 1 (the narrow convention).
     */
    val columns: Int get() = if (this == WIDE || this == FULLWIDTH) 2 else 1
}
