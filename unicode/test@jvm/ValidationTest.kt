package me.zolotov.kodepoint.unicode

import kotlin.test.Test
import kotlin.test.fail

/**
 * JVM-only exhaustive validation tests that traverse all valid Unicode codepoints
 * and verify that the multiplatform Codepoint implementation produces the same
 * results as JVM's java.lang.Character methods.
 *
 * These tests ensure cross-platform compatibility by validating against the
 * JVM Unicode implementation.
 */
class ValidationTest {
    companion object {
        private const val MAX_CODE_POINT = 0x10FFFF
    }

    @Test
    fun isLetter() = doTest("isLetter", CodepointFunctions::isLetter, Character::isLetter)
    @Test
    fun isDigit() = doTest("isDigit", CodepointFunctions::isDigit, Character::isDigit)
    @Test
    fun isLetterOrDigit() = doTest("isLetterOrDigit", CodepointFunctions::isLetterOrDigit, Character::isLetterOrDigit)
    @Test
    fun isUpperCase() = doTest("isUpperCase", CodepointFunctions::isUpperCase, Character::isUpperCase)
    @Test
    fun isLowerCase() = doTest("isLowerCase", CodepointFunctions::isLowerCase, Character::isLowerCase)
    @Test
    fun toLowerCase() = doTest("toLowerCase", CodepointFunctions::toLowerCase, Character::toLowerCase)
    @Test
    fun toUpperCase() = doTest("toUpperCase", CodepointFunctions::toUpperCase, Character::toUpperCase)
    @Test
    fun isSpaceChar() = doTest("isSpaceChar", CodepointFunctions::isSpaceChar, Character::isSpaceChar)
    @Test
    fun isWhitespace() = doTest("isWhitespace", CodepointFunctions::isWhitespace, Character::isWhitespace)
    @Test
    fun isIdeographic() = doTest("isIdeographic", CodepointFunctions::isIdeographic, Character::isIdeographic)
    @Test
    fun isIdentifierIgnorable() = doTest("isIdentifierIgnorable", CodepointFunctions::isIdentifierIgnorable, Character::isIdentifierIgnorable)

    @Test
    fun isUnicodeIdentifierStart() = doTest(
        "isUnicodeIdentifierStart",
        CodepointFunctions::isUnicodeIdentifierStart,
        Character::isUnicodeIdentifierStart
    )

    @Test
    fun isUnicodeIdentifierPart() = doTest(
        "isUnicodeIdentifierPart",
        CodepointFunctions::isUnicodeIdentifierPart,
        Character::isUnicodeIdentifierPart
    )

    @Test
    fun isJavaIdentifierStart() =
        doTest("isJavaIdentifierStart", CodepointFunctions::isJavaIdentifierStart, Character::isJavaIdentifierStart)

    @Test
    fun isJavaIdentifierPart() =
        doTest("isJavaIdentifierPart", CodepointFunctions::isJavaIdentifierPart, Character::isJavaIdentifierPart)

    @Test
    fun isISOControl() = doTest("isISOControl", CodepointFunctions::isISOControl, Character::isISOControl)

    private data class Mismatch(
        val codepoint: Int,
        val expected: Any,
        val actual: Any
    ) {
        override fun toString(): String {
            return "U+${codepoint.toString(16).uppercase().padStart(4, '0')}: expected=$expected, actual=$actual"
        }
    }

    private fun doTest(name: String, multiplatformFn: (Int) -> Any, jvmFn: (Int) -> Any) {
        val mismatches = (0..MAX_CODE_POINT).mapNotNull { codepoint ->
            val expected = jvmFn(codepoint)
            val actual = CodepointFunctions.isLetter(codepoint)
            if (expected != actual) {
                Mismatch(codepoint, expected, actual)
            } else {
                null
            }
        }
        reportMismatches(name, mismatches)
    }

    private fun reportMismatches(name: String, mismatches: List<Mismatch>) {
        if (mismatches.isNotEmpty()) {
            val sampleSize = minOf(20, mismatches.size)
            val sample = mismatches.take(sampleSize)
            val message = buildString {
                appendLine("$name: ${mismatches.size} mismatches found")
                appendLine("First $sampleSize mismatches:")
                sample.forEach { appendLine("  $it") }
                if (mismatches.size > sampleSize) {
                    appendLine("  ... and ${mismatches.size - sampleSize} more")
                }
            }
            fail(message)
        }
    }
}