package me.zolotov.kodepoint.generator.graphemes

import me.zolotov.kodepoint.generator.MAX_CODEPOINT
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * `Grapheme_Cluster_Break` values in the order of the runtime `GraphemeClusterBreak` enum.
 * The index of a value is its packed code, so `Other`, the default, packs to 0.
 */
val GRAPHEME_CLUSTER_BREAK_VALUES = listOf(
    "Other", "CR", "LF", "Control", "Extend", "ZWJ", "Regional_Indicator",
    "Prepend", "SpacingMark", "L", "V", "T", "LV", "LVT"
)

/** `Indic_Conjunct_Break` values in the order of the runtime `IndicConjunctBreak` enum; `None` packs to 0. */
val INDIC_CONJUNCT_BREAK_VALUES = listOf("None", "Linker", "Consonant", "Extend")

/** The `emoji-data.txt` properties packed into one bit each, in bit order. */
val EMOJI_PROPERTIES = listOf(
    "Extended_Pictographic", "Emoji", "Emoji_Presentation", "Emoji_Modifier", "Emoji_Modifier_Base", "Emoji_Component"
)

/**
 * One line of a UCD property file: a code point range and the value fields after it.
 */
class UcdEntry(val first: Int, val last: Int, val fields: List<String>)

/**
 * Parses a UCD property file of the form `XXXX..YYYY ; value [; value] # comment`.
 * `@missing` lines are returned through [missing] so callers can check the default they assume.
 */
fun parseUcdPropertyFile(file: Path, missing: MutableList<UcdEntry>? = null): List<UcdEntry> {
    val entries = mutableListOf<UcdEntry>()
    for (rawLine in file.readLines()) {
        val missingPrefix = "# @missing:"
        if (rawLine.startsWith(missingPrefix)) {
            missing?.add(parseUcdLine(rawLine.removePrefix(missingPrefix)))
            continue
        }
        val line = rawLine.substringBefore('#').trim()
        if (line.isEmpty()) continue
        entries += parseUcdLine(line)
    }
    return entries
}

private fun parseUcdLine(line: String): UcdEntry {
    val parts = line.substringBefore('#').split(';').map { it.trim() }
    val range = parts[0].split("..")
    val first = range[0].toInt(16)
    val last = range.last().toInt(16)
    return UcdEntry(first, last, parts.drop(1))
}

/**
 * The per-code-point properties grapheme segmentation and the emoji predicates need, packed as
 * the runtime decodes them (see `GraphemeData` in the generated sources):
 *
 * | Bits  | Field                                  |
 * |-------|----------------------------------------|
 * | 0–3   | `Grapheme_Cluster_Break`               |
 * | 4–5   | `Indic_Conjunct_Break`                 |
 * | 6     | `Extended_Pictographic`                |
 * | 7–11  | `Emoji`, `Emoji_Presentation`, `Emoji_Modifier`, `Emoji_Modifier_Base`, `Emoji_Component` |
 */
object GraphemePropertyPacker {
    const val GCB_MASK = 0xF
    const val INCB_SHIFT = 4
    const val INCB_BITS = 0x3
    const val EMOJI_PROPERTIES_SHIFT = 6

    fun emojiPropertyBit(property: String): Int {
        val index = EMOJI_PROPERTIES.indexOf(property)
        require(index >= 0) { "Unknown emoji property $property" }
        return 1 shl (EMOJI_PROPERTIES_SHIFT + index)
    }

    fun gcb(props: Int): Int = props and GCB_MASK
    fun incb(props: Int): Int = (props ushr INCB_SHIFT) and INCB_BITS
}

class GraphemeProperties(
    /** Packed properties for every code point U+0000..U+10FFFF. */
    val packed: IntArray
)

fun parseGraphemeProperties(
    graphemeBreakPropertyFile: Path,
    derivedCorePropertiesFile: Path,
    emojiDataFile: Path,
): GraphemeProperties {
    val packed = IntArray(MAX_CODEPOINT + 1)

    val gcbMissing = mutableListOf<UcdEntry>()
    for (entry in parseUcdPropertyFile(graphemeBreakPropertyFile, gcbMissing)) {
        val code = GRAPHEME_CLUSTER_BREAK_VALUES.indexOf(entry.fields[0])
        require(code >= 0) {
            "Unknown Grapheme_Cluster_Break value '${entry.fields[0]}'. A new value needs a constant in " +
                "GRAPHEME_CLUSTER_BREAK_VALUES, in the runtime GraphemeClusterBreak enum and, most likely, a new rule."
        }
        for (cp in entry.first..entry.last) packed[cp] = packed[cp] or code
    }
    require(gcbMissing.all { it.fields == listOf("Other") }) {
        "GraphemeBreakProperty.txt declares an unexpected default: ${gcbMissing.map { it.fields }}"
    }

    val derivedMissing = mutableListOf<UcdEntry>()
    for (entry in parseUcdPropertyFile(derivedCorePropertiesFile, derivedMissing)) {
        if (entry.fields[0] != "InCB") continue
        val code = INDIC_CONJUNCT_BREAK_VALUES.indexOf(entry.fields[1])
        require(code > 0) { "Unknown Indic_Conjunct_Break value '${entry.fields[1]}'" }
        for (cp in entry.first..entry.last) {
            packed[cp] = packed[cp] or (code shl GraphemePropertyPacker.INCB_SHIFT)
        }
    }
    require(derivedMissing.filter { it.fields.firstOrNull() == "InCB" }.all { it.fields == listOf("InCB", "None") }) {
        "DerivedCoreProperties.txt declares an unexpected InCB default"
    }

    for (entry in parseUcdPropertyFile(emojiDataFile)) {
        val property = entry.fields[0]
        if (property !in EMOJI_PROPERTIES) continue
        val bit = GraphemePropertyPacker.emojiPropertyBit(property)
        for (cp in entry.first..entry.last) packed[cp] = packed[cp] or bit
    }

    checkSegmentationInvariants(packed)
    return GraphemeProperties(packed)
}

