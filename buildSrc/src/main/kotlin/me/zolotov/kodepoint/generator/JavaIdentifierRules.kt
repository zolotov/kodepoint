package me.zolotov.kodepoint.generator

/**
 * Java identifier rules derived from Unicode data, following the contract of
 * `java.lang.Character.isJavaIdentifierStart`, `isJavaIdentifierPart` and `isIdentifierIgnorable`.
 *
 * These are computed from the parsed UCD rather than queried from the build JDK's `Character`
 * so that generated tables reflect the Unicode version being generated, not whichever version
 * the JDK running the build happens to implement.
 */
object JavaIdentifierRules {

    /**
     * `Character.isIdentifierIgnorable`: ISO controls that are not whitespace, plus all FORMAT characters.
     */
    fun isIdentifierIgnorable(codepoint: Int, category: GeneralCategory): Boolean =
        codepoint in 0x0000..0x0008 ||
            codepoint in 0x000E..0x001B ||
            codepoint in 0x007F..0x009F ||
            category == GeneralCategory.Cf

    /**
     * `Character.isJavaIdentifierStart`: a letter, a letter number, a currency symbol, or connector punctuation.
     */
    fun isJavaIdentifierStart(category: GeneralCategory): Boolean = when (category) {
        GeneralCategory.Lu, GeneralCategory.Ll, GeneralCategory.Lt, GeneralCategory.Lm, GeneralCategory.Lo,
        GeneralCategory.Nl,
        GeneralCategory.Sc,
        GeneralCategory.Pc -> true

        else -> false
    }

    /**
     * `Character.isJavaIdentifierPart`: everything accepted by [isJavaIdentifierStart], plus decimal digits,
     * combining spacing marks, non-spacing marks and identifier-ignorable characters.
     */
    fun isJavaIdentifierPart(codepoint: Int, category: GeneralCategory): Boolean = when (category) {
        GeneralCategory.Nd,
        GeneralCategory.Mc,
        GeneralCategory.Mn -> true

        else -> isJavaIdentifierStart(category) || isIdentifierIgnorable(codepoint, category)
    }
}
