package me.zolotov.kodepoint.generator

import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

/**
 * Codepoints whose value differs between two Unicode Character Database releases, per validated property.
 *
 * The exhaustive `ValidationTest` compares tables generated from one Unicode version against
 * `java.lang.Character` of a JDK that may implement another. The mismatches it observes must be
 * exactly this diff: nothing unexplained by the UCD delta, and nothing in the delta that the tables
 * failed to pick up. Keys are the `ValidationTest` test names.
 */
class UcdDiff(val from: UnicodeVersion, val to: UnicodeVersion, val changes: Map<String, List<Int>>) {
    val isEmpty: Boolean get() = changes.values.all { it.isEmpty() }
}

/**
 * The properties `ValidationTest` compares, extracted the way the tables encode them
 * (and, where the test compares a composite, the same composite).
 */
private val VALIDATED_PROPERTIES: Map<String, (UnicodeData, Int) -> Any> = linkedMapOf(
    "isLetter" to { d, cp -> d.characters[cp].isLetter },
    "isDigit" to { d, cp -> d.characters[cp].isDigit },
    "isLetterOrDigit" to { d, cp -> d.characters[cp].isLetter || d.characters[cp].isDigit },
    "isUpperCase" to { d, cp -> d.characters[cp].isUpperCase },
    "isLowerCase" to { d, cp -> d.characters[cp].isLowerCase },
    "toLowerCase" to { d, cp -> d.characters[cp].lowerCase.takeIf { it >= 0 } ?: cp },
    "toUpperCase" to { d, cp -> d.characters[cp].upperCase.takeIf { it >= 0 } ?: cp },
    "isSpaceChar" to { d, cp -> d.characters[cp].isSpaceChar },
    "isWhitespace" to { d, cp -> d.characters[cp].isWhitespace },
    "isIdeographic" to { d, cp -> d.characters[cp].isIdeographic },
    "isIdentifierIgnorable" to { d, cp -> JavaIdentifierRules.isIdentifierIgnorable(cp, d.characters[cp].category) },
    "isUnicodeIdentifierStart" to { d, cp -> d.characters[cp].isIdStart },
    "isUnicodeIdentifierPart" to { d, cp ->
        d.characters[cp].isIdContinue || JavaIdentifierRules.isIdentifierIgnorable(cp, d.characters[cp].category)
    },
    "isJavaIdentifierStart" to { d, cp -> d.characters[cp].isJavaIdentifierStart },
    "isJavaIdentifierPart" to { d, cp -> d.characters[cp].isJavaIdentifierPart },
    "isISOControl" to { d, cp -> d.characters[cp].isISOControl },
    "getScript" to { d, cp -> d.scripts[cp] ?: "Unknown" },
    "getCategory" to { d, cp -> d.characters[cp].category },
)

fun computeUcdDiff(from: UnicodeVersion, fromData: UnicodeData, to: UnicodeVersion, toData: UnicodeData): UcdDiff {
    val changes = VALIDATED_PROPERTIES.mapValues { (_, property) ->
        (0..MAX_CODEPOINT).filter { cp -> property(fromData, cp) != property(toData, cp) }
    }
    return UcdDiff(from, to, changes)
}

/**
 * Writes the diff between the UCD releases [from] and [to] into [output], one `<property> <hex codepoint>`
 * per line. Equal versions produce a header-only file without touching the UCD at all.
 */
fun generateUcdDiff(from: UnicodeVersion, to: UnicodeVersion, cacheDir: Path, output: Path) {
    val diff = if (from == to) {
        UcdDiff(from, to, emptyMap())
    } else {
        computeUcdDiff(from, loadUnicodeData(cacheDir, from), to, loadUnicodeData(cacheDir, to))
    }
    output.createParentDirectories().writeText(diff.toText())
    val total = diff.changes.values.sumOf { it.size }
    println("UCD diff $from -> $to: $total changed (property, codepoint) pairs written to $output")
}

fun UcdDiff.toText(): String = buildString {
    appendLine("# UCD diff: $from -> $to")
    appendLine("# <property> <codepoint>: values differ between the two releases")
    for ((property, codepoints) in changes) {
        for (cp in codepoints) {
            append(property).append(' ').appendLine(cp.toString(16).uppercase().padStart(4, '0'))
        }
    }
}
