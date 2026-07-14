package me.zolotov.kodepoint.generator

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private const val BYTES_MEASURE = "bytes"
private const val LATIN1_INDEX_BYTES = 1
private const val RANGE_VALUES_PER_ENTRY = 3
private const val CHARACTER_DATA_TOTAL_BENCHMARK = "character-data-total"

data class CharacterDataSizeMetric(
    val benchmark: String,
    val bytes: Int,
)

class CharacterDataSizeMetrics(
    val metrics: List<CharacterDataSizeMetric>,
) {
    val totalBytes: Int = metrics.first { it.benchmark == CHARACTER_DATA_TOTAL_BENCHMARK }.bytes

    fun writeJson(outputPath: Path) {
        outputPath.parent?.createDirectories()
        outputPath.writeText(
            buildString {
                appendLine("{")
                metrics.forEachIndexed { index, metric ->
                    append("  ")
                    append(jsonString(metric.benchmark))
                    appendLine(": {")
                    append("    ")
                    append(jsonString(BYTES_MEASURE))
                    append(": { \"value\": ")
                    append(metric.bytes)
                    appendLine(" }")
                    append("  }")
                    if (index < metrics.lastIndex) {
                        append(',')
                    }
                    appendLine()
                }
                appendLine("}")
            }
        )
    }
}

fun buildCharacterDataSizeMetrics(
    propertyBuildResult: PropertyTableBuildResult,
    largeCaseDeltaRanges: LastCaseDeltaRanges
): CharacterDataSizeMetrics {
    // These sizes track the encoded table payload emitted by the generator, not runtime object overhead.
    val metrics = mutableListOf<CharacterDataSizeMetric>()
    metrics += CharacterDataSizeMetric(
        benchmark = "character-data-latin1",
        bytes = propertyBuildResult.latin1Properties.size * LATIN1_INDEX_BYTES
    )

    for (planeResult in propertyBuildResult.planeResults) {
        metrics += CharacterDataSizeMetric(
            benchmark = "character-data-${planeResult.plane.name.lowercase()}",
            bytes = planeResult.size
        )
    }

    metrics += CharacterDataSizeMetric(
        benchmark = "character-data-unique-property-values",
        bytes = propertyBuildResult.uniqueCharacterProperties.size * Int.SIZE_BYTES
    )
    metrics += CharacterDataSizeMetric(
        benchmark = "character-data-large-lowercase-ranges",
        bytes = largeCaseDeltaRanges.toLower.size * RANGE_VALUES_PER_ENTRY * Int.SIZE_BYTES
    )
    metrics += CharacterDataSizeMetric(
        benchmark = "character-data-large-uppercase-ranges",
        bytes = largeCaseDeltaRanges.toUpper.size * RANGE_VALUES_PER_ENTRY * Int.SIZE_BYTES
    )

    val totalBytes = metrics.sumOf { it.bytes }
    metrics += CharacterDataSizeMetric(
        benchmark = CHARACTER_DATA_TOTAL_BENCHMARK,
        bytes = totalBytes
    )

    return CharacterDataSizeMetrics(metrics)
}

private fun jsonString(value: String): String = buildString {
    append('"')
    for (char in value) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else {
                    append(char)
                }
            }
        }
    }
    append('"')
}
