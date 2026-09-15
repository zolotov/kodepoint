package me.zolotov.kodepoint.generator.code

import com.squareup.kotlinpoet.*
import me.zolotov.kodepoint.generator.*
import java.nio.file.Path

internal val BINARY_SEARCH_RANGE = MemberName("me.zolotov.kodepoint.internal", "binarySearchRange")

/** Packed byte indices into `CharacterData.UNIQUE_PROPERTY_VALUES`, exposed by table-encoded planes. */
private const val INDICES = "INDICES"

/** Lookup entry point of a table-encoded plane: returns an index into `UNIQUE_PROPERTY_VALUES`. */
private const val GET_PROPERTY_INDEX = "getPropertyIndex"

/** Lookup entry point of a sparse-encoded plane, and of the facade: returns packed properties. */
private const val GET_PROPERTIES = "getProperties"

private fun characterDataClassName(planeName: String) = ClassName(GENERATED_PACKAGE, "CharacterData${planeName}")

fun generateCharacterDataClasses(
    outputDir: Path,
    propertyBuildResult: PropertyTableBuildResult,
    additionalComment: String,
    largeCaseDeltaRanges: LastCaseDeltaRanges
) {
    val uniqueCharacterProperties = propertyBuildResult.uniqueCharacterProperties

    // Build value-to-index mapping for Latin1
    val propertyToIndex = uniqueCharacterProperties.withIndex().associate { it.value to it.index }
    val latin1Indices = propertyBuildResult.latin1Properties.map { propertyToIndex[it]!! }.toIntArray()

    val characterDataLatin1ClassName = characterDataClassName(LATIN1_PLANE_NAME)
    FileSpec.builder(characterDataLatin1ClassName)
        .addType(latin1CharacterData(characterDataLatin1ClassName, latin1Indices, additionalComment))
        .build()
        .writeTo(outputDir)
    println("Generated CharacterDataLatin1 (256 byte indices)")

    for (planeResult in propertyBuildResult.planeResults) {
        val characterDataClassName = characterDataClassName(planeResult.plane.name)
        val typeSpec = when (planeResult) {
            is PlaneTableResult.Sparse -> sparseCharacterData(characterDataClassName, planeResult, additionalComment)
            is PlaneTableResult.Table -> planeCharacterData(characterDataClassName, planeResult, additionalComment)
        }
        FileSpec.builder(characterDataClassName)
            .addType(typeSpec)
            .build()
            .writeTo(outputDir)
    }

    val characterDataFacadeClass = ClassName(GENERATED_PACKAGE, "CharacterData")
    FileSpec.builder(characterDataFacadeClass)
        .addType(
            characterDataFacade(
                characterDataFacadeClass,
                uniqueCharacterProperties,
                propertyBuildResult.planeResults,
                largeCaseDeltaRanges,
                additionalComment
            )
        )
        .build()
        .writeTo(outputDir)
    println("Generated CharacterData facade (with ${uniqueCharacterProperties.size} value lookup entries)")
}

