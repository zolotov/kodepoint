package me.zolotov.kodepoint.generator.code

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ClassName
import me.zolotov.kodepoint.generator.ScriptBuildResult
import me.zolotov.kodepoint.generator.ScriptPlaneResult
import me.zolotov.kodepoint.generator.UNICODE_SCRIPT_CLASS_NAME
import java.nio.file.Path

/** Lookup entry point that every generated `ScriptData*` plane object exposes. */
private const val GET_SCRIPT_ID = "getScriptId"

private fun scriptDataClassName(planeName: String) = ClassName(GENERATED_PACKAGE, "ScriptData${planeName}")

fun generateScriptDataClasses(
    outputDir: Path,
    scriptBuildResult: ScriptBuildResult,
    additionalComment: String
) {
    val scriptDataLatin1ClassName = scriptDataClassName(LATIN1_PLANE_NAME)
    FileSpec.builder(scriptDataLatin1ClassName)
        .addType(latin1ScriptData(scriptDataLatin1ClassName, scriptBuildResult.latin1ScriptIds, additionalComment))
        .build()
        .writeTo(outputDir)
    println("Generated ScriptDataLatin1 (256 entries)")

    for (planeResult in scriptBuildResult.planeResults) {
        val scriptDataClassName = scriptDataClassName(planeResult.plane.name)
        val typeSpec = when (planeResult) {
            is ScriptPlaneResult.Sparse -> sparseScriptData(scriptDataClassName, planeResult, additionalComment)
            is ScriptPlaneResult.Table -> planeScriptData(scriptDataClassName, planeResult, additionalComment)
        }
        FileSpec.builder(scriptDataClassName)
            .addType(typeSpec)
            .build()
            .writeTo(outputDir)
    }

    val scriptDataFacadeClass = ClassName(GENERATED_PACKAGE, "ScriptData")
    val scriptDataFacade = scriptDataFacade(scriptDataFacadeClass, scriptBuildResult.planeResults, additionalComment)
    FileSpec.builder(scriptDataFacadeClass)
        .addType(scriptDataFacade)
        .build()
        .writeTo(outputDir)

    println("Generated ScriptData facade")
}

private fun latin1ScriptData(className: ClassName, scriptIds: IntArray, additionalComment: String): TypeSpec {
    require(scriptIds.size == 256) { "Latin1 requires exactly 256 script IDs" }

    val scriptsProperty = PropertySpec
        .builder("SCRIPTS", String::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%S", encodeIntArrayAsString8(scriptIds))
        .build()
    return TypeSpec.objectBuilder(className)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Auto-generated Unicode script data for Latin-1 (0x00-0xFF).\n\n$additionalComment")
        .addProperty(scriptsProperty)
        .addFunction(
            FunSpec.builder(GET_SCRIPT_ID)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("codepoint", Int::class)
                .returns(Int::class)
                .addStatement("return %N[codepoint].code", scriptsProperty)
                .build()
        )
        .build()
}

private fun planeScriptData(
    scriptDataClassName: ClassName,
    planeResult: ScriptPlaneResult.Table,
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
    val scriptsProperty = PropertySpec
        .builder("SCRIPTS", String::class, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%S", encodeIntArrayAsString8(table.dataTable))
        .build()

    return TypeSpec.objectBuilder(scriptDataClassName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Auto-generated Unicode script data for ${planeResult.plane.name}.\n\n$additionalComment")
        .addProperty(blockShiftProperty)
        .addProperty(blockMaskProperty)
        .addProperty(blockSizeProperty)
        .addProperty(blockIndexProperty)
        .addProperty(scriptsProperty)
        .addFunction(
            FunSpec.builder(GET_SCRIPT_ID)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("val blockNum = %N[offset ushr %N].code", blockIndexProperty, blockShiftProperty)
                .addStatement(
                    "return %N[blockNum * %N + (offset and %N)].code",
                    scriptsProperty,
                    blockSizeProperty,
                    blockMaskProperty
                )
                .build()
        )
        .build()
}

private fun sparseScriptData(
    scriptDataClassName: ClassName,
    planeResult: ScriptPlaneResult.Sparse,
    additionalComment: String
): TypeSpec {
    val rangesProperty = PropertySpec
        .builder("RANGES", IntArray::class, KModifier.PRIVATE)
        .initializer("intArrayOf(%L)", rangeTriplets(planeResult.ranges, hexValue = false))
        .build()
    return TypeSpec.objectBuilder(scriptDataClassName)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Auto-generated Unicode script data for ${planeResult.plane.name}.\n\n$additionalComment")
        .addProperty(rangesProperty)
        .addFunction(
            FunSpec.builder(GET_SCRIPT_ID)
                .addModifiers(KModifier.INTERNAL)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("return %M(offset, %N, 0)", BINARY_SEARCH_RANGE, rangesProperty)
                .build()
        )
        .build()
}

private fun scriptDataFacade(
    className: ClassName,
    planeResults: List<ScriptPlaneResult>,
    additionalComment: String
): TypeSpec {
    require(planeResults.isNotEmpty()) { "At least one plane is required to generate the ScriptData facade" }

    val getScript = FunSpec.builder("getScript")
        .addModifiers(KModifier.INTERNAL)
        .addParameter("cp", Int::class)
        .returns(UNICODE_SCRIPT_CLASS_NAME)
        .beginControlFlow("return when")
        .addStatement("cp < 0 -> %T.UNKNOWN", UNICODE_SCRIPT_CLASS_NAME)
        // Latin-1 has its own object and covers everything below the first plane's start.
        .addStatement(
            "cp < %L -> %T.entries[%T.%N(cp)]",
            hex(planeResults.first().plane.startCodepoint),
            UNICODE_SCRIPT_CLASS_NAME,
            scriptDataClassName(LATIN1_PLANE_NAME),
            GET_SCRIPT_ID
        )

    // Sparse and table planes both expose getScriptId(offset), so every plane reads the same way
    // and the branches follow straight from the plane bounds.
    for ((name, startCodepoint, endCodepoint) in planeResults.map { it.plane }) {
        getScript.addStatement(
            "cp <= %L -> %T.entries[%T.%N(cp - %L)]",
            hex(endCodepoint),
            UNICODE_SCRIPT_CLASS_NAME,
            scriptDataClassName(name),
            GET_SCRIPT_ID,
            hex(startCodepoint)
        )
    }

    getScript
        .addStatement("else -> %T.UNKNOWN", UNICODE_SCRIPT_CLASS_NAME)
        .endControlFlow()

    return TypeSpec.objectBuilder(className)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc("Auto-generated Unicode script data facade.\n\n$additionalComment")
        .addFunction(getScript.build())
        .build()
}
