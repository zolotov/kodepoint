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
import me.zolotov.kodepoint.Codepoint
import me.zolotov.kodepoint.Direction
import me.zolotov.kodepoint.appendCodePoint
import me.zolotov.kodepoint.codepoints

@Suppress("unused")
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class TextApiBenchmark {
    private var asciiCodepoint = Codepoint(0)
    private var supplementaryCodepoint = Codepoint(0)
    private var mixedText = ""

    @Setup
    fun setup() {
        asciiCodepoint = Codepoint('A'.code)
        supplementaryCodepoint = Codepoint(0x1F600)
        mixedText = "Hi \uD83D\uDE00 Καλημέρα \uD83D\uDC4D 123"
    }

    @Benchmark
    fun asStringAscii(blackhole: Blackhole) {
        repeat(64) {
            blackhole.consume(asciiCodepoint.asString())
        }
    }

    @Benchmark
    fun asStringSupplementary(blackhole: Blackhole) {
        repeat(64) {
            blackhole.consume(supplementaryCodepoint.asString())
        }
    }

    @Benchmark
    fun appendCodePoint(blackhole: Blackhole) {
        val sb = StringBuilder()
        repeat(64) {
            sb.appendCodePoint(asciiCodepoint)
            sb.appendCodePoint(supplementaryCodepoint)
        }
        blackhole.consume(sb.length)
    }

    @Benchmark
    fun codepointsForward(blackhole: Blackhole) {
        repeat(32) {
            val iterator = mixedText.codepoints(0, Direction.FORWARD)
            while (iterator.hasNext()) {
                blackhole.consume(iterator.next().codepoint)
            }
        }
    }

    @Benchmark
    fun codepointsBackward(blackhole: Blackhole) {
        repeat(32) {
            val iterator = mixedText.codepoints(mixedText.length, Direction.BACKWARD)
            while (iterator.hasNext()) {
                blackhole.consume(iterator.next().codepoint)
            }
        }
    }
}
