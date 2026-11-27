package me.zolotov.kodepoint.generator.graphemes

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import me.zolotov.kodepoint.generator.CharacterDataSizeMetric
import me.zolotov.kodepoint.generator.PlaneTableResult
import me.zolotov.kodepoint.generator.PropertyTableBuildResult
import me.zolotov.kodepoint.generator.UnicodeDataDownloader
import me.zolotov.kodepoint.generator.UnicodeVersion
import me.zolotov.kodepoint.generator.buildPropertyTables
import me.zolotov.kodepoint.generator.writeSizeMetricsJson
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

private const val RUNTIME_PACKAGE = "me.zolotov.kodepoint.graphemes"
private const val GENERATED_PACKAGE = "me.zolotov.kodepoint.graphemes.generated"
private val BINARY_SEARCH_RANGE = MemberName("$RUNTIME_PACKAGE.internal", "binarySearchRange")

/**
 * Longest string literal emitted in one piece. A JVM class-file constant holds at most 65,535 bytes
 * of modified UTF-8 (up to 3 bytes per char), so longer texts are split and joined at runtime.
 */
private const val MAX_LITERAL_CHARS = 16_000

/**
 * Separates the items of a joined text. Not `\n`: KotlinPoet emits a string with line breaks as a
 * `trimMargin()` raw string, which drops a line break at either end of a literal.
 */
private const val SEPARATOR = '\u001F'

const val GRAPHEME_BREAK_PROPERTY_FILE = "auxiliary/GraphemeBreakProperty.txt"
const val GRAPHEME_BREAK_TEST_FILE = "auxiliary/GraphemeBreakTest.txt"
const val DERIVED_CORE_PROPERTIES_FILE = "DerivedCoreProperties.txt"
const val EMOJI_DATA_FILE = "emoji/emoji-data.txt"
const val EMOJI_TEST_FILE = "emoji-test.txt"

/**
 * Generates the `:graphemes` tables, its conformance test data and its size report.
 *
 * @param outputDir commonMain sources: `GraphemeData`, `EmojiData` and the public `EmojiGroup` enum.
 * @param testOutputDir commonTest sources: the cases of `GraphemeBreakTest.txt`.
 * @param testResourcesDir jvmTest resources: copies of the data files, for the exhaustive JVM tests
 *   that parse them independently of this generator.
 */
@OptIn(ExperimentalPathApi::class)
fun generateGraphemeData(
    unicodeVersion: UnicodeVersion,
    outputDir: Path,
    testOutputDir: Path,
    testResourcesDir: Path,
    cacheDir: Path,
    additionalComment: String,
    metricsOutput: Path,
) {
    println("Generating grapheme and emoji data for Unicode $unicodeVersion")
    val ucdFiles = listOf(GRAPHEME_BREAK_PROPERTY_FILE, GRAPHEME_BREAK_TEST_FILE, DERIVED_CORE_PROPERTIES_FILE, EMOJI_DATA_FILE)
    val ucdDir = UnicodeDataDownloader.ensureUnicodeFilesDownloaded(cacheDir, unicodeVersion, ucdFiles)
    val emojiDir = UnicodeDataDownloader.ensureEmojiFilesDownloaded(cacheDir, unicodeVersion, listOf(EMOJI_TEST_FILE))

    val properties = parseGraphemeProperties(
        ucdDir.resolve(GRAPHEME_BREAK_PROPERTY_FILE),
        ucdDir.resolve(DERIVED_CORE_PROPERTIES_FILE),
        ucdDir.resolve(EMOJI_DATA_FILE),
    )
    val tables = buildPropertyTables(properties.packed)
    require(tables.uniqueCharacterProperties.size <= 256) {
        "${tables.uniqueCharacterProperties.size} distinct packed values do not fit the byte indices"
    }

    val emoji = buildEmojiTables(parseEmojiTest(emojiDir.resolve(EMOJI_TEST_FILE)))
    val testCases = parseGraphemeBreakTest(ucdDir.resolve(GRAPHEME_BREAK_TEST_FILE))
    println("  ${emoji.entries.size} RGI entries, ${emoji.sorted.size} emoji-test sequences, ${testCases.size} break test cases")

    outputDir.deleteRecursively()
    testOutputDir.deleteRecursively()
    testResourcesDir.deleteRecursively()
    val header = "Unicode version: $unicodeVersion\n$additionalComment"

    FileSpec.builder(GENERATED_PACKAGE, "GraphemeData")
        .addType(graphemeDataObject(tables, unicodeVersion, header))
        .build()
        .writeTo(outputDir)
    FileSpec.builder(GENERATED_PACKAGE, "EmojiData")
        .addType(emojiDataObject(emoji, unicodeVersion, header))
        .build()
        .writeTo(outputDir)
    FileSpec.builder(RUNTIME_PACKAGE, "EmojiGroup")
        .addType(emojiGroupEnum(emoji.groups, unicodeVersion, header))
        .build()
        .writeTo(outputDir)
    FileSpec.builder(RUNTIME_PACKAGE, "GraphemeBreakTestData")
        .addType(graphemeBreakTestData(testCases, unicodeVersion, header))
        .build()
        .writeTo(testOutputDir)

    val resourceDir = testResourcesDir.resolve("grapheme-data").createDirectories()
    for (file in ucdFiles) ucdDir.resolve(file).copyTo(resourceDir.resolve(file.substringAfterLast('/')))
    emojiDir.resolve(EMOJI_TEST_FILE).copyTo(resourceDir.resolve(EMOJI_TEST_FILE))

    val metrics = sizeMetrics(tables, emoji)
    writeSizeMetricsJson(metrics, metricsOutput)
    metrics.forEach { println("  ${it.benchmark}: ${it.bytes} bytes") }
}

