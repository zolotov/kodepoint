# Kodepoint - Kotlin Multiplatform Unicode Library

A Kotlin multiplatform library providing limited Unicode Character Database functionality.

> **Note**: This project serves as a temporary solution until [KT-23251 Extend Unicode support in Kotlin common](https://youtrack.jetbrains.com/issue/KT-23251) and [KT-24908 CodePoint inline class](https://youtrack.jetbrains.com/issue/KT-24908) are implemented in the Kotlin standard library.

## Overview

Kodepoint provides Unicode character property access across multiple platforms:

- **JVM**: Uses Java's `Character` class
- **WASM**: Pure Kotlin implementations

## Architecture

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

## Platforms

- **JVM**
- **WASM**

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
for (cp in text.codepoints()) {
    println("U+${cp.codepoint.toString(16).uppercase()}: ${cp.asString()}")
}

// Append to StringBuilder
val sb = StringBuilder()
sb.appendCodePoint(Codepoint(0x1F44D)) // 👍
```µ