package me.zolotov.kodepoint.gradle

import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Aggregates kotlinx-benchmark reports and CharacterData size metrics into a single
 * normalized JSON schema, compares the run against the last published baseline,
 * maintains a bounded run history, and assembles a static GitHub Pages bundle
 * (template assets plus generated JSON payloads and `data/data.js`).
 *
 * Outputs under [outputDirectory]:
 * - `raw/` — untouched copies of the input reports
 * - `report/` — `current.json`, `comparison.json`, `history.json`, `summary.md`
 * - `site/` — the deployable Pages bundle
 */
abstract class BenchmarkReportTask : DefaultTask() {
    @get:InputDirectory
    abstract val benchmarkReportsDirectory: DirectoryProperty

    @get:InputFile
    abstract val characterDataMetricsFile: RegularFileProperty

    @get:InputDirectory
    abstract val siteTemplateDirectory: DirectoryProperty

    @get:Optional
    @get:InputFile
    abstract val historyFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val siteUrl: Property<String>

    @get:Input
    abstract val historyLimit: Property<Int>

    /**
     * Fallback significance threshold for throughput measurements that lack confidence
     * intervals. When both the current and baseline measurements carry a JMH confidence
     * interval, significance is decided by interval overlap instead. Deterministic
     * suites (size metrics) always compare exactly.
     */
    @get:Input
    abstract val significanceThreshold: Property<Double>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        historyLimit.convention(90)
        significanceThreshold.convention(0.03)
    }

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.asFile.get().apply {
            deleteRecursively()
            mkdirs()
        }
        val rawDir = outputDir.resolve("raw").apply(File::mkdirs)
        val reportsDir = outputDir.resolve("report").apply(File::mkdirs)
        val siteDir = outputDir.resolve("site")

        val jvmInput = findLatestBenchmarkReport("jvm")
        val wasmInput = findLatestBenchmarkReport("wasmJs")
        val characterDataInput = characterDataMetricsFile.asFile.get()

        val jvmOutput = rawDir.resolve("jvm.json")
        val wasmOutput = rawDir.resolve("wasmJs.json")
        val characterDataOutput = rawDir.resolve("character-data.json")
        jvmInput.copyTo(jvmOutput, overwrite = true)
        wasmInput.copyTo(wasmOutput, overwrite = true)
        characterDataInput.copyTo(characterDataOutput, overwrite = true)

        val currentRun = buildCurrentRun(jvmOutput, wasmOutput, characterDataOutput)
        val existingHistory = readExistingHistory()
        val baselineRun = existingHistory.runs.lastOrNull()
        val comparison = compareRuns(currentRun, baselineRun)
        val updatedHistory = mergeHistory(existingHistory, currentRun)

        val currentJson = currentRun.toJson()
        val comparisonJson = comparison.toJson(currentRun, baselineRun)
        val historyJson = updatedHistory.toJson()
        val summaryMarkdown = renderSummaryMarkdown(currentRun, baselineRun, comparison)

        reportsDir.resolve("current.json").writeText(json.encodeToString(JsonObject.serializer(), currentJson) + "\n")
        reportsDir.resolve("comparison.json").writeText(json.encodeToString(JsonObject.serializer(), comparisonJson) + "\n")
        reportsDir.resolve("history.json").writeText(json.encodeToString(JsonObject.serializer(), historyJson) + "\n")
        reportsDir.resolve("summary.md").writeText(summaryMarkdown)

        siteTemplateDirectory.asFile.get().copyRecursively(siteDir, overwrite = true)
        val siteDataDir = siteDir.resolve("data").apply(File::mkdirs)
        siteDataDir.resolve("latest.json").writeText(json.encodeToString(JsonObject.serializer(), currentJson) + "\n")
        siteDataDir.resolve("comparison.json").writeText(json.encodeToString(JsonObject.serializer(), comparisonJson) + "\n")
        siteDataDir.resolve("history.json").writeText(json.encodeToString(JsonObject.serializer(), historyJson) + "\n")
        // data.js lets the dashboard work over file:// where fetch() of local JSON is blocked.
        siteDataDir.resolve("data.js").writeText(renderDataJs(currentJson, comparisonJson, historyJson))
        siteDir.resolve(".nojekyll").writeText("\n")

        publishGitHubSummary(summaryMarkdown)
    }

    private fun renderDataJs(current: JsonObject, comparison: JsonObject, history: JsonObject): String {
        val payload = buildJsonObject {
            put("latest", current)
            put("comparison", comparison)
            put("history", history)
        }
        return "window.BENCHMARK_DATA = " + compactJson.encodeToString(JsonObject.serializer(), payload) + ";\n"
    }

    private fun publishGitHubSummary(summaryMarkdown: String) {
        val summaryPath = System.getenv("GITHUB_STEP_SUMMARY").orEmpty().trim()
        if (summaryPath.isEmpty()) return
        File(summaryPath).appendText(summaryMarkdown)
    }

    private fun readExistingHistory(): BenchmarkHistory {
        val candidate = historyFile.orNull?.asFile?.takeIf(File::isFile) ?: return BenchmarkHistory(emptyList())
        val root = json.parseToJsonElement(candidate.readText()).jsonObject
        if (root["schemaVersion"]?.jsonPrimitive?.intOrNull != HISTORY_SCHEMA_VERSION) {
            error("Unsupported benchmark history schema in ${candidate.path}")
        }
        val runs = root.jsonArrayOrEmpty("runs").map { runElement ->
            parseRun(runElement.jsonObject)
        }
        return BenchmarkHistory(runs)
    }

    private fun parseRun(objectValue: JsonObject): BenchmarkRun {
        val metadata = RunMetadata(
            generatedAt = objectValue.string("generatedAt"),
            repository = objectValue.stringOrNull("repository"),
            eventName = objectValue.stringOrNull("eventName"),
            refName = objectValue.stringOrNull("refName"),
            commitSha = objectValue.stringOrNull("commitSha"),
            runId = objectValue.stringOrNull("runId"),
            runAttempt = objectValue.stringOrNull("runAttempt"),
            runNumber = objectValue.stringOrNull("runNumber"),
            runUrl = objectValue.stringOrNull("runUrl"),
            commitUrl = objectValue.stringOrNull("commitUrl"),
            siteUrl = objectValue.stringOrNull("siteUrl"),
            runnerName = objectValue.stringOrNull("runnerName"),
            runnerOs = objectValue.stringOrNull("runnerOs")
        )
        val measurements = objectValue.jsonArrayOrEmpty("measurements").map { measurementElement ->
            parseMeasurement(measurementElement.jsonObject)
        }
        return BenchmarkRun(metadata, measurements)
    }

    private fun parseMeasurement(objectValue: JsonObject): Measurement =
        Measurement(
            suite = objectValue.string("suite"),
            name = objectValue.string("name"),
            displayName = objectValue.string("displayName"),
            group = objectValue.string("group"),
            unit = objectValue.string("unit"),
            value = objectValue.double("value"),
            lowerValue = objectValue.doubleOrNull("lowerValue"),
            upperValue = objectValue.doubleOrNull("upperValue"),
            biggerIsBetter = objectValue.boolean("biggerIsBetter")
        )

    private fun buildCurrentRun(
        jvmReportFile: File,
        wasmReportFile: File,
        characterDataReportFile: File
    ): BenchmarkRun {
        val metadata = RunMetadata(
            generatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            repository = readEnv("GITHUB_REPOSITORY"),
            eventName = readEnv("GITHUB_EVENT_NAME"),
            refName = readEnv("GITHUB_HEAD_REF") ?: readEnv("GITHUB_REF_NAME"),
            commitSha = readEnv("GITHUB_SHA"),
            runId = readEnv("GITHUB_RUN_ID"),
            runAttempt = readEnv("GITHUB_RUN_ATTEMPT"),
            runNumber = readEnv("GITHUB_RUN_NUMBER"),
            runUrl = githubRunUrl(),
            commitUrl = githubCommitUrl(),
            siteUrl = siteUrl.orNull?.ifBlank { null },
            runnerName = readEnv("RUNNER_NAME"),
            runnerOs = readEnv("RUNNER_OS")
        )

        val measurements = buildList {
            addAll(parseThroughputReport(jvmReportFile, suite = "jvm"))
            addAll(parseThroughputReport(wasmReportFile, suite = "wasmJs"))
            addAll(parseCharacterDataReport(characterDataReportFile))
        }.sortedWith(compareBy<Measurement>({ suiteSortIndex(it.suite) }, { it.displayName }))

        return BenchmarkRun(metadata, measurements)
    }

    private fun parseThroughputReport(reportFile: File, suite: String): List<Measurement> {
        val entries = json.parseToJsonElement(reportFile.readText()).jsonArray
        return entries.map { entry ->
            val objectValue = entry.jsonObject
            val rawName = objectValue.string("benchmark")
            val metric = objectValue.requiredObject("primaryMetric")
            val confidence = metric["scoreConfidence"]?.jsonArray?.mapNotNull { it.jsonPrimitive.doubleOrNull }.orEmpty()
            Measurement(
                suite = suite,
                name = rawName,
                displayName = rawName.removePrefix(BENCHMARK_PACKAGE_PREFIX),
                group = rawName.removePrefix(BENCHMARK_PACKAGE_PREFIX).substringBefore('.'),
                unit = metric.string("scoreUnit"),
                value = metric.double("score"),
                lowerValue = confidence.getOrNull(0),
                upperValue = confidence.getOrNull(1),
                biggerIsBetter = true
            )
        }
    }

    private fun parseCharacterDataReport(reportFile: File): List<Measurement> {
        val root = json.parseToJsonElement(reportFile.readText()).jsonObject
        return root.entries.sortedBy { it.key }.map { (name, metricElement) ->
            val metricName = metricElement.jsonObject.entries.firstOrNull()
                ?: error("Character data metric '$name' is empty.")
            val metricValue = metricName.value.jsonObject.double("value")
            Measurement(
                suite = "character-data",
                name = name,
                displayName = name,
                group = "CharacterData",
                unit = metricName.key,
                value = metricValue,
                lowerValue = null,
                upperValue = null,
                biggerIsBetter = false
            )
        }
    }

    private fun criterionFor(measurement: Measurement, baseline: Measurement?): SignificanceCriterion =
        when {
            measurement.suite in DETERMINISTIC_SUITES -> SignificanceCriterion.EXACT
            baseline != null &&
                measurement.lowerValue != null && measurement.upperValue != null &&
                baseline.lowerValue != null && baseline.upperValue != null -> SignificanceCriterion.CONFIDENCE_INTERVAL
            else -> SignificanceCriterion.THRESHOLD
        }

    private fun isSignificant(
        criterion: SignificanceCriterion,
        measurement: Measurement,
        baseline: Measurement,
        deltaRatio: Double?
    ): Boolean = when (criterion) {
        SignificanceCriterion.EXACT -> measurement.value != baseline.value
        // JMH reports a 99.9% confidence interval per benchmark; a change is real
        // only when the current and baseline intervals do not overlap.
        SignificanceCriterion.CONFIDENCE_INTERVAL ->
            measurement.lowerValue!! > baseline.upperValue!! || measurement.upperValue!! < baseline.lowerValue!!
        SignificanceCriterion.THRESHOLD ->
            deltaRatio != null && abs(deltaRatio) >= significanceThreshold.get()
    }

    private fun compareRuns(currentRun: BenchmarkRun, baselineRun: BenchmarkRun?): List<ComparisonEntry> {
        val baselineByKey = baselineRun?.measurements?.associateBy(Measurement::key).orEmpty()
        return currentRun.measurements.map { measurement ->
            val baseline = baselineByKey[measurement.key]
            val delta = baseline?.let { measurement.value - it.value }
            val deltaRatio = baseline
                ?.takeIf { it.value != 0.0 }
                ?.let { (measurement.value - it.value) / it.value }
            val criterion = criterionFor(measurement, baseline)
            val change = when {
                baseline == null -> ChangeType.NEW
                deltaRatio == null || !isSignificant(criterion, measurement, baseline, deltaRatio) -> ChangeType.UNCHANGED
                measurement.biggerIsBetter == (deltaRatio > 0) -> ChangeType.IMPROVEMENT
                else -> ChangeType.REGRESSION
            }
            ComparisonEntry(
                current = measurement,
                baseline = baseline,
                delta = delta,
                deltaRatio = deltaRatio,
                criterion = criterion,
                change = change
            )
        }.sortedWith(compareBy<ComparisonEntry>({ suiteSortIndex(it.current.suite) }, { it.current.displayName }))
    }

    private fun mergeHistory(existingHistory: BenchmarkHistory, currentRun: BenchmarkRun): BenchmarkHistory {
        val deduplicated = existingHistory.runs.filterNot { existingRun ->
            existingRun.metadata.commitSha != null &&
                existingRun.metadata.commitSha == currentRun.metadata.commitSha &&
                existingRun.metadata.refName == currentRun.metadata.refName &&
                existingRun.metadata.eventName == currentRun.metadata.eventName
        }
        return BenchmarkHistory((deduplicated + currentRun).takeLast(max(1, historyLimit.get())))
    }

    private fun renderSummaryMarkdown(
        currentRun: BenchmarkRun,
        baselineRun: BenchmarkRun?,
        comparison: List<ComparisonEntry>
    ): String {
        val regressions = comparison.topEntries(ChangeType.REGRESSION)
        val improvements = comparison.topEntries(ChangeType.IMPROVEMENT)
        val siteLink = currentRun.metadata.siteUrl?.let { "- Site: [$it]($it)\n" }.orEmpty()
        val baselineLine = baselineRun?.let {
            val baselineRef = listOfNotNull(it.metadata.refName, it.metadata.commitSha?.take(7)).joinToString(" @ ")
            "- Baseline: `${baselineRef.ifBlank { "latest published run" }}`\n"
        } ?: "- Baseline: none yet; this run created the first snapshot.\n"
        val fallbackPercent = formatNumber("%.1f", significanceThreshold.get() * 100)

        return buildString {
            appendLine("# Benchmark Report")
            appendLine()
            appendLine("- Generated: `${currentRun.metadata.generatedAt}`")
            appendLine("- Measurements: `${currentRun.measurements.size}` across `${currentRun.measurements.map(Measurement::suite).distinct().size}` suites")
            append(baselineLine)
            appendLine("- Significance: per-benchmark 99.9% confidence intervals (±$fallbackPercent% fallback); size metrics compare exactly")
            append(siteLink)
            currentRun.metadata.runUrl?.let { appendLine("- Workflow run: [$it]($it)") }
            currentRun.metadata.commitUrl?.let { appendLine("- Commit: [$it]($it)") }
            appendLine()

            appendLine("## Largest Regressions")
            appendLine()
            append(renderChangeTable(regressions, emptyLabel = "No significant regressions relative to the baseline.\n"))
            appendLine()

            appendLine("## Largest Improvements")
            appendLine()
            append(renderChangeTable(improvements, emptyLabel = "No significant improvements relative to the baseline.\n"))
            appendLine()

            appendLine("## Suite Coverage")
            appendLine()
            appendLine("| Suite | Measurements |")
            appendLine("| --- | ---: |")
            currentRun.measurements.groupBy(Measurement::suite).toSortedMap(compareBy(::suiteSortIndex)).forEach { (suite, entries) ->
                appendLine("| `${suite}` | ${entries.size} |")
            }
        }
    }

    private fun renderChangeTable(entries: List<ComparisonEntry>, emptyLabel: String): String =
        if (entries.isEmpty()) {
            emptyLabel
        } else {
            buildString {
                appendLine("| Suite | Benchmark | Current | Baseline | Delta |")
                appendLine("| --- | --- | ---: | ---: | ---: |")
                entries.forEach { entry ->
                    appendLine(
                        "| `${entry.current.suite}` | `${entry.current.displayName}` | ${formatValue(entry.current)} | " +
                            "${entry.baseline?.let(::formatValue) ?: "new"} | ${formatDelta(entry)} |"
                    )
                }
            }
        }

    private fun findLatestBenchmarkReport(target: String): File {
        val candidates = benchmarkReportsDirectory.asFileTree.matching {
            include("quick/**/$target.json")
        }.files
        return candidates.maxByOrNull { it.parentFile.name }
            ?: error("Could not find a quick benchmark report for '$target' under ${benchmarkReportsDirectory.asFile.get().path}")
    }

    private fun githubRunUrl(): String? {
        val serverUrl = readEnv("GITHUB_SERVER_URL")
        val repository = readEnv("GITHUB_REPOSITORY")
        val runId = readEnv("GITHUB_RUN_ID")
        return if (serverUrl != null && repository != null && runId != null) {
            "$serverUrl/$repository/actions/runs/$runId"
        } else {
            null
        }
    }

    private fun githubCommitUrl(): String? {
        val serverUrl = readEnv("GITHUB_SERVER_URL")
        val repository = readEnv("GITHUB_REPOSITORY")
        val commitSha = readEnv("GITHUB_SHA")
        return if (serverUrl != null && repository != null && commitSha != null) {
            "$serverUrl/$repository/commit/$commitSha"
        } else {
            null
        }
    }

    private fun readEnv(name: String): String? =
        System.getenv(name)?.trim()?.takeIf(String::isNotEmpty)

    private fun formatValue(measurement: Measurement): String {
        val formattedValue = when (measurement.unit) {
            "bytes" -> formatNumber("%,.0f", measurement.value)
            else -> when {
                abs(measurement.value) >= 100 -> formatNumber("%,.2f", measurement.value)
                abs(measurement.value) >= 10 -> formatNumber("%,.3f", measurement.value)
                abs(measurement.value) >= 1 -> formatNumber("%,.4f", measurement.value)
                else -> formatNumber("%,.5f", measurement.value)
            }
        }
        return "$formattedValue ${measurement.unit}"
    }

    private fun formatDelta(entry: ComparisonEntry): String =
        when {
            entry.baseline == null -> "new"
            entry.deltaRatio == null -> "n/a"
            abs(entry.deltaRatio) < DISPLAY_ZERO_EPSILON -> "0.00%"
            entry.change == ChangeType.UNCHANGED -> formatNumber("%+.2f", entry.deltaRatio * 100) + "% (noise)"
            else -> formatNumber("%+.2f", entry.deltaRatio * 100) + "%"
        }

    private fun suiteSortIndex(suite: String): Int =
        when (suite) {
            "jvm" -> 0
            "wasmJs" -> 1
            "character-data" -> 2
            else -> 99
        }

    private fun buildComparisonEntriesJson(entries: List<ComparisonEntry>): JsonArray = buildJsonArray {
        entries.forEach { entry ->
            add(entry.toJson())
        }
    }

    private fun BenchmarkRun.toJson(): JsonObject = buildJsonObject {
        put("schemaVersion", HISTORY_SCHEMA_VERSION)
        put("generatedAt", metadata.generatedAt)
        metadata.repository?.let { put("repository", it) }
        metadata.eventName?.let { put("eventName", it) }
        metadata.refName?.let { put("refName", it) }
        metadata.commitSha?.let { put("commitSha", it) }
        metadata.runId?.let { put("runId", it) }
        metadata.runAttempt?.let { put("runAttempt", it) }
        metadata.runNumber?.let { put("runNumber", it) }
        metadata.runUrl?.let { put("runUrl", it) }
        metadata.commitUrl?.let { put("commitUrl", it) }
        metadata.siteUrl?.let { put("siteUrl", it) }
        metadata.runnerName?.let { put("runnerName", it) }
        metadata.runnerOs?.let { put("runnerOs", it) }
        put("measurements", buildJsonArray {
            measurements.forEach { add(it.toJson()) }
        })
    }

    private fun Measurement.toJson(): JsonObject = buildJsonObject {
        put("suite", suite)
        put("name", name)
        put("displayName", displayName)
        put("group", group)
        put("unit", unit)
        put("value", value)
        lowerValue?.let { put("lowerValue", it) }
        upperValue?.let { put("upperValue", it) }
        put("biggerIsBetter", biggerIsBetter)
    }

    private fun ComparisonEntry.toJson(): JsonObject = buildJsonObject {
        put("current", current.toJson())
        baseline?.let { put("baseline", it.toJson()) }
        delta?.let { put("delta", it) }
        deltaRatio?.let { put("deltaRatio", it) }
        put("criterion", criterion.jsonName)
        put("change", change.name.lowercase())
    }

    private fun List<ComparisonEntry>.toJson(currentRun: BenchmarkRun, baselineRun: BenchmarkRun?): JsonObject = buildJsonObject {
        put("schemaVersion", HISTORY_SCHEMA_VERSION)
        put("generatedAt", currentRun.metadata.generatedAt)
        put("significanceThreshold", significanceThreshold.get())
        put("current", currentRun.toJson())
        baselineRun?.let { put("baseline", it.toJson()) }
        put("entries", buildComparisonEntriesJson(this@toJson))
    }

    private fun BenchmarkHistory.toJson(): JsonObject = buildJsonObject {
        put("schemaVersion", HISTORY_SCHEMA_VERSION)
        put("generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        put("runs", buildJsonArray {
            runs.forEach { add(it.toJson()) }
        })
    }

    private fun List<ComparisonEntry>.topEntries(changeType: ChangeType): List<ComparisonEntry> =
        filter { it.change == changeType && it.deltaRatio != null }
            .sortedByDescending { abs(it.deltaRatio ?: 0.0) }
            .take(8)

    private fun JsonObject.requiredObject(name: String): JsonObject =
        this[name]?.jsonObject ?: error("Missing object '$name'")

    private fun JsonObject.jsonArrayOrEmpty(name: String): JsonArray =
        this[name]?.jsonArray ?: JsonArray(emptyList())

    private fun JsonObject.string(name: String): String =
        stringOrNull(name) ?: error("Missing string '$name'")

    private fun JsonObject.stringOrNull(name: String): String? =
        (this[name] as? JsonPrimitive)?.content

    private fun JsonObject.double(name: String): Double =
        this[name]?.jsonPrimitive?.double ?: error("Missing double '$name'")

    private fun JsonObject.doubleOrNull(name: String): Double? =
        this[name]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.boolean(name: String): Boolean =
        this[name]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: error("Missing boolean '$name'")

    private companion object {
        private const val BENCHMARK_PACKAGE_PREFIX = "me.zolotov.kodepoint.benchmark."
        private const val HISTORY_SCHEMA_VERSION = 1

        /** Suites whose values are exact byte counts, where any change is real. */
        private val DETERMINISTIC_SUITES = setOf("character-data")
        private const val DISPLAY_ZERO_EPSILON = 1e-4

        // Locale.ROOT everywhere: default-locale formatting can emit comma decimal
        // separators that corrupt JSON-adjacent output and percentage strings.
        private fun formatNumber(pattern: String, value: Double): String =
            String.format(Locale.ROOT, pattern, value)

        private val json = Json {
            prettyPrint = true
        }

        private val compactJson = Json
    }
}

