package me.zolotov.kodepoint.benchmark

import kotlinx.benchmark.*
import me.zolotov.kodepoint.Codepoint
import me.zolotov.kodepoint.unicode.Codepoints
import org.openjdk.jmh.annotations.Level

@Suppress("unused")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class JvmComparisonBenchmark {
    private lateinit var asciiAll: IntArray
    private lateinit var latinExtended: IntArray
    private lateinit var mixedData: IntArray
    private lateinit var supplementaryMixed: IntArray

    @Setup(Level.Trial)
    fun setup() {
        asciiAll = (0..127).toList().toIntArray()
        latinExtended = (0x0100..0x024F).toList().toIntArray()
        mixedData = asciiAll + latinExtended
        supplementaryMixed = asciiAll + latinExtended + (0x1F300..0x1F3FF).toList().toIntArray()
    }

    @Benchmark
    fun isLetterAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isLetter(cp))
        }
    }

    @Benchmark
    fun toLowerCaseAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.toUpperCase(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isWhitespaceAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isWhitespaceAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isWhitespaceLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isWhitespaceLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isDigitAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isLetterOrDigitMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isLetterOrDigit(cp))
        }
    }

    @Benchmark
    fun isUpperCaseAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isUpperCaseAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isUpperCaseLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isUpperCaseLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isUpperCaseMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isUpperCaseMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isUpperCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isLowerCaseMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isLowerCase(cp))
        }
    }

    @Benchmark
    fun isSpaceCharAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isSpaceCharAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isSpaceCharLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isSpaceCharLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isSpaceCharMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isSpaceCharMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isSpaceChar(cp))
        }
    }

    @Benchmark
    fun isIdeographicAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdeographicAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdeographicLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdeographicLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdeographicMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdeographicMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isIdeographic(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isIdentifierIgnorableMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isIdentifierIgnorable(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierStartMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isUnicodeIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isUnicodeIdentifierPartMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isUnicodeIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isISOControlAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isISOControl(cp))
        }
    }

    @Benchmark
    fun isISOControlAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.isISOControl(cp))
        }
    }

    @Benchmark
    fun isISOControlLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isISOControl(cp))
        }
    }

    @Benchmark
    fun isISOControlLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.isISOControl(cp))
        }
    }

    @Benchmark
    fun isISOControlMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.isISOControl(cp))
        }
    }

    @Benchmark
    fun isISOControlMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.isISOControl(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptAsciiCodepoints(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.getUnicodeScript(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptAsciiJvm(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Character.UnicodeScript.of(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptLatinExtendedCodepoints(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.getUnicodeScript(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptLatinExtendedJvm(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Character.UnicodeScript.of(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptMixedCodepoints(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Codepoints.getUnicodeScript(cp))
        }
    }

    @Benchmark
    fun getUnicodeScriptMixedJvm(blackhole: Blackhole) {
        for (cp in mixedData) {
            blackhole.consume(Character.UnicodeScript.of(cp))
        }
    }

    @Benchmark
    fun charCountSupplementaryMixedCodepoints(blackhole: Blackhole) {
        for (cp in supplementaryMixed) {
            blackhole.consume(Codepoint(cp).charCount)
        }
    }

    @Benchmark
    fun charCountSupplementaryMixedJvm(blackhole: Blackhole) {
        for (cp in supplementaryMixed) {
            blackhole.consume(Character.charCount(cp))
        }
    }
}
