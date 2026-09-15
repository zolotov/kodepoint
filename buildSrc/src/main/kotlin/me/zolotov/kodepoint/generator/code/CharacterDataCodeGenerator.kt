package me.zolotov.kodepoint.generator.code

import com.squareup.kotlinpoet.*
import me.zolotov.kodepoint.generator.LastCaseDeltaRanges
import me.zolotov.kodepoint.generator.PlaneTableResult
import me.zolotov.kodepoint.generator.PropertyTableBuildResult
import me.zolotov.kodepoint.generator.UNICODE_VERSION
import java.nio.file.Path

internal const val GENERATED_PACKAGE = "me.zolotov.kodepoint.generated"
internal val BINARY_SEARCH_RANGE = MemberName("me.zolotov.kodepoint.internal", "binarySearchRange")

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

    val characterDataLatin1ClassName = characterDataClassName("Latin1")
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
                .builder("INDICES", String::class, KModifier.INTERNAL, KModifier.CONST)
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
        .builder("INDICES", String::class, KModifier.PRIVATE, KModifier.CONST)
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
            FunSpec.builder("getPropertyIndex")
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
            FunSpec.builder("getProperties")
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
    largeCaseDeltaRanges: LastCaseDeltaRanges,
    additionalComment: String
): TypeSpec {
    // Value lookup table for byte indices
    val uniquePropertyValuesProperty = PropertySpec
        .builder("UNIQUE_PROPERTY_VALUES", IntArray::class, KModifier.PRIVATE)
        .initializer("intArrayOf(%L)", uniqueCharacterProperties.joinToString(", ") { hex(it) })
        .build()

    return TypeSpec.objectBuilder(className)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            """
            Auto-generated Unicode character property data facade.
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        // Bit constants (internal for use by Codepoints)
        .addProperty(intInternalConstProperty("CASE_DELTA_MASK", "0x3FF"))
        .addProperty(intInternalConstProperty("DELTA_TO_LOWERCASE_BIT", "1 shl 10"))
        .addProperty(intInternalConstProperty("CATEGORY_MASK", "0x1F shl 11"))
        .addProperty(intInternalConstProperty("CATEGORY_SHIFT", "11"))
        .addProperty(intInternalConstProperty("IS_OTHER_UPPERCASE_BIT", "1 shl 16"))
        .addProperty(intInternalConstProperty("IS_OTHER_LOWERCASE_BIT", "1 shl 17"))
        .addProperty(intInternalConstProperty("IS_WHITESPACE_BIT", "1 shl 18"))
        .addProperty(intInternalConstProperty("IS_IDEOGRAPHIC_BIT", "1 shl 19"))
        .addProperty(intInternalConstProperty("IS_UNICODE_ID_START_BIT", "1 shl 20"))
        .addProperty(intInternalConstProperty("IS_UNICODE_ID_PART_BIT", "1 shl 21"))
        .addProperty(intInternalConstProperty("IS_JAVA_ID_START_BIT", "1 shl 22"))
        .addProperty(intInternalConstProperty("IS_JAVA_ID_PART_BIT", "1 shl 23"))
        .addProperty(intInternalConstProperty("HAS_LARGE_LOWERCASE_DELTA_BIT", "1 shl 24"))
        .addProperty(intInternalConstProperty("HAS_LARGE_UPPERCASE_DELTA_BIT", "1 shl 25"))
        .addProperty(intInternalConstProperty("IS_LETTER_BIT", "1 shl 26"))
        .addProperty(intInternalConstProperty("IS_DIGIT_BIT", "1 shl 27"))
        .addProperty(intInternalConstProperty("IS_UPPERCASE_BIT", "1 shl 28"))
        .addProperty(intInternalConstProperty("IS_LOWERCASE_BIT", "1 shl 29"))
        .addProperty(intInternalConstProperty("IS_SPACE_CHAR_BIT", "1 shl 30"))

        // Category constants
        .addProperty(intInternalConstProperty("CAT_LU", "1"))
        .addProperty(intInternalConstProperty("CAT_LL", "2"))
        .addProperty(intInternalConstProperty("CAT_LT", "3"))
        .addProperty(intInternalConstProperty("CAT_LM", "4"))
        .addProperty(intInternalConstProperty("CAT_LO", "5"))
        .addProperty(intInternalConstProperty("CAT_ND", "9"))
        .addProperty(intInternalConstProperty("CAT_ZS", "23"))
        .addProperty(intInternalConstProperty("CAT_ZL", "24"))
        .addProperty(intInternalConstProperty("CAT_ZP", "25"))
        .addProperty(intInternalConstProperty("CAT_CC", "26"))
        .addProperty(intInternalConstProperty("CAT_CF", "27"))

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
        .addFunction(
            FunSpec.builder("getProperties")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("cp", Int::class)
                .returns(Int::class)
                .beginControlFlow("return when")
                // BMP first (including a negativity check via ushr) so the common case
                // pays two branches instead of three
                .addStatement(
                    "cp ushr 16 == 0 -> if (cp < 0x100) %N[%T.INDICES[cp].code] " +
                            "else %N[%T.getPropertyIndex(cp - 0x100)]",
                    uniquePropertyValuesProperty,
                    characterDataClassName("Latin1"),
                    uniquePropertyValuesProperty,
                    characterDataClassName("BMP")
                )
                .addStatement("cp < 0 -> 0")
                // Sparse planes return full property values directly
                .addStatement("cp <= 0x1FFFF -> %T.getProperties(cp - 0x10000)", characterDataClassName("SMP"))
                .addStatement("cp <= 0x2FFFF -> %T.getProperties(cp - 0x20000)", characterDataClassName("SIP"))
                .addStatement("cp <= 0x10FFFF -> %T.getProperties(cp - 0x30000)", characterDataClassName("SSP"))
                .addStatement("else -> 0")
                .endControlFlow()
                .build()
        )
        .build()
}
