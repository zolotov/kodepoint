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

### `generator` - Code Generation Plugin

Amper build plugin that generates Unicode property lookup tables at build time.

- Unicode database processing
- Code generation at `build/tasks/_lib_generate@generator/`

## Platforms

- **JVM**
- **WASM**

## Building

```bash
# Build all modules
./amper build

# Run tests
./amper test
```

## Usage

```kotlin
import me.zolotov.kodepoint.*

// Create a Codepoint
val codepoint = Codepoint(0x1F600) // 😀

// Character properties (implementation pending)
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