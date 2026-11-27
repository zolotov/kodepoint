package me.zolotov.kodepoint.graphemes

import me.zolotov.kodepoint.graphemes.generated.EmojiData
import me.zolotov.kodepoint.graphemes.internal.emojiSequenceKind
import me.zolotov.kodepoint.graphemes.internal.nextBoundaryFromBoundary

/**
 * The kind of a well-formed emoji sequence (UTS #51 ED-14 to ED-17).
 */
enum class EmojiSequenceKind {
    /** A single emoji character, optionally followed by U+FE0F: 😀, ☺️, and also `1` or `©` (see [forEachEmoji]). */
    BASIC,

    /** A keycap sequence: `[0-9#*]` U+FE0F U+20E3, e.g. #️⃣. */
    KEYCAP,

    /** A pair of regional indicators, e.g. 🇺🇦. Any pair, also one that names no country. */
    FLAG,

    /** A tag sequence, e.g. the flag of England 🏴󠁧󠁢󠁥󠁮󠁧󠁿. */
    TAG,

    /** An emoji modifier base followed by a skin-tone modifier, e.g. 👋🏽. */
    MODIFIER,

    /** Emoji elements joined by U+200D ZERO WIDTH JOINER, e.g. 👨‍👩‍👧. */
    ZWJ,
}

/**
 * Calls [action] for every grapheme cluster whose whole content is a well-formed emoji sequence by
 * the UTS #51 grammar (ED-17 `emoji_sequence`), whether or not it is recommended for general
 * interchange (RGI; see [Emoji.isRgi]). Every reported range is a grapheme cluster. Allocates nothing.
 *
 * As in UTS #51, a single character with the `Emoji` property is an emoji sequence even when it is
 * displayed as text by default: the digits, `#`, `*`, `©`, `®`, `‼` and others are reported as
 * [EmojiSequenceKind.BASIC]. Check `Codepoint.isEmojiPresentation()` or [Emoji.isRgi] to narrow
 * the result.
 *
 * ```
 * "Hi 👋🏽, á 🇺🇦!".forEachEmoji { start, end, kind -> println("$start..<$end $kind") }
 * // 3..<7 MODIFIER
 * // 11..<15 FLAG
 * ```
 */
inline fun CharSequence.forEachEmoji(action: (start: Int, end: Int, kind: EmojiSequenceKind) -> Unit) {
    forEachGraphemeCluster { start, end ->
        val kind = emojiSequenceKind(this, start, end)
        if (kind >= 0) action(start, end, EmojiSequenceKind.entries[kind])
    }
}

/** Whether the whole text is one well-formed emoji sequence, as [forEachEmoji] reports them. */
fun CharSequence.isEmojiSequence(): Boolean =
    isNotEmpty() && nextBoundaryFromBoundary(this, 0) == length && emojiSequenceKind(this, 0, length) >= 0

/** The qualification status of an `emoji-test.txt` line (UTS #51 ED-18 to ED-20). */
enum class EmojiStatus {
    /** The RGI form, with every emoji presentation selector U+FE0F it needs. */
    FULLY_QUALIFIED,

    /** Lacks a U+FE0F that the fully-qualified form has, except the first one. */
    MINIMALLY_QUALIFIED,

    /** Lacks the first U+FE0F of the fully-qualified form. */
    UNQUALIFIED,

    /** A skin-tone modifier or hair component, which is RGI on its own. */
    COMPONENT,
}

/**
 * Metadata of an emoji from `emoji-test.txt`.
 *
 * @property fullyQualified the fully-qualified form of the emoji; for a component, the component itself.
 * @property status the status of the sequence that was looked up.
 * @property name the CLDR short name, e.g. `grinning face`.
 * @property group the group, e.g. [EmojiGroup.SMILEYS_AND_EMOTION].
 * @property subgroup the subgroup, e.g. `face-smiling`.
 * @property version the emoji version that introduced it, e.g. `1.0` or `15.1`.
 */