// ---------------------------------------------------------------------------------------------
// Code-point properties

private fun graphemeDataObject(tables: PropertyTableBuildResult, unicodeVersion: UnicodeVersion, header: String): TypeSpec {
    val unique = tables.uniqueCharacterProperties
    val indexOf = unique.withIndex().associate { it.value to it.index }
    val builder = TypeSpec.objectBuilder(ClassName(GENERATED_PACKAGE, "GraphemeData"))
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            "Grapheme_Cluster_Break, Indic_Conjunct_Break and the emoji properties of every code point, packed " +
                "as in `GraphemePropertyPacker`.\n\n$header"
        )
        .addProperty(constString("UNICODE_VERSION", unicodeVersion.value, KModifier.INTERNAL))
        .addProperty(constInt("GCB_MASK", "0x%X".format(GraphemePropertyPacker.GCB_MASK)))
    GRAPHEME_CLUSTER_BREAK_VALUES.forEachIndexed { code, name ->
        builder.addProperty(constInt("GCB_${constantName(name)}", code.toString()))
    }
    builder.addProperty(constInt("INCB_MASK", "${GraphemePropertyPacker.INCB_BITS} shl ${GraphemePropertyPacker.INCB_SHIFT}"))
    builder.addProperty(constInt("INCB_SHIFT", GraphemePropertyPacker.INCB_SHIFT.toString()))
    INDIC_CONJUNCT_BREAK_VALUES.forEachIndexed { code, name ->
        builder.addProperty(constInt("INCB_${constantName(name)}", "$code shl ${GraphemePropertyPacker.INCB_SHIFT}"))
    }
    for (property in EMOJI_PROPERTIES) {
        val bit = GraphemePropertyPacker.emojiPropertyBit(property)
        builder.addProperty(constInt("${constantName(property)}_BIT", "1 shl ${bit.countTrailingZeroBits()}"))
    }

    val uniqueValues = PropertySpec.builder("UNIQUE_VALUES", IntArray::class, KModifier.PRIVATE)
        .initializer("intArrayOf(%L)", unique.joinToString(", ") { "0x%X".format(it) })
        .build()
    builder.addProperty(uniqueValues)
    builder.addProperty(
        PropertySpec.builder("LATIN1_INDICES", STRING, KModifier.PRIVATE, KModifier.CONST)
            .initializer("%S", string8(tables.latin1Properties.map { indexOf.getValue(it) }))
            .build()
    )

    val getProperties = FunSpec.builder("getProperties")
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Packed properties of [cp]; 0 (Other, None, no emoji property) outside U+0000..U+10FFFF.")
        .addParameter("cp", Int::class)
        .returns(Int::class)
        .beginControlFlow("return when")

    for (planeResult in tables.planeResults) {
        val plane = planeResult.plane
        val prefix = plane.name
        when (planeResult) {
            is PlaneTableResult.Table -> {
                val table = planeResult.table
                builder.addProperty(constInt("${prefix}_BLOCK_SHIFT", table.blockBits.toString(), KModifier.PRIVATE))
                builder.addProperty(constInt("${prefix}_BLOCK_MASK", ((1 shl table.blockBits) - 1).toString(), KModifier.PRIVATE))
                builder.addProperty(
                    PropertySpec.builder("${prefix}_BLOCK_INDEX", STRING, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%S", string16(table.indexTable.toList()))
                        .build()
                )
                builder.addProperty(
                    PropertySpec.builder("${prefix}_INDICES", STRING, KModifier.PRIVATE, KModifier.CONST)
                        .initializer("%S", string8(table.dataTable.toList()))
                        .build()
                )
            }

            is PlaneTableResult.Sparse -> builder.addProperty(
                PropertySpec.builder("${prefix}_RANGES", IntArray::class, KModifier.PRIVATE)
                    .initializer(
                        "intArrayOf(%L)",
                        planeResult.ranges.joinToString(", ") {
                            "0x%X, 0x%X, 0x%X".format(it.startCodepoint, it.endCodepoint, it.value)
                        }
                    )
                    .build()
            )
        }
    }

    val bmp = tables.planeResults.first()
    require(bmp is PlaneTableResult.Table && bmp.plane.startCodepoint == 0x100 && bmp.plane.endCodepoint == 0xFFFF) {
        "The first plane must be the table-encoded BMP after Latin-1"
    }
    getProperties.addStatement(
        "cp ushr 16 == 0 -> if (cp < 0x100) UNIQUE_VALUES[LATIN1_INDICES[cp].code] else UNIQUE_VALUES[lookupBMP(cp - 0x100)]"
    )
    getProperties.addStatement("cp < 0 -> 0")
    for (planeResult in tables.planeResults.drop(1)) {
        val plane = planeResult.plane
        when (planeResult) {
            is PlaneTableResult.Sparse -> getProperties.addStatement(
                "cp <= %L -> %M(cp - %L, ${plane.name}_RANGES, 0)",
                hex(plane.endCodepoint), BINARY_SEARCH_RANGE, hex(plane.startCodepoint)
            )

            is PlaneTableResult.Table -> getProperties.addStatement(
                "cp <= %L -> UNIQUE_VALUES[lookup${plane.name}(cp - %L)]", hex(plane.endCodepoint), hex(plane.startCodepoint)
            )
        }
    }
    getProperties.addStatement("else -> 0").endControlFlow()
    builder.addFunction(getProperties.build())

    for (planeResult in tables.planeResults.filterIsInstance<PlaneTableResult.Table>()) {
        val prefix = planeResult.plane.name
        builder.addFunction(
            FunSpec.builder("lookup$prefix")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("val block = ${prefix}_BLOCK_INDEX[offset ushr ${prefix}_BLOCK_SHIFT].code")
                .addStatement(
                    "return ${prefix}_INDICES[(block shl ${prefix}_BLOCK_SHIFT) + (offset and ${prefix}_BLOCK_MASK)].code"
                )
                .build()
        )
    }
    return builder.build()
}

