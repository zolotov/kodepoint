package me.zolotov.kodepoint.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Blackhole
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import me.zolotov.kodepoint.graphemes.forEachEmoji
import me.zolotov.kodepoint.graphemes.forEachGraphemeCluster
import me.zolotov.kodepoint.graphemes.forEachGraphemeClusterReversed
import me.zolotov.kodepoint.graphemes.graphemeClusterCount

@Suppress("unused")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class GraphemeClusterBenchmark {
    private var asciiText = ""
    private var mixedText = ""

    @Setup
    fun setup() {
        asciiText = "The quick brown fox jumps over the lazy dog. 0123456789\r\n".repeat(16)
        // ASCII, Latin with combining marks, CJK, emoji ZWJ sequences with modifiers, and flags.
        mixedText = (
            "Hello, e\u0301te\u0301 \u4E16\u754C " +
                "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67 \uD83D\uDC4B\uD83C\uDFFD " +
                "\uD83C\uDDFA\uD83C\uDDE6\uD83C\uDDE9\uD83C\uDDEA \u0915\u094D\u0937 \uAC01!\n"
            ).repeat(16)
    }

    @Benchmark
    fun graphemeClusterCountAscii(blackhole: Blackhole) {
        blackhole.consume(asciiText.graphemeClusterCount())
    }

    @Benchmark
    fun graphemeClusterCountMixed(blackhole: Blackhole) {
        blackhole.consume(mixedText.graphemeClusterCount())
    }

    @Benchmark
    fun forEachGraphemeClusterMixed(blackhole: Blackhole) {
        var sum = 0
        mixedText.forEachGraphemeCluster { start, end -> sum += end - start }
        blackhole.consume(sum)
    }

    @Benchmark
    fun forEachGraphemeClusterReversedMixed(blackhole: Blackhole) {
        var sum = 0
        mixedText.forEachGraphemeClusterReversed { start, end -> sum += end - start }
        blackhole.consume(sum)
    }

    @Benchmark
    fun forEachEmojiMixed(blackhole: Blackhole) {
        var count = 0
        mixedText.forEachEmoji { _, _, _ -> count++ }
        blackhole.consume(count)
    }
}