private data class BenchmarkHistory(
    val runs: List<BenchmarkRun>
)

private data class BenchmarkRun(
    val metadata: RunMetadata,
    val measurements: List<Measurement>
)

private data class RunMetadata(
    val generatedAt: String,
    val repository: String?,
    val eventName: String?,
    val refName: String?,
    val commitSha: String?,
    val runId: String?,
    val runAttempt: String?,
    val runNumber: String?,
    val runUrl: String?,
    val commitUrl: String?,
    val siteUrl: String?,
    val runnerName: String?,
    val runnerOs: String?
)

private data class Measurement(
    val suite: String,
    val name: String,
    val displayName: String,
    val group: String,
    val unit: String,
    val value: Double,
    val lowerValue: Double?,
    val upperValue: Double?,
    val biggerIsBetter: Boolean
) {
    val key: String
        get() = "$suite::$name::$unit"
}

private data class ComparisonEntry(
    val current: Measurement,
    val baseline: Measurement?,
    val delta: Double?,
    val deltaRatio: Double?,
    val criterion: SignificanceCriterion,
    val change: ChangeType
)

private enum class SignificanceCriterion(val jsonName: String) {
    EXACT("exact"),
    CONFIDENCE_INTERVAL("confidence-interval"),
    THRESHOLD("threshold")
}

private enum class ChangeType {
    IMPROVEMENT,
    REGRESSION,
    NEW,
    UNCHANGED
}
