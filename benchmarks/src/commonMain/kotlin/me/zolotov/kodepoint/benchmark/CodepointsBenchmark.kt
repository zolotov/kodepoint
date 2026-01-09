package me.zolotov.kodepoint.benchmark

import kotlinx.benchmark.*
import me.zolotov.kodepoint.unicode.Codepoints

@Suppress("unused")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class CodepointsBenchmark {
    private lateinit var asciiLetters: List<Int>
    private lateinit var asciiDigits: List<Int>
    private lateinit var asciiAll: List<Int>
    private lateinit var latinExtended: List<Int>
    private lateinit var greek: List<Int>
    private lateinit var cjk: List<Int>
    private lateinit var mixedAsciiHeavy: List<Int>
    private lateinit var mixedBalanced: List<Int>
    private lateinit var mixedUnicodeHeavy: List<Int>

    @Setup
    fun setup() {
        asciiLetters = (('A'..'Z') + ('a'..'z')).map { it.code }.toList()
        asciiDigits = ('0'..'9').map { it.code }
        asciiAll = (0..127).toList()

        latinExtended = (0x0100..0x024F).toList()
        greek = (0x0370..0x03FF).toList()
        cjk = (0x4E00..0x4E7F).toList()

        // 80% ASCII, 20% non-ASCII
        mixedAsciiHeavy = buildList {
            repeat(80) { addAll(asciiLetters) }
            repeat(20) { addAll(latinExtended.take(52)) }
        }.shuffled()

        // 50% ASCII, 50% non-ASCII
        mixedBalanced = buildList {
            repeat(50) { addAll(asciiLetters) }
            repeat(50) { addAll(latinExtended.take(52)) }
        }.shuffled()

        // 20% ASCII, 80% non-ASCII
        mixedUnicodeHeavy = buildList {
            repeat(20) { addAll(asciiLetters) }
            repeat(80) { addAll(latinExtended.take(52)) }
        }.shuffled()
    }

    @Benchmark
    fun isLetterAsciiLetters(blackhole: Blackhole) {
        for (cp in asciiLetters) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterGreek(blackhole: Blackhole) {
        for (cp in greek) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterCjk(blackhole: Blackhole) {
        for (cp in cjk) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isDigitAsciiDigits(blackhole: Blackhole) {
        for (cp in asciiDigits) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun isDigitLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isDigit(cp))
        }
    }

    @Benchmark
    fun toLowerCaseAsciiUppercase(blackhole: Blackhole) {
        for (cp in asciiLetters) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toLowerCaseGreek(blackhole: Blackhole) {
        for (cp in greek) {
            blackhole.consume(Codepoints.toLowerCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseAsciiLowercase(blackhole: Blackhole) {
        for (cp in asciiLetters) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun toUpperCaseGreek(blackhole: Blackhole) {
        for (cp in greek) {
            blackhole.consume(Codepoints.toUpperCase(cp))
        }
    }

    @Benchmark
    fun isWhitespaceAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isWhitespaceLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isWhitespace(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartAsciiLetters(blackhole: Blackhole) {
        for (cp in asciiLetters) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierStartLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isJavaIdentifierStart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartAsciiLettersDigits(blackhole: Blackhole) {
        for (cp in asciiLetters) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
        for (cp in asciiDigits) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartAsciiAll(blackhole: Blackhole) {
        for (cp in asciiAll) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isJavaIdentifierPartLatinExtended(blackhole: Blackhole) {
        for (cp in latinExtended) {
            blackhole.consume(Codepoints.isJavaIdentifierPart(cp))
        }
    }

    @Benchmark
    fun isLetterMixed80AsciiHeavy(blackhole: Blackhole) {
        for (cp in mixedAsciiHeavy) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterMixed50Balanced(blackhole: Blackhole) {
        for (cp in mixedBalanced) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }

    @Benchmark
    fun isLetterMixed80UnicodeHeavy(blackhole: Blackhole) {
        for (cp in mixedUnicodeHeavy) {
            blackhole.consume(Codepoints.isLetter(cp))
        }
    }
}
