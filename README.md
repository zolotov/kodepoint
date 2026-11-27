# Kodepoint

**Lightweight Unicode code-point handling for Kotlin Multiplatform – the `Character` API you've been missing in common code, without depending on ICU.**

[![Maven central version](https://img.shields.io/maven-central/v/me.zolotov.kodepoint/kodepoint.svg)](https://search.maven.org/artifact/me.zolotov.kodepoint/kodepoint)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/zolotov/kodepoint/test.yml)](https://github.com/zolotov/kodepoint/actions/workflows/test.yml)
[![GitHub License](https://img.shields.io/github/license/zolotov/kodepoint)](https://github.com/zolotov/kodepoint/blob/main/LICENSE)

Kodepoint brings Unicode Character Database queries – case conversion, character classification, script and category lookup, and correct code-point iteration – to **every Kotlin target** through a single, allocation-free `Codepoint` value class.

```kotlin
val emoji = Codepoint(0x1F600)   // 😀
emoji.isLetter()                 // false
emoji.getUnicodeScript()         // COMMON
Codepoint('A'.code).toLowerCase().asString()  // "a"
```

## Why Kodepoint?

Kotlin's common standard library has no equivalent of Java's `Character`. In shared code you can't ask whether a character is a letter, uppercase it, or safely walk a string by code point – and Kotlin's `Char` is only 16 bits, so emoji, CJK extensions, and other supplementary characters (anything above U+FFFF) silently break naive per-`Char` logic.

Kodepoint fills that gap:

- **📦 Works everywhere** – one API across JVM, JS, WasmJS, WasmWasi, iOS, macOS, watchOS, tvOS, Linux, and Windows. Write your text logic once in `commonMain`.
- **🧩 Full Unicode range** – first-class support for supplementary code points and surrogate pairs, so 😀 and 𝕏 are handled correctly.
- **⚡ Zero-allocation** – `Codepoint` is a `@JvmInline value class` wrapping a single `Int`. No boxing, no wrapper objects on the hot path.
- **✅ JVM-validated correctness** – non-JVM results are checked against `java.lang.Character` across **all 1,114,112 code points** (see [JVM Compatibility](#jvm-compatibility)).
- **🪶 No runtime dependencies** – pure Kotlin plus compact generated lookup tables. On the JVM it delegates straight to `java.lang.Character`.
- **📈 Benchmarked** – performance tracked over time on a [public dashboard](https://zolotov.github.io/kodepoint).

> **Note:** This project is a stopgap until [KT-23251 (Extend Unicode support in Kotlin common)](https://youtrack.jetbrains.com/issue/KT-23251) and [KT-24908 (CodePoint inline class)](https://youtrack.jetbrains.com/issue/KT-24908) land in the Kotlin standard library.

## When should I use Kodepoint?

Reach for Kodepoint whenever you're in Kotlin Multiplatform / `commonMain` and need something the common standard library doesn't offer:

- **"How do I check if a character is a letter/digit/uppercase in Kotlin common code?"** → `Codepoint(c).isLetter()`, `.isDigit()`, `.isUpperCase()` – the `java.lang.Character` equivalents, but multiplatform.
- **"How do I uppercase/lowercase a code point in KMP?"** → `codepoint.toUpperCase()` / `.toLowerCase()`.
- **"How do I iterate a string by Unicode code point without breaking emoji?"** → `text.forEachCodepoint { ... }` (surrogate-safe, unlike `for (c in text)`).
- **"How do I count real characters when a string contains emoji or CJK?"** → `text.graphemeClusterCount()` from [`kodepoint-graphemes`](#grapheme-clusters--emoji) counts what users see as one character (👨‍👩‍👧, 🇺🇦, é), `forEachCodepoint` counts code points.
- **"How do I split a string into user-perceived characters, or move a cursor by one, in KMP?"** → `forEachGraphemeCluster`, `nextGraphemeClusterBoundary` / `previousGraphemeClusterBoundary` ([grapheme clusters](#grapheme-clusters--emoji)).
- **"How do I find emoji in a string, or get an emoji's name?"** → `forEachEmoji`, `Emoji.lookup("😀")?.name` ([emoji](#grapheme-clusters--emoji)).
- **"How do I get a character's Unicode script or general category in common code?"** → `codepoint.getUnicodeScript()` / `.getCategory()`.
- **"How do I append a supplementary code point (above U+FFFF) to a `StringBuilder`?"** → `sb.appendCodePoint(codepoint)`.
- **"How do I validate Java/Unicode identifiers in KMP?"** → `.isJavaIdentifierStart()`, `.isUnicodeIdentifierPart()`, etc.

Kodepoint covers code-point-level UCD properties and, in the separate `kodepoint-graphemes` artifact, extended grapheme-cluster segmentation (UAX #29) and emoji. It is **not** an ICU replacement – for locale-aware collation, Unicode normalization (NFC/NFD), bidi, or word, sentence and line breaking, use a dedicated text library.

## Installation

Kodepoint is published to Maven Central. Add it to your `commonMain` dependencies:

```kotlin
// build.gradle.kts
dependencies {
    implementation("me.zolotov.kodepoint:kodepoint:4.0.0")
}
```

## Quick start

```kotlin
import me.zolotov.kodepoint.*

// Create a code point from an Int...
val grinning = Codepoint(0x1F600)      // 😀
// ...or from a Char.
val a = Codepoint('A'.code)

// Query Unicode properties.
a.isLetter()          // true
a.isUpperCase()       // true
a.toLowerCase()       // Codepoint(0x61) -> "a"
a.getCategory()       // Category.UPPERCASE_LETTER
grinning.getUnicodeScript()  // UnicodeScript.COMMON

// Convert back to text.
grinning.asString()   // "😀"
```

### Iterate a string by code point – surrogate-safe

Walking a string with `for (c in text)` splits emoji and other supplementary
characters into broken surrogate halves. `forEachCodepoint` gives you whole
characters:

```kotlin
val text = "Hi 👋🏽 世界"

text.forEachCodepoint { cp ->
    println("U+%04X %s".format(cp.codepoint, cp.asString()))
}
// U+0048 H
// U+0069 i
// U+0020
// U+1F44B 👋
// U+1F3FD 🏽
// U+4E16 世
// U+754C 界
```

### Count real characters, build strings

```kotlin
// Count code points, not UTF-16 chars ("😀".length == 2).
var count = 0
"a😀b".forEachCodepoint { count++ }   // 3

// Append a supplementary code point to any Appendable.
val sb = StringBuilder()
sb.appendCodePoint(Codepoint(0x1F44D))   // 👍
sb.appendCodePoint('!'.code)
sb.toString()   // "👍!"
```

## Grapheme clusters & emoji

What a user sees as one character is often several code points: `e` + U+0301 is é, 👨‍👩‍👧 is five code points
joined by U+200D, 🇺🇦 is two regional indicators, क्ष is three code points. Unicode calls these units
*extended grapheme clusters* (UAX #29). The `kodepoint-graphemes` artifact finds them, and identifies the
clusters that are emoji:

```kotlin
// build.gradle.kts
dependencies {
    implementation("me.zolotov.kodepoint:kodepoint-graphemes:4.0.0")
}
```

```kotlin
import me.zolotov.kodepoint.graphemes.*

val text = "Hi 👋🏽 👨‍👩‍👧 🇺🇦"
text.length                       // 21 UTF-16 chars
text.graphemeClusterCount()       // 8: H, i, space, 👋🏽, space, 👨‍👩‍👧, space, 🇺🇦
text.graphemeClusters().toList()  // ["H", "i", " ", "👋🏽", " ", "👨‍👩‍👧", " ", "🇺🇦"]

// Allocation-free iteration over UTF-16 ranges, in both directions.
text.forEachGraphemeCluster { start, end -> /* text.substring(start, end) */ }
text.forEachGraphemeClusterReversed { start, end -> }

// Move a cursor by one user-perceived character.
text.nextGraphemeClusterBoundary(3)      // 7: past 👋🏽
text.previousGraphemeClusterBoundary(7)  // 3
text.isGraphemeClusterBoundary(4)        // false: inside 👋🏽

// Emoji in a text, and their metadata.
text.forEachEmoji { start, end, kind -> println("$start..<$end $kind") }  // 3..<7 MODIFIER, 8..<16 ZWJ, 17..<21 FLAG
Emoji.lookup("👋🏽")?.name          // "waving hand: medium skin tone"
Emoji.lookup("☺")?.fullyQualified   // "☺️" (the lookup was UNQUALIFIED)
Emoji.isRgi("🇦🇦")                  // false: a well-formed flag sequence, but no region
```

The segmentation follows the Unicode 18.0.0 rules on **every target, the JVM included**: unlike the
`Codepoint` queries, which delegate to `java.lang.Character` on the JVM, this module always uses its own
generated tables. `java.text.BreakIterator` only implements extended grapheme clusters since JDK 20 and
follows the JDK's Unicode version (16.0 on JDK 24 and 25), so results would otherwise depend on the
user's JDK.

| API                                                                        | Description                                                                         |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| `CharSequence.forEachGraphemeCluster { start, end -> }`                    | Clusters left to right, as UTF-16 ranges (`inline`, allocation-free).               |
| `CharSequence.forEachGraphemeClusterReversed { start, end -> }`            | The same clusters, right to left.                                                   |
| `CharSequence.graphemeClusterCount()`                                      | Number of clusters.                                                                 |
| `CharSequence.graphemeClusters()`                                          | `Sequence<String>` of the clusters (allocates).                                     |
| `CharSequence.isGraphemeClusterBoundary(index)`                            | Whether a cluster starts or ends at `index` in `0..length`.                         |
| `CharSequence.nextGraphemeClusterBoundary(index)` / `previous…(index)`     | Nearest boundary after / before `index`.                                            |
| `GraphemeClusterSegmenter().isBoundaryBefore(codepoint)`                   | Streaming segmentation of code points that are not in a `CharSequence`.             |
| `Codepoint.getGraphemeClusterBreak()` / `.getIndicConjunctBreak()`         | The `Grapheme_Cluster_Break` and `Indic_Conjunct_Break` properties.                 |
| `Codepoint.isExtendedPictographic()`, `.isEmoji()`, `.isEmojiPresentation()`, `.isEmojiModifier()`, `.isEmojiModifierBase()`, `.isEmojiComponent()` | The UTS #51 emoji properties. |
| `CharSequence.forEachEmoji { start, end, kind -> }`                        | Clusters that are well-formed emoji sequences, with their `EmojiSequenceKind`.      |
| `CharSequence.isEmojiSequence()`                                           | Whether the whole text is one emoji sequence.                                       |
| `Emoji.lookup(sequence)`                                                   | `EmojiInfo` (name, group, subgroup, version, status) of any `emoji-test.txt` line.  |
| `Emoji.isRgi(sequence)` / `Emoji.all()`                                    | The RGI emoji set: fully-qualified emoji and components.                            |
| `Graphemes.UNICODE_VERSION` / `Emoji.VERSION`                              | `18.0.0` / `18.0`.                                                                  |

`forEachEmoji` follows the UTS #51 grammar, in which a single character with the `Emoji` property is
an emoji sequence even when it is shown as text by default, so it also reports the digits, `#`, `*`, `©`
and `®`. Check `Codepoint.isEmojiPresentation()` or `Emoji.isRgi` to narrow the result.

Legacy grapheme clusters, tailorings, display width, and word, sentence and line breaking are out of
scope. The segmentation tables take about 16 KB. The emoji metadata is larger: about 110,000 characters of
names plus the sequences, about 340 KB once loaded as UTF-16 strings, and it is only loaded when `Emoji`
is first used. `./gradlew :graphemes:graphemeDataMetrics` reports the size of every generated table.

## API reference

### `Codepoint`

A `value class` wrapping an `Int` code point.

| Member                           | Returns     | Description                        |
|----------------------------------|-------------|------------------------------------|
| `Codepoint(Int)`                 | `Codepoint` | Construct from a code-point value. |
| `Codepoint.fromChars(high, low)` | `Codepoint` | Combine a high/low surrogate pair. |
| `codepoint`                      | `Int`       | The raw code-point value.          |
| `charCount`                      | `Int`       | UTF-16 units needed (1 or 2).      |
| `asString()`                     | `String`    | Encode as a `String`.              |

**Classification**

| Method                                                     | Description                                    |
|------------------------------------------------------------|------------------------------------------------|
| `isLetter()`                                               | Letter (Lu, Ll, Lt, Lm, Lo).                   |
| `isDigit()`                                                | Decimal digit (Nd).                            |
| `isLetterOrDigit()`                                        | Letter or digit.                               |
| `isUpperCase()` / `isLowerCase()`                          | Upper- / lowercase letter.                     |
| `isSpaceChar()`                                            | Unicode space (Zs, Zl, Zp).                    |
| `isWhitespace()`                                           | Whitespace ([see differences](#iswhitespace)). |
| `isIdeographic()`                                          | Ideographic character.                         |
| `isISOControl()`                                           | Control character (Cc).                        |
| `isIdentifierIgnorable()`                                  | Ignorable in identifiers.                      |
| `isUnicodeIdentifierStart()` / `isUnicodeIdentifierPart()` | Unicode identifier rules.                      |
| `isJavaIdentifierStart()` / `isJavaIdentifierPart()`       | Java identifier rules.                         |

**Conversion & metadata**

| Method                            | Returns         | Description                                 |
|-----------------------------------|-----------------|---------------------------------------------|
| `toUpperCase()` / `toLowerCase()` | `Codepoint`     | Case mapping (unchanged if none).           |
| `getCategory()`                   | `Category`      | General category (e.g. `UPPERCASE_LETTER`). |
| `getUnicodeScript()`              | `UnicodeScript` | Unicode script (e.g. `LATIN`, `HAN`).       |

### `CharSequence` extensions

| Extension                            | Returns               | Description                                                   |
|--------------------------------------|-----------------------|---------------------------------------------------------------|
| `forEachCodepoint { cp -> }`         | `Unit`                | Iterate code points left-to-right (surrogate-safe, `inline`). |
| `forEachCodepointReversed { cp -> }` | `Unit`                | Iterate right-to-left.                                        |
| `codePointAt(index)`                 | `Codepoint`           | Code point starting at `index`.                               |
| `codePointBefore(index)`             | `Codepoint`           | Code point ending before `index`.                             |
| `codepoints(offset, direction)`      | `Iterator<Codepoint>` | Lazy iterator (`Direction.FORWARD` / `BACKWARD`).             |

### `Appendable` extensions

| Extension                    | Description                                              |
|------------------------------|----------------------------------------------------------|
| `appendCodePoint(Int)`       | Append a code point (as its surrogate pair when needed). |
| `appendCodePoint(Codepoint)` | Same, taking a `Codepoint`.                              |

## Compatibility

|                                 |                                                                                      |
|---------------------------------|--------------------------------------------------------------------------------------|
| **Unicode version**             | 18.0.0 on non-JVM targets; the running JDK's on the JVM – see [Unicode version](#unicode-version). `kodepoint-graphemes`: 18.0.0 everywhere |
| **Kotlin API/language version** | 2.1+                                                                                 |
| **Minimum consumer Kotlin**     | 2.1 on the JVM, 2.2 for all other targets (built with Kotlin 2.2.20)                 |
| **JVM bytecode target**         | 11                                                                                   |
| **Correctness**                 | Non-JVM output validated against `java.lang.Character` for all 1,114,112 code points, up to the documented Unicode version delta |
| **Runtime dependencies**        | None                                                                                 |

## Supported targets

| Platform   | Targets                                               |
|------------|-------------------------------------------------------|
| JVM        | `jvm`                                                 |
| JavaScript | `js`, `wasmJs`                                        |
| WASI       | `wasmWasi`                                            |
| iOS        | `iosArm64`, `iosSimulatorArm64`, `iosX64`             |
| macOS      | `macosArm64`, `macosX64`                              |
| tvOS       | `tvosArm64`, `tvosSimulatorArm64`, `tvosX64`          |
| watchOS    | `watchosArm64`, `watchosSimulatorArm64`, `watchosX64` |
| Linux      | `linuxArm64`, `linuxX64`                              |
| Windows    | `mingwX64`                                            |

> **Note:** Only JVM and WasmJS targets are actively tested in CI. WasmWasi and the other targets compile and should work correctly, but have not been thoroughly validated.

## JVM compatibility

Kodepoint aims for consistent Unicode behavior across all platforms. On the JVM it delegates to `java.lang.Character`; elsewhere it uses generated Unicode Character Database lookup tables. Non-JVM output is validated against the JVM implementation for all 1,114,112 code points, with the expected differences between the two Unicode versions checked exactly (see [Unicode version](#unicode-version)).

### Fully compatible functions

These produce **identical results** to `java.lang.Character` for every code point assigned in the JDK's Unicode version:

- `isLetter()`, `isDigit()`, `isLetterOrDigit()`
- `isUpperCase()`, `isLowerCase()`
- `toLowerCase()`, `toUpperCase()`
- `isSpaceChar()`
- `isIdeographic()`
- `isIdentifierIgnorable()`
- `isISOControl()`
- `isJavaIdentifierStart()`, `isJavaIdentifierPart()` – derived from the Unicode general category following the `java.lang.Character` contract

### Known differences

#### Unicode version

The lookup tables follow **Unicode 18.0.0**, while `java.lang.Character` follows the JDK: JDK 24 and 25 implement Unicode 16.0.0. On the JVM Kodepoint returns the JDK's answers, so until a JDK ships Unicode 18 the two differ exactly by the 16.0 → 18.0 delta:

- **17,810 newly assigned characters** – mostly Seal (11,328), Han (4,322, almost all CJK Unified Ideographs Extension J) and Jurchen (965), plus the new scripts Beria Erfe, Sidetic, Tai Yo and Tolong Siki (17.0) and Proto-Cuneiform (18.0), and additions to Latin, Cuneiform, Tangut, Arabic, Sharada, Katakana, Khitan Small Script, Armenian and Common/Inherited. The JDK reports these as unassigned (`getCategory()` = `UNASSIGNED`, `getUnicodeScript()` = `UNKNOWN`, `isLetter()` = `false`, …); the tables classify them.
- **One re-categorized character** – U+0295 LATIN LETTER PHARYNGEAL VOICED FRICATIVE ʕ moved from `LOWERCASE_LETTER` to `OTHER_LETTER`.
- **48 new case pairs** – Latin (U+A7CE–U+A7D5, U+0277/U+A7DD, U+027C/U+A7E2, U+AB4B/U+AB6C, U+AB4C/U+AB6D and Latin Extended-G U+1DF40–U+1DF7F) and the Beria Erfe alphabet; `toUpperCase()`/`toLowerCase()` map them only on non-JVM targets.
- **New decimal digits** – Tolong Siki U+11DE0–U+11DE9.
- **`UnicodeScript`** gains `BERIA_ERFE`, `JURCHEN`, `PROTO_CUNEIFORM`, `SEAL`, `SIDETIC`, `TAI_YO` and `TOLONG_SIKI`. An exhaustive `when` over the enum needs new branches or an `else`.

The complete per-function list is generated by `./gradlew :unicode:generateUcdDiff` into `unicode/build/generated/resources/ucd-diff/ucd-diff.txt`; the JVM validation test requires the observed differences to match it exactly. Once a JDK implementing Unicode 18 is used, the delta is empty and the platforms agree again.

A few further functions intentionally differ from JVM behavior to follow the Unicode standard more closely.

#### `isWhitespace()`

This library uses Unicode's `White_Space` property, which differs from `Character.isWhitespace()`:

| Codepoint | Character             | Unicode White_Space | Java isWhitespace |
|-----------|-----------------------|---------------------|-------------------|
| U+001C    | File Separator        | `false`             | `true`            |
| U+001D    | Group Separator       | `false`             | `true`            |
| U+001E    | Record Separator      | `false`             | `true`            |
| U+001F    | Unit Separator        | `false`             | `true`            |
| U+0085    | Next Line (NEL)       | `true`              | `false`           |
| U+00A0    | No-Break Space        | `true`              | `false`           |
| U+2007    | Figure Space          | `true`              | `false`           |
| U+202F    | Narrow No-Break Space | `true`              | `false`           |

Java excludes non-breaking spaces from `isWhitespace()` and includes control characters that Unicode does not classify as whitespace.

#### `isUnicodeIdentifierStart()` / `isUnicodeIdentifierPart()`

JVM includes U+2E2F (VERTICAL TILDE) for backward compatibility, but this character is not in Unicode's `ID_Start` or `ID_Continue` properties. This library follows the Unicode standard.

## How it works

Kodepoint is split into four modules:

- **`lib`** (`me.zolotov.kodepoint:kodepoint`) – the public API: the `Codepoint` value class plus `CharSequence`/`Appendable` extensions, with platform-specific implementations.
- **`unicode`** – generated Unicode property lookup tables used by non-JVM targets.
- **`common`** – the `UnicodeScript` enum shared across modules.
- **`graphemes`** (`me.zolotov.kodepoint:kodepoint-graphemes`) – grapheme-cluster segmentation and emoji, with its own generated tables on every target.

For a deep dive into data storage, lookup-table layout, and platform implementations, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Benchmarks

Benchmark results – including history and comparisons against `java.lang.Character` – are published to the [dashboard](https://zolotov.github.io/kodepoint). Commands, categories, and the reporting pipeline are documented in [`benchmarks/README.md`](benchmarks/README.md).

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew allTests
```

Releases and the changelog workflow are described in [RELEASING.md](RELEASING.md).

## Contributing

Issues and pull requests are welcome. If you hit a code point that behaves differently from `java.lang.Character` (outside the [documented differences](#known-differences)), please [open an issue](https://github.com/zolotov/kodepoint/issues).

## License

Licensed under the [Apache License 2.0](LICENSE).