private fun latin1CharacterData(
    characterDataLatin1ClassName: ClassName,
    indices: IntArray,
    additionalComment: String
): TypeSpec {
    require(indices.size == 256) { "Latin1 requires exactly 256 index values" }

    return TypeSpec.objectBuilder(characterDataLatin1ClassName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            """
            Auto-generated Unicode character property data for Latin-1 (0x00-0xFF).
            Unicode version: $UNICODE_VERSION
            Uses byte indices into CharacterData.UNIQUE_PROPERTY_VALUES for memory efficiency.
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(
            PropertySpec
                .builder(INDICES, String::class, KModifier.INTERNAL, KModifier.CONST)
                .initializer("%S", encodeIntArrayAsString8(indices))
                .build()
        )
        .build()
}

private fun planeCharacterData(
    characterDataClassName: ClassName,
    planeResult: PlaneTableResult.Table,
    additionalComment: String
): TypeSpec {
    val table = planeResult.table
    val blockSize = 1 shl table.blockBits

    val blockShiftProperty = PropertySpec
        .builder("BLOCK_SHIFT", Int::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%L", table.blockBits)
        .build()
    val blockMaskProperty = PropertySpec
        .builder("BLOCK_MASK", Int::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%L", blockSize - 1)
        .build()
    val blockSizeProperty = PropertySpec
        .builder("BLOCK_SIZE", Int::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%L", blockSize)
        .build()
    val blockIndexProperty = PropertySpec
        .builder("BLOCK_INDEX", String::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%S", encodeIntArrayAsString16(table.indexTable))
        .build()
    val indicesProperty = PropertySpec
        .builder(INDICES, String::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%S", encodeIntArrayAsString8(table.dataTable))
        .build()

    return TypeSpec.objectBuilder(characterDataClassName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            """
            Auto-generated Unicode character property data for ${planeResult.plane.name}.
            Unicode version: $UNICODE_VERSION
            Uses byte indices into CharacterData.UNIQUE_PROPERTY_VALUES for memory efficiency.
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(blockShiftProperty)
        .addProperty(blockMaskProperty)
        .addProperty(blockSizeProperty)
        .addProperty(blockIndexProperty)
        .addProperty(indicesProperty)
        .addFunction(
            FunSpec.builder(GET_PROPERTY_INDEX)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("val blockNum = %N[offset ushr %N].code", blockIndexProperty, blockShiftProperty)
                .addStatement("val propIdx = blockNum * %N + (offset and %N)", blockSizeProperty, blockMaskProperty)
                .addStatement("return %N[propIdx].code", indicesProperty)
                .build()
        )
        .build()
}

private fun sparseCharacterData(
    characterDataClassName: ClassName,
    planeResult: PlaneTableResult.Sparse,
    additionalComment: String
): TypeSpec {
    val rangesProperty = PropertySpec
        .builder("RANGES", IntArray::class, KModifier.PRIVATE)
        .initializer("intArrayOf(%L)", rangeTriplets(planeResult.ranges, hexValue = true))
        .build()
    return TypeSpec.objectBuilder(characterDataClassName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("""
            Auto-generated Unicode character property data for ${planeResult.plane.name}.
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(rangesProperty)
        .addFunction(
            FunSpec.builder(GET_PROPERTIES)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("return %M(offset, %N, 0)", BINARY_SEARCH_RANGE, rangesProperty)
                .build()
        )
        .build()
}

private fun characterDataFacade(
    className: ClassName,
    uniqueCharacterProperties: IntArray,
    planeResults: List<PlaneTableResult>,
    largeCaseDeltaRanges: LastCaseDeltaRanges,
    additionalComment: String
): TypeSpec {
    // Value lookup table for byte indices
    val uniquePropertyValuesProperty = PropertySpec
        .builder("UNIQUE_PROPERTY_VALUES", IntArray::class, KModifier.PRIVATE)
        .initializer("intArrayOf(%L)", uniqueCharacterProperties.joinToString(", ") { hex(it) })
        .build()

    val getProperties = getPropertiesFacadeFun(planeResults, uniquePropertyValuesProperty)

    return TypeSpec.objectBuilder(className)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            """
            Auto-generated Unicode character property data facade.
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        // Bit constants (internal for use by Codepoints), emitted from the same values
        // PropertyPacker packs with, so the accessors can never decode a stale layout.
        .addProperty(intInternalConstProperty("CASE_DELTA_MASK", hex(PropertyPacker.CASE_DELTA_MASK)))
        .addProperty(singleBitConstProperty("DELTA_TO_LOWERCASE_BIT", PropertyPacker.DELTA_TO_LOWERCASE_BIT))
        .addProperty(
            intInternalConstProperty(
                "CATEGORY_MASK",
                "${hex(PropertyPacker.CATEGORY_BITS)} shl ${PropertyPacker.CATEGORY_SHIFT}"
            )
        )
        .addProperty(intInternalConstProperty("CATEGORY_SHIFT", PropertyPacker.CATEGORY_SHIFT.toString()))
        .addProperty(singleBitConstProperty("IS_OTHER_UPPERCASE_BIT", PropertyPacker.IS_OTHER_UPPERCASE_BIT))
        .addProperty(singleBitConstProperty("IS_OTHER_LOWERCASE_BIT", PropertyPacker.IS_OTHER_LOWERCASE_BIT))
        .addProperty(singleBitConstProperty("IS_WHITESPACE_BIT", PropertyPacker.IS_WHITESPACE_BIT))
        .addProperty(singleBitConstProperty("IS_IDEOGRAPHIC_BIT", PropertyPacker.IS_IDEOGRAPHIC_BIT))
        .addProperty(singleBitConstProperty("IS_UNICODE_ID_START_BIT", PropertyPacker.IS_UNICODE_ID_START_BIT))
        .addProperty(singleBitConstProperty("IS_UNICODE_ID_PART_BIT", PropertyPacker.IS_UNICODE_ID_PART_BIT))
        .addProperty(singleBitConstProperty("IS_JAVA_ID_START_BIT", PropertyPacker.IS_JAVA_ID_START_BIT))
        .addProperty(singleBitConstProperty("IS_JAVA_ID_PART_BIT", PropertyPacker.IS_JAVA_ID_PART_BIT))
        .addProperty(
            singleBitConstProperty("HAS_LARGE_LOWERCASE_DELTA_BIT", PropertyPacker.HAS_LARGE_LOWERCASE_DELTA_BIT)
        )
        .addProperty(
            singleBitConstProperty("HAS_LARGE_UPPERCASE_DELTA_BIT", PropertyPacker.HAS_LARGE_UPPERCASE_DELTA_BIT)
        )
        .addProperty(singleBitConstProperty("IS_LETTER_BIT", PropertyPacker.IS_LETTER_BIT))
        .addProperty(singleBitConstProperty("IS_DIGIT_BIT", PropertyPacker.IS_DIGIT_BIT))
        .addProperty(singleBitConstProperty("IS_UPPERCASE_BIT", PropertyPacker.IS_UPPERCASE_BIT))
        .addProperty(singleBitConstProperty("IS_LOWERCASE_BIT", PropertyPacker.IS_LOWERCASE_BIT))
        .addProperty(singleBitConstProperty("IS_SPACE_CHAR_BIT", PropertyPacker.IS_SPACE_CHAR_BIT))
        .addProperty(uniquePropertyValuesProperty)
        // Large case delta ranges
        .apply {
            if (largeCaseDeltaRanges.toLower.isNotEmpty()) {
                addProperty(
                    PropertySpec
                        .builder("largeLowercaseRanges", IntArray::class, KModifier.INTERNAL)
                        .initializer("intArrayOf(%L)", rangeTriplets(largeCaseDeltaRanges.toLower, hexValue = false))
                        .build()
                )
            }
            if (largeCaseDeltaRanges.toUpper.isNotEmpty()) {
                addProperty(
                    PropertySpec
                        .builder("largeUppercaseRanges", IntArray::class, KModifier.INTERNAL)
                        .initializer("intArrayOf(%L)", rangeTriplets(largeCaseDeltaRanges.toUpper, hexValue = false))
                        .build()
                )
            }
        }
        .addFunction(getProperties)
        .build()
}

/**
 * Builds the facade lookup, deriving one branch per plane from [planeResults].
 *
 * Latin-1 and the first plane share the leading branch, which is hand-shaped: testing
 * `cp ushr 16 == 0` covers the whole BMP *and* rules out negatives, so the common case pays two
 * branches instead of three and `cp < 0` can be deferred. The remaining planes follow mechanically,
 * with the encoding deciding how each one is read.
 */
private fun getPropertiesFacadeFun(
    planeResults: List<PlaneTableResult>,
    uniquePropertyValuesProperty: PropertySpec
): FunSpec {
    val bmp = planeResults.firstOrNull()
    require(bmp is PlaneTableResult.Table) {
        "The first plane must be table-encoded for the CharacterData facade's BMP fast path, was $bmp"
    }
    require(bmp.plane.endCodepoint == 0xFFFF) {
        "The first plane must end at 0xFFFF for the `cp ushr 16 == 0` fast path, " +
                "was ${hex(bmp.plane.endCodepoint)}"
    }

    val getProperties = FunSpec.builder(GET_PROPERTIES)
        .addModifiers(KModifier.INTERNAL)
        .addParameter("cp", Int::class)
        .returns(Int::class)
        .beginControlFlow("return when")
        // BMP first (including a negativity check via ushr) so the common case
        // pays two branches instead of three
        .addStatement(
            "cp ushr 16 == 0 -> if (cp < %L) %N[%T.%N[cp].code] else %N[%T.%N(cp - %L)]",
            hex(bmp.plane.startCodepoint),
            uniquePropertyValuesProperty,
            characterDataClassName(LATIN1_PLANE_NAME),
            INDICES,
            uniquePropertyValuesProperty,
            characterDataClassName(bmp.plane.name),
            GET_PROPERTY_INDEX,
            hex(bmp.plane.startCodepoint)
        )
        .addStatement("cp < 0 -> 0")

    for (planeResult in planeResults.drop(1)) {
        val plane = planeResult.plane
        when (planeResult) {
            // Sparse planes return full property values directly
            is PlaneTableResult.Sparse -> getProperties.addStatement(
                "cp <= %L -> %T.%N(cp - %L)",
                hex(plane.endCodepoint),
                characterDataClassName(plane.name),
                GET_PROPERTIES,
                hex(plane.startCodepoint)
            )
            // Table planes return a byte index into the unique value table
            is PlaneTableResult.Table -> getProperties.addStatement(
                "cp <= %L -> %N[%T.%N(cp - %L)]",
                hex(plane.endCodepoint),
                uniquePropertyValuesProperty,
                characterDataClassName(plane.name),
                GET_PROPERTY_INDEX,
                hex(plane.startCodepoint)
            )
        }
    }

    return getProperties
        .addStatement("else -> 0")
        .endControlFlow()
        .build()
}