// ---------------------------------------------------------------------------------------------
// Emoji metadata

/**
 * @property entries the fully-qualified and component lines in file order: what `Emoji.all()` returns.
 * @property sorted every line of `emoji-test.txt`, sorted by UTF-16 text for binary search.
 * @property sortedEntry for each [sorted] line, the index in [entries] of the line it belongs to.
 * @property entrySorted for each of [entries], its index in [sorted].
 */
class EmojiTables(
    val entries: List<EmojiTestLine>,
    val sorted: List<EmojiTestLine>,
    val sortedEntry: List<Int>,
    val entrySorted: List<Int>,
    val groups: List<String>,
    val subgroups: List<String>,
    val versions: List<String>,
)

private fun buildEmojiTables(lines: List<EmojiTestLine>): EmojiTables {
    val entries = lines.filter {
        it.status == EmojiStatusValue.FULLY_QUALIFIED || it.status == EmojiStatusValue.COMPONENT
    }
    // Minimally-qualified and unqualified forms differ from their fully-qualified form only by
    // missing U+FE0F, so stripping every U+FE0F gives the key that ties them together.
    fun key(line: EmojiTestLine) = line.codepoints.filter { it != 0xFE0F }
    val entryByKey = HashMap<List<Int>, Int>()
    entries.forEachIndexed { index, line ->
        require(entryByKey.put(key(line), index) == null) { "Two RGI entries share the key of ${line.text}" }
    }
    val sorted = lines.sortedWith { a, b -> a.text.compareTo(b.text) }
    for (i in 1 until sorted.size) {
        require(sorted[i - 1].text != sorted[i].text) { "Duplicate emoji-test.txt sequence ${sorted[i].text}" }
    }
    val sortedEntry = sorted.map { line ->
        val index = entryByKey[key(line)] ?: error("No fully-qualified form for ${line.codepoints} (${line.name})")
        val entry = entries[index]
        require(entry.name == line.name && entry.version == line.version && entry.group == line.group && entry.subgroup == line.subgroup) {
            "${line.status} ${line.text} (${line.name}) differs from its fully-qualified form ${entry.text} (${entry.name}) " +
                "in more than U+FE0F; Emoji.lookup stores the metadata once per fully-qualified form"
        }
        index
    }
    val sortedIndexOf = sorted.withIndex().associate { it.value to it.index }
    return EmojiTables(
        entries = entries,
        sorted = sorted,
        sortedEntry = sortedEntry,
        entrySorted = entries.map { sortedIndexOf.getValue(it) },
        groups = lines.map { it.group }.distinct(),
        subgroups = lines.map { it.subgroup }.distinct(),
        versions = lines.map { it.version }.distinct().sortedWith(compareBy({ it.substringBefore('.').toInt() }, { it.substringAfter('.').toInt() })),
    )
}

