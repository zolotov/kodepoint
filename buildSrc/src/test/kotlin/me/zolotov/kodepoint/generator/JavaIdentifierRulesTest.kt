package me.zolotov.kodepoint.generator

import kotlin.test.Test
import kotlin.test.fail

/**
 * Checks that [JavaIdentifierRules], fed with the general category as reported by the running JDK,
 * reproduces `java.lang.Character` for every codepoint. Using `Character.getType` as the category
 * source keeps this test offline and independent of the UCD parser: it validates the rule itself,
 * while `ValidationTest` in `:unicode` validates the generated tables end to end.
 */
class JavaIdentifierRulesTest {

    @Test
    fun isIdentifierIgnorable() = checkAllCodepoints("isIdentifierIgnorable", Character::isIdentifierIgnorable) { cp, cat ->
        JavaIdentifierRules.isIdentifierIgnorable(cp, cat)
    }

    @Test
    fun isJavaIdentifierStart() = checkAllCodepoints("isJavaIdentifierStart", Character::isJavaIdentifierStart) { _, cat ->
        JavaIdentifierRules.isJavaIdentifierStart(cat)
    }

    @Test
    fun isJavaIdentifierPart() = checkAllCodepoints("isJavaIdentifierPart", Character::isJavaIdentifierPart) { cp, cat ->
        JavaIdentifierRules.isJavaIdentifierPart(cp, cat)
    }

    private fun checkAllCodepoints(
        name: String,
        jvm: (Int) -> Boolean,
        derived: (codepoint: Int, category: GeneralCategory) -> Boolean
    ) {
        val mismatches = (0..MAX_CODEPOINT).filter { cp ->
            jvm(cp) != derived(cp, jvmCategory(cp))
        }
        if (mismatches.isNotEmpty()) {
            val sample = mismatches.take(20).joinToString("\n") { cp ->
                "  U+%04X (%s): jvm=%s, derived=%s".format(cp, jvmCategory(cp), jvm(cp), derived(cp, jvmCategory(cp)))
            }
            fail("$name: ${mismatches.size} mismatches against java.lang.Character\n$sample")
        }
    }

    private fun jvmCategory(codepoint: Int): GeneralCategory =
        GeneralCategory.fromAbbrev(CharCategory.valueOf(Character.getType(codepoint)).code)
}