class EmojiInfo(
    val fullyQualified: String,
    val status: EmojiStatus,
    val name: String,
    val group: EmojiGroup,
    val subgroup: String,
    val version: String,
) {
    override fun equals(other: Any?): Boolean =
        other is EmojiInfo && fullyQualified == other.fullyQualified && status == other.status &&
            name == other.name && group == other.group && subgroup == other.subgroup && version == other.version

    override fun hashCode(): Int {
        var result = fullyQualified.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + group.hashCode()
        result = 31 * result + subgroup.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    override fun toString(): String =
        "EmojiInfo(fullyQualified=$fullyQualified, status=$status, name=$name, group=$group, " +
            "subgroup=$subgroup, version=$version)"
}

/**
 * The emoji of `emoji-test.txt` for emoji version [VERSION]: names, groups, versions and the RGI set.
 */
object Emoji {
    /** The emoji version of the data, e.g. `18.0`: the major and minor part of [Graphemes.UNICODE_VERSION]. */
    const val VERSION: String = EmojiData.EMOJI_VERSION

    /**
     * The metadata of [sequence] if it is exactly one line of `emoji-test.txt`: a fully-qualified,
     * minimally-qualified or unqualified emoji, or a component; otherwise null.
     *
     * ```
     * Emoji.lookup("☺")?.status          // UNQUALIFIED
     * Emoji.lookup("☺")?.fullyQualified  // "☺️"
     * ```
     */
    fun lookup(sequence: CharSequence): EmojiInfo? {
        val index = EmojiTables.find(sequence)
        if (index < 0) return null
        val entry = EmojiTables.entry(EmojiData.SEQUENCE_ENTRIES[index].code)
        val status = EmojiStatus.entries[EmojiData.SEQUENCE_STATUSES[index].code]
        return if (status == entry.status) entry else EmojiInfo(
            entry.fullyQualified, status, entry.name, entry.group, entry.subgroup, entry.version
        )
    }

    /**
     * Whether [sequence] is in the RGI emoji set (UTS #51 ED-27): it is a fully-qualified emoji or a
     * component of `emoji-test.txt`, which are the entries of [all].
     */
    fun isRgi(sequence: CharSequence): Boolean {
        val index = EmojiTables.find(sequence)
        if (index < 0) return false
        val status = EmojiData.SEQUENCE_STATUSES[index].code
        return status == EmojiStatus.FULLY_QUALIFIED.ordinal || status == EmojiStatus.COMPONENT.ordinal
    }

    /** The RGI emoji: every fully-qualified and component entry of `emoji-test.txt`, in file order. */
    fun all(): List<EmojiInfo> = EmojiTables.all
}

private object EmojiTables {
    private const val SEPARATOR = '\u001F'

    /** Start of each sorted sequence in [EmojiData.SEQUENCES], plus the end of the last. */
    private val sequenceOffsets: IntArray by lazy {
        val offsets = IntArray(EmojiData.SEQUENCE_COUNT + 1)
        for (i in 0 until EmojiData.SEQUENCE_COUNT) offsets[i + 1] = offsets[i] + EmojiData.SEQUENCE_LENGTHS[i].code
        offsets
    }

    private val nameOffsets: IntArray by lazy { separatorOffsets(EmojiData.NAMES, EmojiData.ENTRY_COUNT) }
    private val subgroups: List<String> by lazy { EmojiData.SUBGROUPS.split(SEPARATOR) }
    private val versions: List<String> by lazy { EmojiData.VERSIONS.split(SEPARATOR) }
    private val entries = arrayOfNulls<EmojiInfo>(EmojiData.ENTRY_COUNT)

    val all: List<EmojiInfo> by lazy { List(EmojiData.ENTRY_COUNT) { entry(it) } }

    /** Start of each of [count] items of [text] separated by [SEPARATOR], plus the end of the text + 1. */
    private fun separatorOffsets(text: String, count: Int): IntArray {
        val offsets = IntArray(count + 1)
        var item = 1
        for (i in text.indices) {
            if (text[i] == SEPARATOR) offsets[item++] = i + 1
        }
        offsets[count] = text.length + 1
        return offsets
    }

    fun entry(index: Int): EmojiInfo {
        entries[index]?.let { return it }
        val sequence = EmojiData.ENTRY_SEQUENCES[index].code
        val offsets = sequenceOffsets
        val names = nameOffsets
        val info = EmojiInfo(
            fullyQualified = EmojiData.SEQUENCES.substring(offsets[sequence], offsets[sequence + 1]),
            status = EmojiStatus.entries[EmojiData.SEQUENCE_STATUSES[sequence].code],
            name = EmojiData.NAMES.substring(names[index], names[index + 1] - 1),
            group = EmojiGroup.entries[EmojiData.ENTRY_GROUPS[index].code],
            subgroup = subgroups[EmojiData.ENTRY_SUBGROUPS[index].code],
            version = versions[EmojiData.ENTRY_VERSIONS[index].code],
        )
        entries[index] = info
        return info
    }

    /** Index of [sequence] among the sorted sequences, or -1. */
    fun find(sequence: CharSequence): Int {
        val offsets = sequenceOffsets
        val sequences = EmojiData.SEQUENCES
        var low = 0
        var high = EmojiData.SEQUENCE_COUNT - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val comparison = compare(sequence, sequences, offsets[mid], offsets[mid + 1])
            when {
                comparison < 0 -> high = mid - 1
                comparison > 0 -> low = mid + 1
                else -> return mid
            }
        }
        return -1
    }

    /** Compares [sequence] with `sequences[start, end)` by UTF-16 code units, as the generator sorted them. */
    private fun compare(sequence: CharSequence, sequences: String, start: Int, end: Int): Int {
        val length = end - start
        val common = minOf(sequence.length, length)
        for (i in 0 until common) {
            val difference = sequence[i].code - sequences[start + i].code
            if (difference != 0) return difference
        }
        return sequence.length - length
    }
}
