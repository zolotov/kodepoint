package me.zolotov.kodepoint.generator.code

import me.zolotov.kodepoint.generator.EastAsianWidthBuildResult
import me.zolotov.kodepoint.generator.EastAsianWidthPlaneResult
import me.zolotov.kodepoint.generator.UNICODE_VERSION
import me.zolotov.kodepoint.generator.dsl.kotlinFile
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

fun generateEastAsianWidthDataClasses(
    generatedDir: Path,
    buildResult: EastAsianWidthBuildResult,
    additionalComment: String
) {
    generatedDir.resolve("EastAsianWidthDataLatin1.kt").bufferedWriter().use { writer ->
        generateLatin1EawData(writer, buildResult.latin1WidthIds, UNICODE_VERSION, additionalComment)
        println("Generated EastAsianWidthDataLatin1 (256 entries)")
    }

    for (planeResult in buildResult.planeResults) {
        generatedDir.resolve("EastAsianWidthData${planeResult.plane.name}.kt")
            .createParentDirectories()
            .bufferedWriter().use { writer ->
                when (planeResult) {
                    is EastAsianWidthPlaneResult.Sparse -> generateSparseEawData(
                        writer, planeResult, UNICODE_VERSION, additionalComment
                    )
                    is EastAsianWidthPlaneResult.Table -> generatePlaneEawData(
                        writer, planeResult, UNICODE_VERSION, additionalComment
                    )
                }
            }
    }

    generatedDir.resolve("EastAsianWidthData.kt").bufferedWriter().use { writer ->
        generateEawDataFacade(writer, UNICODE_VERSION, additionalComment)
        println("Generated EastAsianWidthData facade")
    }
}

private fun generateLatin1EawData(
    writer: Writer,
    widthIds: IntArray,
    unicodeVersion: String,
    additionalComment: String
) {
    require(widthIds.size == 256) { "Latin1 requires exactly 256 width IDs" }

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode East Asian Width data for Latin-1 (0x00-0xFF).")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("EastAsianWidthDataLatin1") {
            byteArrayProperty("WIDTHS", widthIds.map { it.toString() })
            emptyLine()

            expressionFunction(
                "getWidthId",
                listOf("codepoint" to "Int"),
                "Int",
                expression = "WIDTHS[codepoint].toInt() and 0xFF"
            )
        }
    }.writeTo(writer)
}

private fun generatePlaneEawData(
    writer: Writer,
    planeResult: EastAsianWidthPlaneResult.Table,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = planeResult.plane
    val result = planeResult.table
    val className = "EastAsianWidthData${plane.name}"
    val blockBits = result.blockBits
    val blockSize = 1 shl blockBits
    val blockMask = blockSize - 1

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode East Asian Width data for ${plane.name}.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration(className) {
            const("BLOCK_SHIFT", blockBits, private = true)
            const("BLOCK_MASK", blockMask, private = true)
            const("BLOCK_SIZE", blockSize, private = true)
            emptyLine()

            encodedString16Property("BLOCK_INDEX", result.indexTable)
            emptyLine()

            byteArrayProperty("WIDTHS", result.dataTable.map { it.toString() })
            emptyLine()

            function("getWidthId", listOf("offset" to "Int"), "Int") {
                variable("blockNum", "BLOCK_INDEX_DATA[offset ushr BLOCK_SHIFT].code")
                returnStatement("WIDTHS[blockNum * BLOCK_SIZE + (offset and BLOCK_MASK)].toInt() and 0xFF")
            }
        }
    }.writeTo(writer)
}

private fun generateSparseEawData(
    writer: Writer,
    planeResult: EastAsianWidthPlaneResult.Sparse,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = planeResult.plane
    val ranges = planeResult.ranges
    val className = "EastAsianWidthData${plane.name}"

    kotlinFile("me.zolotov.kodepoint.generated") {
        import("me.zolotov.kodepoint.internal.binarySearchRange")

        kdoc {
            line("Auto-generated Unicode East Asian Width data for ${plane.name}.")
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
                "getWidthId",
                listOf("offset" to "Int"),
                "Int",
                expression = "binarySearchRange(offset, RANGES, 0)"
            )
        }
    }.writeTo(writer)
}

private fun generateEawDataFacade(
    writer: Writer,
    unicodeVersion: String,
    additionalComment: String
) {
    kotlinFile("me.zolotov.kodepoint.generated") {
        import("me.zolotov.kodepoint.eaw.EastAsianWidth")

        kdoc {
            line("Auto-generated Unicode East Asian Width data facade.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("EastAsianWidthData") {
            function("getEastAsianWidth", listOf("cp" to "Int"), "EastAsianWidth") {
                returnWhen {
                    branch("cp < 0", "EastAsianWidth.NEUTRAL")
                    branch("cp < 0x100", "EastAsianWidth.entries[EastAsianWidthDataLatin1.getWidthId(cp)]")
                    branch("cp <= 0xFFFF", "EastAsianWidth.entries[EastAsianWidthDataBMP.getWidthId(cp - 0x100)]")
                    branch("cp <= 0x1FFFF", "EastAsianWidth.entries[EastAsianWidthDataSMP.getWidthId(cp - 0x10000)]")
                    branch("cp <= 0x2FFFF", "EastAsianWidth.entries[EastAsianWidthDataSIP.getWidthId(cp - 0x20000)]")
                    branch("cp <= 0x10FFFF", "EastAsianWidth.entries[EastAsianWidthDataSSP.getWidthId(cp - 0x30000)]")
                    elseCase("EastAsianWidth.NEUTRAL")
                }
            }
        }
    }.writeTo(writer)
}
