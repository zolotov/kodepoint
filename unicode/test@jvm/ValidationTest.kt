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
        private const val VERTICAL_TILDA = 0x2E2F

        private val WHITESPACE_JVM_ONLY = setOf(0x001C, 0x001D, 0x001E, 0x001F)
        private val WHITESPACE_UNICODE_ONLY = setOf(0x0085, 0x00A0, 0x2007, 0x202F)
    }

    @Test
    fun isLetter() = doTest("isLetter", Codepoints::isLetter, Character::isLetter)
    @Test
    fun isDigit() = doTest("isDigit", Codepoints::isDigit, Character::isDigit)
    @Test
    fun isLetterOrDigit() = doTest("isLetterOrDigit", Codepoints::isLetterOrDigit, Character::isLetterOrDigit)
    @Test
    fun isUpperCase() = doTest("isUpperCase", Codepoints::isUpperCase, Character::isUpperCase)
    @Test
    fun isLowerCase() = doTest("isLowerCase", Codepoints::isLowerCase, Character::isLowerCase)
    @Test
    fun toLowerCase() = doTest("toLowerCase", Codepoints::toLowerCase, Character::toLowerCase)
    @Test
    fun toUpperCase() = doTest("toUpperCase", Codepoints::toUpperCase, Character::toUpperCase)
    @Test
    fun isSpaceChar() = doTest("isSpaceChar", Codepoints::isSpaceChar, Character::isSpaceChar)

    @Test
    fun isWhitespace() = doTest(
        "isWhitespace",
        // Unicode White_Space includes some chars that Java doesn't consider whitespace
        { Codepoints.isWhitespace(it) || it in WHITESPACE_JVM_ONLY },
        // Java's isWhitespace includes some chars that Unicode doesn't have in White_Space
        { Character.isWhitespace(it) || it in WHITESPACE_UNICODE_ONLY }
    )

    @Test
    fun isIdeographic() = doTest("isIdeographic", Codepoints::isIdeographic, Character::isIdeographic)
    @Test
    fun isIdentifierIgnorable() =
        doTest("isIdentifierIgnorable", Codepoints::isIdentifierIgnorable, Character::isIdentifierIgnorable)

    @Test
    fun isUnicodeIdentifierStart() = doTest(
        "isUnicodeIdentifierStart",
        { Codepoints.isUnicodeIdentifierStart(it) || it == VERTICAL_TILDA }, // `VERTICAL_TILDA` is added to JVM for backward compatibility
        Character::isUnicodeIdentifierStart
    )

    @Test
    fun isUnicodeIdentifierPart() = doTest(
        "isUnicodeIdentifierPart",
        { Codepoints.isUnicodeIdentifierPart(it) || Codepoints.isIdentifierIgnorable(it) || it == VERTICAL_TILDA }, // `ignorable` and `VERTICAL_TILDA` added to JVM for backward compatibility
        Character::isUnicodeIdentifierPart
    )

    @Test
    fun isJavaIdentifierStart() =
        doTest("isJavaIdentifierStart", Codepoints::isJavaIdentifierStart, Character::isJavaIdentifierStart)

    @Test
    fun isJavaIdentifierPart() =
        doTest("isJavaIdentifierPart", Codepoints::isJavaIdentifierPart, Character::isJavaIdentifierPart)

    @Test
    fun isISOControl() = doTest("isISOControl", Codepoints::isISOControl, Character::isISOControl)

    private data class Mismatch(
        val codepoint: Int,
        val jvm: Any,
        val multiplatform: Any
    ) {
        override fun toString(): String {
            val codepoint = "U+${codepoint.toString(16).uppercase().padStart(4, '0')}"
            return "$codepoint [https://www.compart.com/en/unicode/$codepoint]: jvm=$jvm, multiplatform=$multiplatform"
        }
    }

    private fun doTest(name: String, multiplatformFn: (Int) -> Any, jvmFn: (Int) -> Any) {
        val mismatches = (0..MAX_CODE_POINT).mapNotNull { codepoint ->
            val jvm = jvmFn(codepoint)
            val multiplatform = multiplatformFn(codepoint)
            if (jvm != multiplatform) {
                Mismatch(codepoint, jvm, multiplatform)
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