private fun emojiDataObject(tables: EmojiTables, unicodeVersion: UnicodeVersion, header: String): TypeSpec {
    require(tables.subgroups.size <= 256 && tables.groups.size <= 256 && tables.versions.size <= 256)
    require(tables.sorted.all { it.text.length <= 255 })
    val subgroupIndex = tables.subgroups.withIndex().associate { it.value to it.index }
    val groupIndex = tables.groups.withIndex().associate { it.value to it.index }
    val versionIndex = tables.versions.withIndex().associate { it.value to it.index }
    val names = tables.entries.joinToString(SEPARATOR.toString()) { it.name }
    require(tables.entries.none { SEPARATOR in it.name || SEPARATOR in it.subgroup || '\n' in it.name })

    return TypeSpec.objectBuilder(ClassName(GENERATED_PACKAGE, "EmojiData"))
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            "The lines of `emoji-test.txt` ${unicodeVersion.emojiVersion}. Entries are the fully-qualified and " +
                "component lines in file order; every line, in UTF-16 order, points at the entry it belongs to.\n\n$header"
        )
        .addProperty(constString("EMOJI_VERSION", unicodeVersion.emojiVersion, KModifier.INTERNAL))
        .addProperty(constInt("ENTRY_COUNT", tables.entries.size.toString(), KModifier.INTERNAL))
        .addProperty(constInt("SEQUENCE_COUNT", tables.sorted.size.toString(), KModifier.INTERNAL))
        .addProperty(
            joinedString(
                "SEQUENCES", tables.sorted.map { it.text },
                "Every sequence of the file in UTF-16 order, concatenated; [SEQUENCE_LENGTHS] splits them."
            )
        )
        .addProperty(constCodes("SEQUENCE_LENGTHS", tables.sorted.map { it.text.length }, "UTF-16 length of each sorted sequence."))
        .addProperty(constCodes("SEQUENCE_STATUSES", tables.sorted.map { it.status.ordinal }, "`EmojiStatus` ordinal of each sorted sequence."))
        .addProperty(constCodes("SEQUENCE_ENTRIES", tables.sortedEntry, "Entry each sorted sequence belongs to."))
        .addProperty(constCodes("ENTRY_SEQUENCES", tables.entrySorted, "Sorted index of each entry's own sequence."))
        .addProperty(constCodes("ENTRY_GROUPS", tables.entries.map { groupIndex.getValue(it.group) }, "`EmojiGroup` ordinal of each entry."))
        .addProperty(constCodes("ENTRY_SUBGROUPS", tables.entries.map { subgroupIndex.getValue(it.subgroup) }, "Index into [SUBGROUPS] of each entry."))
        .addProperty(constCodes("ENTRY_VERSIONS", tables.entries.map { versionIndex.getValue(it.version) }, "Index into [VERSIONS] of each entry."))
        .addProperty(joinedString("NAMES", listOf(names), "CLDR short name of each entry, separated by U+001F."))
        .addProperty(constString("SUBGROUPS", tables.subgroups.joinToString(SEPARATOR.toString()), KModifier.INTERNAL))
        .addProperty(constString("VERSIONS", tables.versions.joinToString(SEPARATOR.toString()), KModifier.INTERNAL))
        .build()
}

