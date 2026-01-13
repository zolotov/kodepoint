# Architecture

This document provides a comprehensive overview of how the Kodepoint library stores Unicode codepoint data and implements Unicode functionality across JVM and non-JVM platforms.

## Table of Contents

- [Core Data Structure](#core-data-structure)
- [Platform Strategy](#platform-strategy)
- [Property Packing](#property-packing)
- [Lookup Table Architecture](#lookup-table-architecture)
- [Code Generation Pipeline](#code-generation-pipeline)
- [Unicode Script Handling](#unicode-script-handling)

## Core Data Structure

### The Codepoint Value Class

The library's fundamental data type is `Codepoint`, implemented as a JVM inline value class in `lib/src/commonMain/kotlin/me/zolotov/kodepoint/Codepoint.kt`:

```kotlin
@JvmInline
value class Codepoint(val codepoint: Int)
```

**Key characteristics:**
- Wraps a single 32-bit `Int` representing a Unicode codepoint (U+0000 to U+10FFFF)
- Zero heap allocation on JVM due to `@JvmInline` annotation
- `charCount` property returns 1 for BMP characters (< U+10000), 2 for supplementary characters requiring surrogate pairs

### UTF-16 Surrogate Pair Handling

For supplementary characters (U+10000 and above), the library handles UTF-16 surrogate pair encoding/decoding:

```kotlin
const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000
const val MIN_HIGH_SURROGATE = 0xD800
const val MIN_LOW_SURROGATE = 0xDC00
```

The `asString()` method transparently converts codepoints to proper String representations, handling surrogate pairs when necessary.

## Platform Strategy

The library uses Kotlin's `expect`/`actual` mechanism to provide platform-specific implementations:

```
commonMain/
├── Codepoint.kt              # expect declarations
├── MultiplatformCodepoint.kt # expect functions
│
jvmMain/
├── MultiplatformCodepoint.jvm.kt    # actual: delegates to java.lang.Character
│
nonJvmMain/
├── MultiplatformCodepoint.nonJvm.kt # actual: uses generated lookup tables
```

### JVM Implementation

On JVM (`lib/src/jvmMain/kotlin/me/zolotov/kodepoint/MultiplatformCodepoint.jvm.kt`), all Unicode operations delegate directly to `java.lang.Character`:

```kotlin
internal actual fun isLetter(codepoint: Int): Boolean = Character.isLetter(codepoint)
internal actual fun toLowerCase(codepoint: Int): Int = Character.toLowerCase(codepoint)
internal actual fun getUnicodeScript(codepoint: Int): UnicodeScript =
    jvmScriptToUnicodeScript(Character.UnicodeScript.of(codepoint))
```

**Advantages:**
- Zero memory overhead for character data tables
- Optimal performance from JVM built-in implementations
- Consistent behavior with Java ecosystem

### Non-JVM Implementation

On non-JVM platforms (`lib/src/nonJvmMain/kotlin/me/zolotov/kodepoint/MultiplatformCodepoint.nonJvm.kt`), operations use generated lookup tables from the `:unicode` module:

```kotlin
internal actual fun isLetter(codepoint: Int): Boolean = Codepoints.isLetter(codepoint)
internal actual fun toLowerCase(codepoint: Int): Int = Codepoints.toLowerCase(codepoint)
internal actual fun getUnicodeScript(codepoint: Int): UnicodeScript =
    Codepoints.getUnicodeScript(codepoint)
```

## Property Packing

All Unicode character properties are packed into a single 32-bit integer to minimize memory usage. The packing logic is in `buildSrc/src/main/kotlin/me/zolotov/kodepoint/generator/PropertyPacker.kt`:

### Bit Layout

```
Bits 0-9:    Case delta (10 bits, signed: -512 to +511)
Bit 10:      Delta direction (1 = to lowercase, 0 = to uppercase)
Bits 11-15:  General category (5 bits, supports 31 categories)
Bit 16:      Is Other_Uppercase
Bit 17:      Is Other_Lowercase
Bit 18:      Is White_Space (Unicode property)
Bit 19:      Is Ideographic
Bit 20:      Is Unicode ID_Start
Bit 21:      Is Unicode ID_Continue
Bit 22:      Is Java identifier start
Bit 23:      Is Java identifier part
Bit 24:      Has large lowercase delta (requires external table lookup)
Bit 25:      Has large uppercase delta (requires external table lookup)
```

### Case Delta Encoding

Most Unicode case mappings have small deltas that fit within 10 bits (-512 to +511). Characters with larger deltas (e.g., some Greek characters) are flagged and stored in separate range arrays:

- `largeLowercaseRanges`: Triplets of `[startCodepoint, endCodepoint, delta]`
- `largeUppercaseRanges`: Same structure for uppercase mappings

Lookup uses binary search via `binarySearchRange()` in `unicode/src/commonMain/kotlin/me/zolotov/kodepoint/internal/Utils.kt`.

### Property Extraction

Properties are extracted using bitwise operations:

```kotlin
fun getCategory(props: Int): Int = (props and CATEGORY_MASK) ushr CATEGORY_SHIFT
fun isWhitespace(props: Int): Boolean = (props and WHITESPACE_BIT) != 0
fun getCaseDelta(props: Int): Int = /* decode signed 10-bit value */
```

## Lookup Table Architecture

The library uses different lookup strategies optimized for each Unicode plane's characteristics:

### Unicode Planes

| Plane | Name | Range | Strategy |
|-------|------|-------|----------|
| 0 | BMP (Latin-1) | U+0000-U+00FF | Direct byte array |
| 0 | BMP (rest) | U+0100-U+FFFF | Two-level block table |
| 1 | SMP | U+10000-U+1FFFF | Binary search ranges |
| 2 | SIP | U+20000-U+2FFFF | Binary search ranges |
| 3-16 | SSP | U+30000-U+10FFFF | Binary search ranges |

Configuration is in `buildSrc/src/main/kotlin/me/zolotov/kodepoint/generator/UnicodeConstants.kt`.

### Latin-1 Lookup (U+0000-U+00FF)

The first 256 codepoints use direct array indexing:

```kotlin
// CharacterDataLatin1.kt (generated)
private val indices: ByteArray = /* 256 bytes */

fun getProperties(codepoint: Int): Int =
    CharacterData.UNIQUE_PROPERTY_VALUES[indices[codepoint].toInt() and 0xFF]
```

For ASCII case conversion, table lookup is skipped entirely:

```kotlin
fun asciiToLowerCase(codepoint: Int): Int =
    if ((codepoint - 'A'.code).toUInt() <= 25u) codepoint + 32 else codepoint
```

### BMP Block Table (U+0100-U+FFFF)

The Basic Multilingual Plane has 65,280 codepoints. Storing 4-byte properties for each would need 256 KB. Two techniques reduce this to ~34 KB.

#### Global Value Table

Most Unicode characters share the same property combinations. Instead of storing 4 bytes per codepoint, we store a 1-byte index into a shared table of unique values.

```
Without global table:
  codepoint 'A' → 0x00420001 (4 bytes)
  codepoint 'B' → 0x00420001 (4 bytes)  // same properties
  codepoint 'C' → 0x00420001 (4 bytes)  // same again
  ... 65,536 × 4 bytes = 256 KB

With global table:
  UNIQUE_PROPERTY_VALUES[42] = 0x00420001  // stored once

  codepoint 'A' → index 42 (1 byte)
  codepoint 'B' → index 42 (1 byte)
  codepoint 'C' → index 42 (1 byte)
  ... 65,536 × 1 byte = 64 KB
```

There are ~170 unique property combinations across all Unicode codepoints, so a single byte is enough.

#### Block Deduplication

64 KB of indices is still large. Many codepoint ranges have identical property patterns, so we group codepoints into blocks and reuse identical blocks.

```
BMP divided into 256 blocks of 256 codepoints each:

Block 0 (U+0100-U+01FF): [idx, idx, idx, ...] ← Latin Extended-A
Block 1 (U+0200-U+02FF): [idx, idx, idx, ...] ← Latin Extended-B
...
Block 100: [0, 0, 0, 0, ...]  ← Unassigned (all zeros)
Block 101: [0, 0, 0, 0, ...]  ← Unassigned (identical!)
Block 102: [0, 0, 0, 0, ...]  ← Unassigned (identical!)
...

Without deduplication: 256 blocks × 256 bytes = 64 KB
With deduplication: ~130 unique blocks × 256 bytes + 256-byte index = ~34 KB
```

Many blocks are identical because:
- Large unassigned ranges share the same "unassigned" block
- Private use areas share identical blocks
- CJK ideograph ranges have uniform properties

The generator tries different block sizes and picks the smallest total memory.

### Sparse Plane Lookup (SMP, SIP, SSP)

Supplementary planes are sparsely populated. These use binary search over contiguous ranges:

```kotlin
// Stored as triplets: [startCodepoint, endCodepoint, propertyValue]
private val ranges: IntArray = intArrayOf(
    0x10000, 0x1000B, /* property value */,
    0x1000D, 0x10026, /* property value */,
    // ...
)
```

Lookup uses binary search with O(log n) complexity, where n is the number of ranges with assigned properties.

## Code Generation Pipeline

Unicode data tables are generated at build time from the Unicode Character Database.

### Generator Entry Point

`buildSrc/src/main/kotlin/me/zolotov/kodepoint/generator/UnicodeDataGenerator.kt` orchestrates:

1. **Download Unicode Data** from `https://www.unicode.org/Public/16.0.0/ucd/`
   - `UnicodeData.txt` - Core character properties
   - `PropList.txt` - Additional properties
   - `DerivedCoreProperties.txt` - Derived properties
   - `Scripts.txt` - Script assignments
   - `CaseFolding.txt`, `SpecialCasing.txt` - Case mapping data

2. **Parse Data** via `UnicodeDataParser`
   - Builds `CharacterData` objects for all 1,114,112 codepoints

3. **Pack Properties** via `PropertyPacker`
   - Converts parsed data to 32-bit packed integers

4. **Build Lookup Tables** via `PropertiesTableBuilder` and `LookupTableBuilder`
   - Optimizes memory layout per plane
   - Deduplicates identical blocks

5. **Generate Kotlin Code** via code generators in `buildSrc/src/main/kotlin/me/zolotov/kodepoint/generator/code/`

### Generated Files

Output to `unicode/build/generated/sources/unicode-data/`:

- `CharacterDataLatin1.kt` - Direct lookup for U+0000-U+00FF
- `CharacterDataBMP.kt` - Block tables for U+0100-U+FFFF
- `CharacterDataSMP.kt` - Binary search for Plane 1
- `CharacterDataSIP.kt` - Binary search for Plane 2
- `CharacterDataSSP.kt` - Binary search for Planes 3-16
- `CharacterData.kt` - Facade with routing logic and shared value table

## Unicode Script Handling

### Script Enum Generation

`buildSrc/src/main/kotlin/me/zolotov/kodepoint/generator/UnicodeScriptGenerator.kt` parses `Scripts.txt` and generates the `UnicodeScript` enum in `common/build/generated/sources/unicode-script/`:

```kotlin
enum class UnicodeScript {
    COMMON,
    LATIN,
    GREEK,
    // ... 160+ scripts
}
```

### Script Lookup Tables

`ScriptTableBuilder` creates separate lookup structures mirroring the character property tables:

- `ScriptDataLatin1` - Direct byte array
- `ScriptDataBMP` - Block tables
- `ScriptDataSMP/SIP/SSP` - Binary search ranges

Each returns a byte script ID that maps to `UnicodeScript` enum ordinals.

## Module Dependencies

```
common (UnicodeScript enum)
   ↑
unicode (generated lookup tables)
   ↑
lib (public API, platform dispatch)
```

- **common**: Shared `UnicodeScript` enum, no platform-specific code
- **unicode**: Generated data tables, used only by non-JVM platforms
- **lib**: Public `Codepoint` API, routes to JVM `Character` or `unicode` tables

## Key Source Files

| File | Purpose |
|------|---------|
| `lib/src/commonMain/.../Codepoint.kt` | Core Codepoint value class and API |
| `lib/src/jvmMain/.../MultiplatformCodepoint.jvm.kt` | JVM delegation to Character |
| `lib/src/nonJvmMain/.../MultiplatformCodepoint.nonJvm.kt` | Non-JVM table lookup |
| `unicode/src/commonMain/.../Codepoints.kt` | Property extraction and lookup dispatch |
| `unicode/src/commonMain/.../internal/Utils.kt` | Binary search, ASCII fast paths |
| `buildSrc/.../generator/PropertyPacker.kt` | 32-bit property encoding |
| `buildSrc/.../generator/PropertiesTableBuilder.kt` | Table construction |
| `buildSrc/.../generator/LookupTableBuilder.kt` | Two-level index optimization |
| `buildSrc/.../generator/UnicodeDataParser.kt` | Unicode file parsing |
