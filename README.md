# Kodepoint

**Unicode code points for Kotlin Multiplatform — the `Character` API you've been missing in common code.**

[![Maven central version](https://img.shields.io/maven-central/v/me.zolotov.kodepoint/kodepoint.svg)](https://search.maven.org/artifact/me.zolotov.kodepoint/kodepoint)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/zolotov/kodepoint/test.yml)](https://github.com/zolotov/kodepoint/actions/workflows/test.yml)
[![GitHub License](https://img.shields.io/github/license/zolotov/kodepoint)](https://github.com/zolotov/kodepoint/blob/main/LICENSE)

Kodepoint brings Unicode Character Database queries — case conversion, character classification, script and category lookup, and correct code-point iteration — to **every Kotlin target** through a single, allocation-free `Codepoint` value class.

```kotlin
val emoji = Codepoint(0x1F600)   // 😀
emoji.isLetter()                 // false
emoji.getUnicodeScript()         // COMMON
Codepoint('A'.code).toLowerCase().asString()  // "a"
```

## Why Kodepoint?

Kotlin's common standard library has no equivalent of Java's `Character`. In shared code you can't ask whether a character is a letter, uppercase it, or safely walk a string by code point — and Kotlin's `Char` is only 16 bits, so emoji, CJK extensions, and other supplementary characters (anything above U+FFFF) silently break naive per-`Char` logic.

Kodepoint fills that gap:

- **📦 Works everywhere** — one API across JVM, JS, WasmJS, iOS, macOS, watchOS, tvOS, Linux, and Windows. Write your text logic once in `commonMain`.
- **🧩 Full Unicode range** — first-class support for supplementary code points and surrogate pairs, so 😀 and 𝕏 are handled correctly.
- **⚡ Zero-allocation** — `Codepoint` is a `@JvmInline value class` wrapping a single `Int`. No boxing, no wrapper objects on the hot path.
- **✅ JVM-validated correctness** — non-JVM results are checked against `java.lang.Character` across **all 1,114,112 code points** (see [JVM Compatibility](#jvm-compatibility)).
- **🪶 No runtime dependencies** — pure Kotlin plus compact generated lookup tables. On the JVM it delegates straight to `java.lang.Character`.
- **📈 Benchmarked** — performance tracked over time on a [public dashboard](https://zolotov.github.io/kodepoint).

> **Note:** This project is a stopgap until [KT-23251 (Extend Unicode support in Kotlin common)](https://youtrack.jetbrains.com/issue/KT-23251) and [KT-24908 (CodePoint inline class)](https://youtrack.jetbrains.com/issue/KT-24908) land in the Kotlin standard library.

## Installation

Kodepoint is published to Maven Central. Add it to your `commonMain` dependencies:

```kotlin
// build.gradle.kts
dependencies {
    implementation("me.zolotov.kodepoint:kodepoint:1.0.1")
}
```

<sub>Check the [badge above](#kodepoint) or [Maven Central](https://search.maven.org/artifact/me.zolotov.kodepoint/kodepoint) for the latest version.</sub>

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

### Iterate a string by code point — surrogate-safe

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

## API reference

### `Codepoint`

A `value class` wrapping an `Int` code point.

| Member | Returns | Description |
|--------|---------|-------------|
| `Codepoint(Int)` | `Codepoint` | Construct from a code-point value. |
| `Codepoint.fromChars(high, low)` | `Codepoint` | Combine a high/low surrogate pair. |
| `codepoint` | `Int` | The raw code-point value. |
| `charCount` | `Int` | UTF-16 units needed (1 or 2). |
| `asString()` | `String` | Encode as a `String`. |

**Classification**

| Method | Description |
|--------|-------------|
| `isLetter()` | Letter (Lu, Ll, Lt, Lm, Lo). |
| `isDigit()` | Decimal digit (Nd). |
| `isLetterOrDigit()` | Letter or digit. |
| `isUpperCase()` / `isLowerCase()` | Upper- / lowercase letter. |
| `isSpaceChar()` | Unicode space (Zs, Zl, Zp). |
| `isWhitespace()` | Whitespace ([see differences](#iswhitespace)). |
| `isIdeographic()` | Ideographic character. |
| `isISOControl()` | Control character (Cc). |
| `isIdentifierIgnorable()` | Ignorable in identifiers. |
| `isUnicodeIdentifierStart()` / `isUnicodeIdentifierPart()` | Unicode identifier rules. |
| `isJavaIdentifierStart()` / `isJavaIdentifierPart()` | Java identifier rules. |

**Conversion & metadata**

| Method | Returns | Description |
|--------|---------|-------------|
| `toUpperCase()` / `toLowerCase()` | `Codepoint` | Case mapping (unchanged if none). |
| `getCategory()` | `Category` | General category (e.g. `UPPERCASE_LETTER`). |
| `getUnicodeScript()` | `UnicodeScript` | Unicode script (e.g. `LATIN`, `HAN`). |

### `CharSequence` extensions

| Extension | Returns | Description |
|-----------|---------|-------------|
| `forEachCodepoint { cp -> }` | `Unit` | Iterate code points left-to-right (surrogate-safe, `inline`). |
| `forEachCodepointReversed { cp -> }` | `Unit` | Iterate right-to-left. |
| `codePointAt(index)` | `Codepoint` | Code point starting at `index`. |
| `codePointBefore(index)` | `Codepoint` | Code point ending before `index`. |
| `codepoints(offset, direction)` | `Iterator<Codepoint>` | Lazy iterator (`Direction.FORWARD` / `BACKWARD`). |

### `Appendable` extensions

| Extension | Description |
|-----------|-------------|
| `appendCodePoint(Int)` | Append a code point (as its surrogate pair when needed). |
| `appendCodePoint(Codepoint)` | Same, taking a `Codepoint`. |

## Supported targets

| Platform | Targets |
|----------|---------|
| JVM | `jvm` |
| JavaScript | `js`, `wasmJs` |
| iOS | `iosArm64`, `iosSimulatorArm64`, `iosX64` |
| macOS | `macosArm64`, `macosX64` |
| tvOS | `tvosArm64`, `tvosSimulatorArm64`, `tvosX64` |
| watchOS | `watchosArm64`, `watchosSimulatorArm64`, `watchosX64` |
| Linux | `linuxArm64`, `linuxX64` |
| Windows | `mingwX64` |

> **Note:** Only JVM and WasmJS targets are actively tested in CI. Other targets compile and should work correctly, but have not been thoroughly validated.

## JVM compatibility

Kodepoint aims for consistent Unicode behavior across all platforms. On the JVM it delegates to `java.lang.Character`; elsewhere it uses generated Unicode Character Database lookup tables. Non-JVM output is validated against the JVM implementation.

### Fully compatible functions

These produce **identical results** to `java.lang.Character` for all 1,114,112 Unicode code points:

- `isLetter()`, `isDigit()`, `isLetterOrDigit()`
- `isUpperCase()`, `isLowerCase()`
- `toLowerCase()`, `toUpperCase()`
- `isSpaceChar()`
- `isIdeographic()`
- `isIdentifierIgnorable()`
- `isISOControl()`
- `isJavaIdentifierStart()`, `isJavaIdentifierPart()` — generated directly from `java.lang.Character` during the build

### Known differences

A few functions intentionally differ from JVM behavior to follow the Unicode standard more closely.

#### `isWhitespace()`

This library uses Unicode's `White_Space` property, which differs from `Character.isWhitespace()`:

| Codepoint | Character | Unicode White_Space | Java isWhitespace |
|-----------|-----------|---------------------|-------------------|
| U+001C | File Separator | `false` | `true` |
| U+001D | Group Separator | `false` | `true` |
| U+001E | Record Separator | `false` | `true` |
| U+001F | Unit Separator | `false` | `true` |
| U+0085 | Next Line (NEL) | `true` | `false` |
| U+00A0 | No-Break Space | `true` | `false` |
| U+2007 | Figure Space | `true` | `false` |
| U+202F | Narrow No-Break Space | `true` | `false` |

Java excludes non-breaking spaces from `isWhitespace()` and includes control characters that Unicode does not classify as whitespace.

#### `isUnicodeIdentifierStart()` / `isUnicodeIdentifierPart()`

JVM includes U+2E2F (VERTICAL TILDE) for backward compatibility, but this character is not in Unicode's `ID_Start` or `ID_Continue` properties. This library follows the Unicode standard.

## How it works

Kodepoint is split into three modules:

- **`lib`** (`me.zolotov.kodepoint:kodepoint`) — the public API: the `Codepoint` value class plus `CharSequence`/`Appendable` extensions, with platform-specific implementations.
- **`unicode`** — generated Unicode property lookup tables used by non-JVM targets.
- **`common`** — the `UnicodeScript` enum shared across modules.

For a deep dive into data storage, lookup-table layout, and platform implementations, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Benchmarks

Benchmark results — including history and comparisons against `java.lang.Character` — are published to the [dashboard](https://zolotov.github.io/kodepoint). Commands, categories, and the reporting pipeline are documented in [`benchmarks/README.md`](benchmarks/README.md).

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew allTests
```

## Contributing

Issues and pull requests are welcome. If you hit a code point that behaves differently from `java.lang.Character` (outside the [documented differences](#known-differences)), please [open an issue](https://github.com/zolotov/kodepoint/issues).

## License

Licensed under the [Apache License 2.0](LICENSE).
