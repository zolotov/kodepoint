package me.zolotov.kodepoint.graphemes.internal

import me.zolotov.kodepoint.graphemes.EmojiSequenceKind
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.EMOJI_BIT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.EMOJI_MODIFIER_BASE_BIT
import me.zolotov.kodepoint.graphemes.generated.GraphemeData.EMOJI_MODIFIER_BIT

/*
 * The UTS #51 grammar of a well-formed emoji sequence:
 *
 *   emoji_sequence        := emoji_core_sequence | emoji_zwj_sequence | emoji_tag_sequence   (ED-17)
 *   emoji_core_sequence   := emoji_character | emoji_presentation_sequence | emoji_keycap_sequence
 *                          | emoji_modifier_sequence | emoji_flag_sequence                     (ED-15)
 *   emoji_presentation_sequence := emoji_character U+FE0F                                     (ED-9a)
 *   emoji_keycap_sequence := [0-9#*] U+FE0F U+20E3                                            (ED-14c)
 *   emoji_modifier_sequence := emoji_modifier_base emoji_modifier                             (ED-13)
 *   emoji_flag_sequence   := regional_indicator regional_indicator                            (ED-14)
 *   emoji_tag_sequence    := tag_base tag_spec tag_end                                        (ED-14a)
 *   tag_base              := emoji_character | emoji_modifier_sequence | emoji_presentation_sequence
 *   tag_spec              := [U+E0020-U+E007E]+
 *   tag_end               := U+E007F
 *   emoji_zwj_sequence    := emoji_zwj_element ( U+200D emoji_zwj_element )+                  (ED-16)
 *   emoji_zwj_element     := emoji_core_sequence | emoji_tag_sequence                         (ED-15a)
 */

private const val PRESENTATION_SELECTOR = 0xFE0F
private const val COMBINING_KEYCAP = 0x20E3
private const val ZWJ = 0x200D
private const val TAG_END = 0xE007F

private val BASIC = EmojiSequenceKind.BASIC.ordinal
private val KEYCAP = EmojiSequenceKind.KEYCAP.ordinal
private val FLAG = EmojiSequenceKind.FLAG.ordinal
private val TAG = EmojiSequenceKind.TAG.ordinal
private val MODIFIER = EmojiSequenceKind.MODIFIER.ordinal
private val ZWJ_SEQUENCE = EmojiSequenceKind.ZWJ.ordinal

private fun isRegionalIndicator(codepoint: Int): Boolean = codepoint in 0x1F1E6..0x1F1FF
private fun isTagSpec(codepoint: Int): Boolean = codepoint in 0xE0020..0xE007E
private fun isKeycapBase(codepoint: Int): Boolean =
    codepoint in '0'.code..'9'.code || codepoint == '#'.code || codepoint == '*'.code

/**
 * The [EmojiSequenceKind] ordinal of `text[start, end)` if it is one well-formed emoji sequence,
 * otherwise -1. Allocates nothing.
 */
@PublishedApi
internal fun emojiSequenceKind(text: CharSequence, start: Int, end: Int): Int {
    if (start >= end) return -1
    var elements = 0
    var kind = -1
    var index = start
    while (true) {
        val element = zwjElement(text, index, end)
        if (element < 0) return -1
        kind = (element and 0xFF).toInt()
        index = (element ushr 8).toInt()
        elements++
        if (index == end) break
        if (codepointAt(text, index) != ZWJ) return -1
        index++
        if (index == end) return -1
    }
    return if (elements > 1) ZWJ_SEQUENCE else kind
}

/**
 * Parses an `emoji_zwj_element` at [start]: returns `(end shl 8) or kind`, or -1 if there is none.
 */
private fun zwjElement(text: CharSequence, start: Int, end: Int): Long {
    val first = codepointAt(text, start)
    var index = start + charCount(first)
    if (isRegionalIndicator(first) && index < end) {
        val second = codepointAt(text, index)
        if (isRegionalIndicator(second)) return element(index + charCount(second), FLAG)
    }
    val props = properties(first)
    if (props and EMOJI_BIT == 0) return -1L
    var kind = BASIC
    if (index < end) {
        val next = codepointAt(text, index)
        if (next == PRESENTATION_SELECTOR) {
            index++
            if (isKeycapBase(first) && index < end && codepointAt(text, index) == COMBINING_KEYCAP) {
                return element(index + 1, KEYCAP)
            }
        } else if (props and EMOJI_MODIFIER_BASE_BIT != 0 && properties(next) and EMOJI_MODIFIER_BIT != 0) {
            index += charCount(next)
            kind = MODIFIER
        }
    }
    // emoji_tag_sequence, with this core as its tag_base
    if (index < end && isTagSpec(codepointAt(text, index))) {
        while (index < end && isTagSpec(codepointAt(text, index))) index += 2
        if (index >= end || codepointAt(text, index) != TAG_END) return -1L
        return element(index + 2, TAG)
    }
    return element(index, kind)
}

private fun element(end: Int, kind: Int): Long = (end.toLong() shl 8) or kind.toLong()
