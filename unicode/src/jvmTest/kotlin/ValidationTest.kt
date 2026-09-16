package me.zolotov.kodepoint.unicode

import kotlin.test.Test
import kotlin.test.fail

/**
 * JVM-only exhaustive validation tests that traverse all valid Unicode codepoints
 * and compare the multiplatform Codepoint implementation with JVM's java.lang.Character.
 *
 * The tables and the JDK may implement different Unicode releases. The build writes the
 * UCD delta between them (`ucd-diff.txt`, from `kodepoint.jvmUnicodeVersion` to
 * `kodepoint.unicodeVersion`, with the value of each release at every changed codepoint),
 * and every test requires its mismatches to be exactly that delta: nothing the delta doesn't
 * explain, nothing in the delta the tables missed, and at each changed codepoint the JVM
 * returning the old release's value and the tables the new one's. When both versions are
 * equal the delta is empty and the comparison is strict.
 */
class ValidationTest {
    @OptIn(ExperimentalStdlibApi::class)
    companion object {
        private const val MAX_CODE_POINT = 0x10FFFF
        private const val VERTICAL_TILDA = 0x2E2F

        private val WHITESPACE_JVM_ONLY = setOf(0x001C, 0x001D, 0x001E, 0x001F)
        private val WHITESPACE_UNICODE_ONLY = setOf(0x0085, 0x00A0, 0x2007, 0x202F)

        private val CODEPOINT_HEX = HexFormat {
            upperCase = true
            number {
                removeLeadingZeros = true
                minLength = 4
            }
        }

        private fun formatCodepoint(codepoint: Int): String {
            val hex = "U+" + codepoint.toHexString(CODEPOINT_HEX)
            return "$hex [https://www.compart.com/en/unicode/$hex]"
        }

        /** Renders an observed result the way the generator renders UCD values in `ucd-diff.txt`. */
        private fun canonical(value: Any): String =
            if (value is Int) value.toHexString(CODEPOINT_HEX) else value.toString()
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

    @Test
    fun getScript() = doTest(
        "getScript",
        { Codepoints.getUnicodeScript(it).name },
        { Character.UnicodeScript.of(it).name }
    )

    @Test
    fun getCategory() = doTest(
        "getCategory",
        { Codepoints.getCategory(it).code },
        { CharCategory.valueOf(Character.getType(it)).code }
    )

    /** The value a changed codepoint has in the JDK's release ([jvm]) and in the tables' release ([multiplatform]). */
    private data class ExpectedChange(val jvm: String, val multiplatform: String)

    /** Per test name and codepoint, the change between the JDK's and the tables' Unicode release. */
    private val expectedDiff: Map<String, Map<Int, ExpectedChange>>
    private val diffDescription: String

    init {
        val lines = checkNotNull(ValidationTest::class.java.getResourceAsStream("/ucd-diff.txt")) {
            "ucd-diff.txt missing from test resources; run :unicode:generateUcdDiff"
        }.bufferedReader().readLines()
        diffDescription = lines.first().removePrefix("# ")
        expectedDiff = lines
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.split(' ') }
            .groupBy({ it[0] }, { it[1].toInt(16) to ExpectedChange(jvm = it[2], multiplatform = it[3]) })
            .mapValues { it.value.toMap() }
    }

    private data class Mismatch(
        val codepoint: Int,
        val jvm: Any,
        val multiplatform: Any
    ) {
        override fun toString(): String = "${formatCodepoint(codepoint)}: jvm=$jvm, multiplatform=$multiplatform"
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
        val expected = expectedDiff[name].orEmpty()
        val observed = mismatches.associateBy { it.codepoint }
        val unexpected = mismatches.filter { it.codepoint !in expected }
        val missing = expected.keys - observed.keys
        // A changed codepoint may disagree with the JVM and still be wrong: the tables must hold the
        // new release's value, and the JVM must hold the old one (else jvmUnicodeVersion is misdeclared).
        val wrongTables = expected.mapNotNull { (cp, change) ->
            observed[cp]?.takeIf { canonical(it.multiplatform) != change.multiplatform }?.let { it to change }
        }
        val wrongJvm = expected.mapNotNull { (cp, change) ->
            observed[cp]?.takeIf { canonical(it.jvm) != change.jvm }?.let { it to change }
        }

        val message = buildString {
            if (unexpected.isNotEmpty()) {
                appendLine("$name: ${unexpected.size} mismatches not explained by the $diffDescription")
                appendSample(unexpected) { it.toString() }
            }
            if (missing.isNotEmpty()) {
                appendLine("$name: ${missing.size} codepoints changed in the $diffDescription but the tables agree with the JVM")
                appendSample(missing.sorted()) { formatCodepoint(it) }
            }
            if (wrongTables.isNotEmpty()) {
                appendLine("$name: ${wrongTables.size} codepoints where the tables disagree with the newer UCD release of the $diffDescription")
                appendSample(wrongTables) { (m, change) -> "${formatCodepoint(m.codepoint)}: expected ${change.multiplatform}, multiplatform=${canonical(m.multiplatform)}" }
            }
            if (wrongJvm.isNotEmpty()) {
                appendLine("$name: ${wrongJvm.size} codepoints where the JVM disagrees with the older UCD release of the $diffDescription (is kodepoint.jvmUnicodeVersion right?)")
                appendSample(wrongJvm) { (m, change) -> "${formatCodepoint(m.codepoint)}: expected ${change.jvm}, jvm=${canonical(m.jvm)}" }
            }
        }
        if (message.isNotEmpty()) {
            fail(message)
        }
    }

    private fun <T> StringBuilder.appendSample(items: List<T>, format: (T) -> String) {
        val sampleSize = minOf(20, items.size)
        appendLine("First $sampleSize:")
        items.take(sampleSize).forEach { appendLine("  ${format(it)}") }
        if (items.size > sampleSize) {
            appendLine("  ... and ${items.size - sampleSize} more")
        }
    }
}