/**
 * The runtime restarts segmentation from any known boundary with an empty state, and decides a
 * single boundary by looking back only over the runs GB9c, GB11 and GB12/13 name. Both are exact
 * only while these facts hold; a data update that breaks one must also update the runtime.
 */
private fun checkSegmentationInvariants(packed: IntArray) {
    val extend = GRAPHEME_CLUSTER_BREAK_VALUES.indexOf("Extend")
    val zwj = GRAPHEME_CLUSTER_BREAK_VALUES.indexOf("ZWJ")
    val regionalIndicator = GRAPHEME_CLUSTER_BREAK_VALUES.indexOf("Regional_Indicator")
    val incbExtend = INDIC_CONJUNCT_BREAK_VALUES.indexOf("Extend")
    val incbConsonant = INDIC_CONJUNCT_BREAK_VALUES.indexOf("Consonant")
    val extPict = GraphemePropertyPacker.emojiPropertyBit("Extended_Pictographic")
    val control = setOf("CR", "LF", "Control").map { GRAPHEME_CLUSTER_BREAK_VALUES.indexOf(it) }.toSet()
    for (cp in packed.indices) {
        val props = packed[cp]
        val gcb = GraphemePropertyPacker.gcb(props)
        val incb = GraphemePropertyPacker.incb(props)
        require(incb != incbExtend || gcb == extend || gcb == zwj) {
            "U+%04X has InCB=Extend but GCB=%s: GB9c chains could then span a cluster boundary".format(
                cp, GRAPHEME_CLUSTER_BREAK_VALUES[gcb]
            )
        }
        require(incb == 0 || gcb !in control) {
            "U+%04X has an InCB value but is a control: GB4/GB5 would cut GB9c chains".format(cp)
        }
        require(incb != incbConsonant || props and extPict == 0) {
            "U+%04X is both InCB=Consonant and Extended_Pictographic".format(cp)
        }
        require(props and extPict == 0 || gcb != extend && gcb != zwj) {
            "U+%04X is Extended_Pictographic and GCB=Extend/ZWJ".format(cp)
        }
        require((gcb == zwj) == (cp == 0x200D)) { "U+%04X: GCB=ZWJ is no longer exactly U+200D".format(cp) }
        // The runtime counts regional indicators by UTF-16 length and recognizes them by range.
        require((gcb == regionalIndicator) == (cp in 0x1F1E6..0x1F1FF)) {
            "U+%04X: GCB=Regional_Indicator is no longer exactly U+1F1E6..U+1F1FF".format(cp)
        }
        require(gcb != regionalIndicator || incb == 0 && props and extPict == 0) {
            "U+%04X is a regional indicator with an InCB value or Extended_Pictographic".format(cp)
        }
    }
}

/** One test line of `GraphemeBreakTest.txt`: the code points and the break opportunity before each and at the end. */
class GraphemeBreakTestCase(val lineNumber: Int, val source: String)

fun parseGraphemeBreakTest(file: Path): List<GraphemeBreakTestCase> =
    file.readLines().mapIndexedNotNull { index, line ->
        val content = line.substringBefore('#').trim()
        if (content.isEmpty()) null else GraphemeBreakTestCase(index + 1, content)
    }

enum class EmojiStatusValue(val fileValue: String) {
    FULLY_QUALIFIED("fully-qualified"),
    MINIMALLY_QUALIFIED("minimally-qualified"),
    UNQUALIFIED("unqualified"),
    COMPONENT("component");

    companion object {
        fun of(fileValue: String): EmojiStatusValue =
            entries.firstOrNull { it.fileValue == fileValue } ?: error("Unknown emoji status '$fileValue'")
    }
}

class EmojiTestLine(
    val codepoints: List<Int>,
    val status: EmojiStatusValue,
    val name: String,
    val version: String,
    val group: String,
    val subgroup: String,
) {
    val text: String = buildString { codepoints.forEach { appendCodePoint(it) } }
}

private val EMOJI_TEST_LINE = Regex("""^([0-9A-F ]+);\s*([a-z-]+)\s*#\s*\S+\s+E(\d+\.\d+)\s+(.+)$""")

fun parseEmojiTest(file: Path): List<EmojiTestLine> {
    var group: String? = null
    var subgroup: String? = null
    val lines = mutableListOf<EmojiTestLine>()
    for (line in file.readLines()) {
        when {
            line.startsWith("# group:") -> group = line.removePrefix("# group:").trim()
            line.startsWith("# subgroup:") -> subgroup = line.removePrefix("# subgroup:").trim()
            line.isBlank() || line.startsWith("#") -> Unit
            else -> {
                val match = EMOJI_TEST_LINE.matchEntire(line) ?: error("Unparseable emoji-test.txt line: $line")
                lines += EmojiTestLine(
                    codepoints = match.groupValues[1].trim().split(Regex("\\s+")).map { it.toInt(16) },
                    status = EmojiStatusValue.of(match.groupValues[2]),
                    name = match.groupValues[4].trim(),
                    version = match.groupValues[3],
                    group = group ?: error("emoji-test.txt line before any group: $line"),
                    subgroup = subgroup ?: error("emoji-test.txt line before any subgroup: $line"),
                )
            }
        }
    }
    return lines
}