private fun emojiGroupEnum(groups: List<String>, unicodeVersion: UnicodeVersion, header: String): TypeSpec {
    val builder = TypeSpec.enumBuilder(ClassName(RUNTIME_PACKAGE, "EmojiGroup"))
        .addKdoc(
            "The groups of `emoji-test.txt` ${unicodeVersion.emojiVersion}, in file order. The groups are " +
                "illustrative, see UTS #51.\n\n$header"
        )
        .primaryConstructor(FunSpec.constructorBuilder().addParameter("label", STRING).build())
        .addProperty(
            PropertySpec.builder("label", STRING)
                .initializer("label")
                .addKdoc("The group name as `emoji-test.txt` writes it, e.g. `Smileys & Emotion`.")
                .build()
        )
    for (group in groups) {
        builder.addEnumConstant(
            constantName(group.replace("&", "and")),
            TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", group).build()
        )
    }
    return builder.build()
}

// ---------------------------------------------------------------------------------------------
// Test data

private fun graphemeBreakTestData(cases: List<GraphemeBreakTestCase>, unicodeVersion: UnicodeVersion, header: String): TypeSpec =
    TypeSpec.objectBuilder(ClassName(RUNTIME_PACKAGE, "GraphemeBreakTestData"))
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("The test lines of `GraphemeBreakTest.txt` $unicodeVersion, without comments.\n\n$header")
        .addProperty(constInt("CASE_COUNT", cases.size.toString(), KModifier.INTERNAL))
        .addProperty(
            joinedString(
                "CASES", cases.map { "${it.lineNumber}:${it.source}$SEPARATOR" },
                "`<line number in the file>:<the line, e.g. ÷ 0020 × 0308 ÷>`, each followed by U+001F."
            )
        )
        .build()

// ---------------------------------------------------------------------------------------------
// Size report

