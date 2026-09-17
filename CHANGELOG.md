# Change Log

## [Unreleased]

## [3.0.0] - 2026-09-16

### Breaking

- `UnicodeScript` gains seven constants for the scripts new in Unicode 17.0.0 and 18.0.0:
  `BERIA_ERFE`, `SIDETIC`, `TAI_YO`, `TOLONG_SIKI`, `JURCHEN`, `PROTO_CUNEIFORM` and `SEAL`. An
  exhaustive `when` over the enum needs new branches or an `else`.
  ([#116](https://github.com/zolotov/kodepoint/pull/116), TODO: PR for the 18.0.0 update)

### Changed

- The lookup tables used on non-JVM targets follow Unicode 18.0.0 instead of 16.0.0. That assigns
  17,810 new characters: mostly Seal (11,328), CJK Unified Ideographs Extension J (4,321) and
  Jurchen (965), the new scripts Beria Erfe, Sidetic, Tai Yo, Tolong Siki and Proto-Cuneiform, and
  additions to Latin, Cuneiform, Tangut, Arabic, Sharada, Katakana, Khitan Small Script and
  Armenian. U+0295 LATIN LETTER PHARYNGEAL VOICED FRICATIVE ʕ is now `OTHER_LETTER` instead of
  `LOWERCASE_LETTER`, `toUpperCase()`/`toLowerCase()` map 48 new Latin and Beria Erfe case pairs,
  and `isDigit()` accepts the ten Tolong Siki digits U+11DE0–U+11DE9. On the JVM every query still
  delegates to `java.lang.Character`, so results there follow the JDK's Unicode version (16.0.0 on
  JDK 24 and 25) and differ from the other targets by exactly this delta until a JDK ships
  Unicode 18. ([#116](https://github.com/zolotov/kodepoint/pull/116), TODO: PR for the 18.0.0
  update)

## [2.0.0] - 2026-07-20

### Added

- `Codepoint.getCategory()` returns the Unicode general category as the new `Category` enum
  (`UPPERCASE_LETTER`, `DECIMAL_DIGIT_NUMBER`, …). Unassigned code points and values outside the
  Unicode range return `Category.UNASSIGNED`. On the JVM it matches `Character.getType()`.
  ([#64](https://github.com/zolotov/kodepoint/pull/64))
- `Appendable.appendCodePoint(Int)` overload, so a raw code point can be appended without wrapping
  it in a `Codepoint` first.
- `wasmWasi` target, for WASI runtimes such as Cloudflare Workers, Fastly Compute and Fermyon Spin.
  It uses the same generated lookup tables as the other non-JVM targets.
  ([#12](https://github.com/zolotov/kodepoint/pull/12))

### Performance

- `CharSequence.codepoints()` iterators are plain index-advancing iterators instead of
  coroutine-based `iterator { }` builders, removing the per-element suspension and allocation.
  ([#47](https://github.com/zolotov/kodepoint/pull/47))
- Every classification and case-conversion function short-circuits for ASCII input with a single
  flag-table lookup, without consulting the Unicode property tables.
  ([#49](https://github.com/zolotov/kodepoint/pull/49),
  [#66](https://github.com/zolotov/kodepoint/pull/66))
- On non-JVM targets `isLetter()`, `isDigit()`, `isUpperCase()`, `isLowerCase()` and `isSpaceChar()`
  test one precomputed bit of the property word instead of decoding the general category, and BMP
  lookups take one branch fewer. ([#65](https://github.com/zolotov/kodepoint/pull/65))
- `Codepoint.asString()` builds the string directly from the one or two UTF-16 units instead of
  going through a vararg conversion. ([#37](https://github.com/zolotov/kodepoint/pull/37))

## [1.0.1] - 2026-01-14

### Fixed

- `CharSequence.codePointBefore(index)` threw `IndexOutOfBoundsException` for `index == length`,
  so the last code point of a string could not be read. The exception message now names the
  offending index.

## [1.0.0] - 2026-01-13

Initial release.

### Added

- `Codepoint`, a `@JvmInline value class` over an `Int` code point, with `java.lang.Character`-style
  queries in Kotlin common code: `isLetter()`, `isDigit()`, `isLetterOrDigit()`, `isUpperCase()`,
  `isLowerCase()`, `isSpaceChar()`, `isWhitespace()`, `isIdeographic()`, `isISOControl()`,
  `isIdentifierIgnorable()`, `isUnicodeIdentifierStart()`, `isUnicodeIdentifierPart()`,
  `isJavaIdentifierStart()`, `isJavaIdentifierPart()`, `toUpperCase()`, `toLowerCase()`,
  `getUnicodeScript()`, `asString()`, `charCount`, and `Codepoint.fromChars(high, low)`.
- `UnicodeScript` enum covering all scripts of Unicode 16.0.0.
- Surrogate-safe `CharSequence` extensions: `forEachCodepoint { }`, `forEachCodepointReversed { }`,
  `codePointAt(index)`, `codePointBefore(index)` and `codepoints(offset, direction)`.
- `Appendable.appendCodePoint(Codepoint)`, writing a surrogate pair for supplementary code points.
- Targets: JVM (bytecode 11), JS, WasmJS, iOS, macOS, tvOS, watchOS, Linux x64/arm64 and Windows
  x64, from a single `commonMain` API. Kotlin API and language version 2.1; Kotlin 2.2.20 so
  WasmJS consumers are not forced onto 2.3.0. No runtime dependencies.
- On the JVM every query delegates to `java.lang.Character`; elsewhere it uses compact generated
  Unicode 16.0.0 lookup tables, validated against `java.lang.Character` for all 1,114,112 code
  points. Two deliberate deviations follow the Unicode standard instead of the JVM:
  `isWhitespace()` uses the `White_Space` property (no-break spaces are whitespace, U+001C–U+001F
  are not), and `isUnicodeIdentifierStart()`/`Part()` exclude U+2E2F VERTICAL TILDE.

[Unreleased]: https://github.com/zolotov/kodepoint/compare/3.0.0...HEAD
[3.0.0]: https://github.com/zolotov/kodepoint/compare/2.0.0...3.0.0
[2.0.0]: https://github.com/zolotov/kodepoint/compare/1.0.1...2.0.0
[1.0.1]: https://github.com/zolotov/kodepoint/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/zolotov/kodepoint/commits/1.0.0
