# Kodepoint - Kotlin Multiplatform Unicode Library

[![Maven central version](https://img.shields.io/maven-central/v/me.zolotov.kodepoint/kodepoint.svg)](https://search.maven.org/artifact/me.zolotov.kodepoint/kodepoint)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/zolotov/kodepoint/test.yml)](https://github.com/zolotov/kodepoint/actions/workflows/test.yml)
[![GitHub License](https://img.shields.io/github/license/zolotov/kodepoint)](https://github.com/zolotov/kodepoint/blob/main/LICENSE)

A Kotlin multiplatform library providing limited Unicode Character Database functionality.

> **Note**: This project serves as a temporary solution until [KT-23251 Extend Unicode support in Kotlin common](https://youtrack.jetbrains.com/issue/KT-23251) and [KT-24908 CodePoint inline class](https://youtrack.jetbrains.com/issue/KT-24908) are implemented in the Kotlin standard library.

## Overview

Kodepoint provides Unicode character property access across multiple platforms:

- **JVM**: Uses Java's `Character` class
- **All other targets**: Pure Kotlin implementations with generated Unicode lookup tables

## Installation

Add the following dependency to your project:

```kotlin
dependencies {
    implementation("me.zolotov.kodepoint:kodepoint:$version")
}
```

## Usage

```kotlin
import me.zolotov.kodepoint.*

// Create a Codepoint
val codepoint = Codepoint(0x1F600) // 😀

val isLetter = codepoint.isLetter()
val upperCase = codepoint.toUpperCase()

// String conversion
val string = codepoint.asString()

// Work with CharSequence
val text = "Hello 👋 World"
text.forEachCodepoint { cp ->
    println("U+${cp.codepoint.toString(16).uppercase()}: ${cp.asString()}")
}

// Append to StringBuilder
val sb = StringBuilder()
sb.appendCodePoint(Codepoint(0x1F44D)) // 👍
```

## Architecture

For detailed technical documentation on data storage, lookup tables, and platform-specific implementations, see [ARCHITECTURE.md](ARCHITECTURE.md).

### `lib` - Main Library Module

Core library providing Unicode character functionality through `@JvmInline value class Codepoint`.

- Unicode property API (case conversion, character classification, script detection)
- Platform-specific implementations with fallbacks
- Surrogate pair handling for supplementary Unicode characters
- Extensions for `CharSequence` and `Appendable`

### `unicode` - Unicode Data Module

Contains generated Unicode property lookup tables.

- Unicode database processing and lookup tables
- Used by `lib` for non-JVM platforms

### `common` - Shared Module

Contains `UnicodeScript` enum accessible from both `unicode` and `lib` modules.

- Unicode script definitions generated from Unicode Character Database
- Shared across all platform-specific implementations

## Supported Targets

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

> **Note**: Only JVM and WasmJS targets are actively tested. Other targets compile and should work correctly, but have not been thoroughly validated.

## JVM Compatibility

This library aims to provide consistent Unicode behavior across all platforms. 
On non-JVM platforms, it uses generated Unicode Character Database lookup tables. 
The implementation is validated against JVM's `java.lang.Character` methods.

### Fully Compatible Functions

The following functions produce **identical results** to JVM's `Character` class for all 1,114,112 Unicode codepoints:

- `isLetter()`, `isDigit()`, `isLetterOrDigit()`
- `isUpperCase()`, `isLowerCase()`
- `toLowerCase()`, `toUpperCase()`
- `isSpaceChar()`
- `isIdeographic()`
- `isIdentifierIgnorable()`
- `isISOControl()`
- `isJavaIdentifierStart()`, `isJavaIdentifierPart()` - generated directly from JVM's `Character` class during build

### Known Differences

Some functions have intentional differences from JVM behavior to maintain Unicode standard compliance:

#### `isWhitespace()`

This library uses Unicode's `White_Space` property, which differs from Java's `Character.isWhitespace()`:

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

JVM includes U+2E2F (VERTICAL TILDA) for backward compatibility, but this character is not in Unicode's `ID_Start` or `ID_Continue` properties. This library follows the Unicode standard.

## Building

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew allTests
```

## Benchmarks

```bash
# Full benchmarks (5 warmups, 5 iterations)
./gradlew :benchmarks:benchmark

# Quick benchmarks (2 warmups, 3 iterations)
./gradlew :benchmarks:quickBenchmark
```

### Benchmark Categories

**CodepointsBenchmark** – Measures performance across different character sets:
- ASCII characters (fast-path optimization)
- Latin Extended (U+0100-024F)
- Greek (U+0370-03FF)
- CJK ideographs (U+4E00-4E7F)
- Mixed workloads (80/20, 50/50, 20/80 ASCII/Unicode ratios)

**JvmComparisonBenchmark** – Direct comparison with `java.lang.Character`:
- `isLetter`, `isDigit`, `toLowerCase`, `toUpperCase`
- `isWhitespace`, `isJavaIdentifierStart`, `isJavaIdentifierPart`

Results are written to `benchmarks/build/reports/benchmarks/`.