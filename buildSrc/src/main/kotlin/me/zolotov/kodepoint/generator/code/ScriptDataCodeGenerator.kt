package me.zolotov.kodepoint.generator.code

import me.zolotov.kodepoint.generator.ScriptBuildResult
import me.zolotov.kodepoint.generator.ScriptPlaneResult
import me.zolotov.kodepoint.generator.UNICODE_VERSION
import me.zolotov.kodepoint.generator.dsl.kotlinFile
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

fun generateScriptDataClasses(
    generatedDir: Path,
    scriptBuildResult: ScriptBuildResult,
    additionalComment: String
) {
    // Generate Latin1 script data class
    generatedDir.resolve("ScriptDataLatin1.kt").bufferedWriter().use { writer ->
        generateLatin1ScriptData(
            writer,
            scriptBuildResult.latin1ScriptIds,
            UNICODE_VERSION,
            additionalComment
        )
        println("Generated ScriptDataLatin1 (256 entries)")
    }

    // Generate plane-specific script data classes
    for (scriptPlaneResult in scriptBuildResult.planeResults) {
        generatedDir.resolve("ScriptData${scriptPlaneResult.plane.name}.kt")
            .createParentDirectories()
            .bufferedWriter().use { writer ->
                when (scriptPlaneResult) {
                    is ScriptPlaneResult.Sparse -> generateSparseScriptData(
                        writer,
                        scriptPlaneResult,
                        UNICODE_VERSION,
                        additionalComment
                    )

                    is ScriptPlaneResult.Table -> generatePlaneScriptData(
                        writer,
                        scriptPlaneResult,
                        UNICODE_VERSION,
                        additionalComment
                    )
                }
            }
    }

    generatedDir.resolve("ScriptData.kt").bufferedWriter().use { writer ->
        // Generate ScriptData facade
        generateScriptDataFacade(
            writer,
            UNICODE_VERSION,
            additionalComment
        )
        println("Generated ScriptData facade")
    }
}

private fun generateLatin1ScriptData(
    writer: Writer,
    scriptIds: IntArray,
    unicodeVersion: String,
    additionalComment: String
) {
    require(scriptIds.size == 256) { "Latin1 requires exactly 256 script IDs" }

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode script data for Latin-1 (0x00-0xFF).")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("ScriptDataLatin1") {
            byteArrayProperty(
                "SCRIPTS",
                scriptIds.map { it.toString() }
            )
            emptyLine()

            expressionFunction(
                "getScriptId",
                listOf("codepoint" to "Int"),
                "Int",
                expression = "SCRIPTS[codepoint].toInt() and 0xFF"
            )
        }
    }.writeTo(writer)
}

private fun generatePlaneScriptData(
    writer: Writer,
    scriptPlaneResult: ScriptPlaneResult.Table,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = scriptPlaneResult.plane
    val result = scriptPlaneResult.table
    val className = "ScriptData${plane.name}"
    val blockBits = result.blockBits
    val blockSize = 1 shl blockBits
    val blockMask = blockSize - 1
    val indexTable = result.indexTable
    val scriptTable = result.dataTable

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode script data for ${plane.name}.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration(className) {
            const("BLOCK_SHIFT", blockBits, private = true)
            const("BLOCK_MASK", blockMask, private = true)
            const("BLOCK_SIZE", blockSize, private = true)
            emptyLine()

            encodedString16Property("BLOCK_INDEX", indexTable)
            emptyLine()

            byteArrayFromStringProperty("SCRIPTS", scriptTable)
            emptyLine()

            function("getScriptId", listOf("offset" to "Int"), "Int") {
                variable("blockNum", "BLOCK_INDEX_DATA[offset ushr BLOCK_SHIFT].code")
                returnStatement("SCRIPTS_DATA[blockNum * BLOCK_SIZE + (offset and BLOCK_MASK)].toInt() and 0xFF")
            }
        }
    }.writeTo(writer)
}

private fun generateSparseScriptData(
    writer: Writer,
    scriptPlaneResult: ScriptPlaneResult.Sparse,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = scriptPlaneResult.plane
    val ranges = scriptPlaneResult.ranges
    val className = "ScriptData${plane.name}"

    kotlinFile("me.zolotov.kodepoint.generated") {
        import("me.zolotov.kodepoint.internal.binarySearchRange")

        kdoc {
            line("Auto-generated Unicode script data for ${plane.name}.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration(className) {
            intArrayProperty(
                "RANGES",
                ranges.flatMap {
                    listOf(
                        "0x${it.startCodepoint.toString(16).uppercase()}",
                        "0x${it.endCodepoint.toString(16).uppercase()}",
                        "${it.value}"
                    )
                }
            )
            emptyLine()

            expressionFunction(
                "getScriptId",
                listOf("offset" to "Int"),
                "Int",
                expression = "binarySearchRange(offset, RANGES, 0)"
            )
        }
    }.writeTo(writer)
}

private fun generateScriptDataFacade(
    writer: Writer,
    unicodeVersion: String,
    additionalComment: String
) {
    kotlinFile("me.zolotov.kodepoint.generated") {
        import("me.zolotov.kodepoint.script.UnicodeScript")

        kdoc {
            line("Auto-generated Unicode script data facade.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("ScriptData") {
            function("getScript", listOf("cp" to "Int"), "UnicodeScript") {
                returnWhen {
                    branch("cp < 0", "UnicodeScript.UNKNOWN")
                    branch("cp < 0x100", "UnicodeScript.entries[ScriptDataLatin1.getScriptId(cp)]")
                    branch("cp <= 0xFFFF", "UnicodeScript.entries[ScriptDataBMP.getScriptId(cp - 0x100)]")
                    branch("cp <= 0x1FFFF", "UnicodeScript.entries[ScriptDataSMP.getScriptId(cp - 0x10000)]")
                    branch("cp <= 0x2FFFF", "UnicodeScript.entries[ScriptDataSIP.getScriptId(cp - 0x20000)]")
                    branch("cp <= 0x10FFFF", "UnicodeScript.entries[ScriptDataSSP.getScriptId(cp - 0x30000)]")
                    elseCase("UnicodeScript.UNKNOWN")
                }
            }
        }
    }.writeTo(writer)
}
