package me.zolotov.kodepoint.generator.code

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ClassName
import me.zolotov.kodepoint.generator.ScriptBuildResult
import me.zolotov.kodepoint.generator.ScriptPlaneResult
import me.zolotov.kodepoint.generator.UNICODE_SCRIPT_CLASS_NAME
import me.zolotov.kodepoint.generator.UNICODE_VERSION
import java.nio.file.Path

private fun scriptDataClassName(planeName: String) = ClassName(GENERATED_PACKAGE, "ScriptData${planeName}")

fun generateScriptDataClasses(
    outputDir: Path,
    scriptBuildResult: ScriptBuildResult,
    additionalComment: String
) {
    val scriptDataLatin1ClassName = scriptDataClassName("Latin1")
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
    val scriptDataFacade = scriptDataFacade(scriptDataFacadeClass, additionalComment)
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
        .addKdoc(
            """
            Auto-generated Unicode script data for Latin-1 (0x00-0xFF).
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(scriptsProperty)
        .addFunction(
            FunSpec.builder("getScriptId")
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
        .addKdoc(
            """
            Auto-generated Unicode script data for ${planeResult.plane.name}.
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(blockShiftProperty)
        .addProperty(blockMaskProperty)
        .addProperty(blockSizeProperty)
        .addProperty(blockIndexProperty)
        .addProperty(scriptsProperty)
        .addFunction(
            FunSpec.builder("getScriptId")
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
        .addKdoc(
            """
            Auto-generated Unicode script data for ${planeResult.plane.name}.
            Unicode version: $UNICODE_VERSION
            
            $additionalComment
        """.trimIndent()
        )
        .addProperty(rangesProperty)
        .addFunction(
            FunSpec.builder("getScriptId")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("offset", Int::class)
                .returns(Int::class)
                .addStatement("return %M(offset, %N, 0)", BINARY_SEARCH_RANGE, rangesProperty)
                .build()
        )
        .build()
}

private fun scriptDataFacade(className: ClassName, additionalComment: String): TypeSpec {
    return TypeSpec.objectBuilder(className)
        .addModifiers(KModifier.INTERNAL)
        .addKdoc(
            """
            Auto-generated Unicode script data facade.
            Unicode version: $UNICODE_VERSION
    
            $additionalComment
        """.trimIndent()
        )
        .addFunction(
            FunSpec.builder("getScript")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("cp", Int::class)
                .returns(UNICODE_SCRIPT_CLASS_NAME)
                .beginControlFlow("return when")
                .addStatement("cp < 0 -> %T.UNKNOWN", UNICODE_SCRIPT_CLASS_NAME)
                .addStatement(
                    "cp < 0x100 -> %T.entries[%T.getScriptId(cp)]",
                    UNICODE_SCRIPT_CLASS_NAME,
                    scriptDataClassName("Latin1")
                )
                .addStatement(
                    "cp <= 0xFFFF -> %T.entries[%T.getScriptId(cp - 0x100)]",
                    UNICODE_SCRIPT_CLASS_NAME,
                    scriptDataClassName("BMP")
                )
                .addStatement(
                    "cp <= 0x1FFFF -> %T.entries[%T.getScriptId(cp - 0x10000)]",
                    UNICODE_SCRIPT_CLASS_NAME,
                    scriptDataClassName("SMP")
                )
                .addStatement(
                    "cp <= 0x2FFFF -> %T.entries[%T.getScriptId(cp - 0x20000)]",
                    UNICODE_SCRIPT_CLASS_NAME,
                    scriptDataClassName("SIP")
                )
                .addStatement(
                    "cp <= 0x10FFFF -> %T.entries[%T.getScriptId(cp - 0x30000)]",
                    UNICODE_SCRIPT_CLASS_NAME,
                    scriptDataClassName("SSP")
                )
                .addStatement("else -> %T.UNKNOWN", UNICODE_SCRIPT_CLASS_NAME)
                .endControlFlow()
                .build()
        )
        .build()
}
