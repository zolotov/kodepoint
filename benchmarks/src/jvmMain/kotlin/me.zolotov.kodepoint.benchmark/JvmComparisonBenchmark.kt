package me.zolotov.kodepoint.benchmark

import kotlinx.benchmark.*
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

    @Setup(Level.Trial)
    fun setup() {
        asciiAll = (0..127).toList().toIntArray()
        latinExtended = (0x0100..0x024F).toList().toIntArray()
        mixedData = asciiAll + latinExtended
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
}