private fun sizeMetrics(tables: PropertyTableBuildResult, emoji: EmojiTables): List<CharacterDataSizeMetric> {
    val metrics = mutableListOf<CharacterDataSizeMetric>()
    metrics += CharacterDataSizeMetric("grapheme-data-latin1", tables.latin1Properties.size)
    for (planeResult in tables.planeResults) {
        metrics += CharacterDataSizeMetric("grapheme-data-${planeResult.plane.name.lowercase()}", planeResult.size)
    }
    metrics += CharacterDataSizeMetric("grapheme-data-unique-values", tables.uniqueCharacterProperties.size * Int.SIZE_BYTES)
    val propertyTotal = metrics.sumOf { it.bytes }
    metrics += CharacterDataSizeMetric("grapheme-data-total", propertyTotal)

    // Runtime strings hold UTF-16 chars: 2 bytes each.
    val sequences = emoji.sorted.sumOf { it.text.length } * 2
    // Lengths, statuses and entries per sequence, plus each entry's own sequence index.
    val sequenceIndex = emoji.sorted.size * 2 * 3 + emoji.entries.size * 2
    val names = (emoji.entries.sumOf { it.name.length } + emoji.entries.size - 1) * 2
    val entryIndex = emoji.entries.size * 2 * 3 // group, subgroup, version
    val labels = (emoji.subgroups.sumOf { it.length + 1 } + emoji.versions.sumOf { it.length + 1 }) * 2
    metrics += CharacterDataSizeMetric("emoji-data-sequences", sequences + sequenceIndex)
    metrics += CharacterDataSizeMetric("emoji-data-names", names)
    metrics += CharacterDataSizeMetric("emoji-data-metadata", entryIndex + labels)
    metrics += CharacterDataSizeMetric("emoji-data-total", sequences + sequenceIndex + names + entryIndex + labels)
    return metrics
}

// ---------------------------------------------------------------------------------------------
// Helpers

private fun hex(value: Int): String = "0x%X".format(value)

/** `Regional_Indicator` → `REGIONAL_INDICATOR`, `SpacingMark` → `SPACING_MARK`, `Smileys and Emotion` → `SMILEYS_AND_EMOTION`. */
internal fun constantName(name: String): String =
    name.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        .replace(Regex("[^A-Za-z0-9]+"), "_")
        .trim('_')
        .uppercase()

private fun constInt(name: String, literal: String, vararg modifiers: KModifier = arrayOf(KModifier.INTERNAL)): PropertySpec =
    PropertySpec.builder(name, Int::class, *modifiers, KModifier.CONST).initializer("%L", literal).build()

private fun constString(name: String, value: String, vararg modifiers: KModifier): PropertySpec {
    require(value.length <= MAX_LITERAL_CHARS) { "$name is too long for one constant" }
    return PropertySpec.builder(name, STRING, *modifiers, KModifier.CONST).initializer("%S", value).build()
}

/** Small non-negative ints, one `Char` each, as a constant string read with `[index].code`. */
private fun constCodes(name: String, values: List<Int>, kdoc: String): PropertySpec =
    PropertySpec.builder(name, STRING, KModifier.INTERNAL, KModifier.CONST)
        .addKdoc(kdoc)
        .initializer("%S", string16(values))
        .build()

/**
 * A string assembled at runtime from literals of at most [MAX_LITERAL_CHARS] chars, cut only
 * between [parts] or, inside a part, never between the two halves of a surrogate pair.
 */
private fun joinedString(name: String, parts: List<String>, kdoc: String): PropertySpec {
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    for (part in parts) {
        var start = 0
        while (start < part.length) {
            if (current.length == MAX_LITERAL_CHARS) {
                chunks += current.toString()
                current.clear()
            }
            var end = minOf(part.length, start + MAX_LITERAL_CHARS - current.length)
            if (end < part.length && part[end - 1].isHighSurrogate()) end--
            if (end == start) {
                chunks += current.toString()
                current.clear()
                continue
            }
            current.append(part, start, end)
            start = end
        }
    }
    if (current.isNotEmpty() || chunks.isEmpty()) chunks += current.toString()
    val initializer = CodeBlock.builder().add("arrayOf(\n").indent()
    chunks.forEach { initializer.add("%S,\n", it) }
    initializer.unindent().add(").joinToString(%S)", "")
    return PropertySpec.builder(name, STRING, KModifier.INTERNAL)
        .addKdoc(kdoc)
        .initializer(initializer.build())
        .build()
}

private fun string8(values: List<Int>): String = buildString {
    for (value in values) {
        require(value in 0..0xFF) { "$value does not fit in a byte" }
        append(value.toChar())
    }
}

private fun string16(values: List<Int>): String = buildString {
    for (value in values) {
        // Values in the surrogate range would make the literal ill-formed UTF-16 in the source file.
        require(value in 0..0xD7FF) { "$value does not fit in a non-surrogate char" }
        append(value.toChar())
    }
}
