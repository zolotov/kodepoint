package me.zolotov.kodepoint.generator.code

import me.zolotov.kodepoint.generator.LastCaseDeltaRanges
import me.zolotov.kodepoint.generator.PlaneTableResult
import me.zolotov.kodepoint.generator.UNICODE_VERSION
import me.zolotov.kodepoint.generator.dsl.kotlinFile
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

fun generateCharacterDataClasses(
    outputDir: Path,
    latin1Properties: IntArray,
    additionalComment: String,
    planeResults: List<PlaneTableResult>,
    largeCaseDeltaRanges: LastCaseDeltaRanges
) {
    outputDir.resolve("CharacterDataLatin1.kt").bufferedWriter().use { writer ->
        generateLatin1CharacterData(
            writer,
            latin1Properties,
            UNICODE_VERSION,
            additionalComment
        )
        println("Generated CharacterDataLatin1 (256 entries, ${256 * 4} bytes)")
    }

    for (planeResult in planeResults) {
        outputDir.resolve("${planeResult.plane.className}.kt").bufferedWriter().use { writer ->
            when (planeResult) {
                is PlaneTableResult.Sparse -> generateSparseCharacterData(
                    writer,
                    planeResult,
                    UNICODE_VERSION,
                    additionalComment
                )

                is PlaneTableResult.Table -> generatePlaneCharacterData(
                    writer,
                    planeResult,
                    UNICODE_VERSION,
                    additionalComment
                )
            }
        }
    }

    // Generate CharacterData facade
    outputDir.resolve("CharacterData.kt").bufferedWriter().use { writer ->
        generateCharacterDataFacade(
            writer,
            largeCaseDeltaRanges,
            UNICODE_VERSION,
            additionalComment
        )
        println("Generated CharacterData facade")
    }
}

private fun generateLatin1CharacterData(
    writer: Writer,
    properties: IntArray,
    unicodeVersion: String,
    additionalComment: String
) {
    require(properties.size == 256) { "Latin1 requires exactly 256 property values" }

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode character property data for Latin-1 (0x00-0xFF).")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("CharacterDataLatin1") {
            intArrayProperty(
                "PROPERTIES",
                properties.map { "0x${it.toUInt().toString(16).uppercase()}" }
            )
            emptyLine()

            expressionFunction(
                "getProperties",
                listOf("codepoint" to "Int"),
                "Int",
                expression = "PROPERTIES[codepoint]"
            )
        }
    }.writeTo(writer)
}

private fun generatePlaneCharacterData(
    writer: Writer,
    planeResult: PlaneTableResult.Table,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = planeResult.plane
    val result = planeResult.table
    val blockBits = result.blockBits
    val blockSize = 1 shl blockBits
    val blockMask = blockSize - 1
    val indexTable = result.indexTable
    val propertyTable = result.dataTable

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode character property data for ${plane.name}.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration(plane.className) {
            const("BLOCK_SHIFT", blockBits, private = true)
            const("BLOCK_MASK", blockMask, private = true)
            const("BLOCK_SIZE", blockSize, private = true)
            emptyLine()

            encodedString16Property("BLOCK_INDEX", indexTable)
            emptyLine()

            encodedString32Property("PROPERTIES", propertyTable)
            emptyLine()

            function("getProperties", listOf("offset" to "Int"), "Int") {
                variable("blockNum", "BLOCK_INDEX_DATA[offset ushr BLOCK_SHIFT].code")
                variable("propIdx", "(blockNum * BLOCK_SIZE + (offset and BLOCK_MASK)) * 2")
                returnStatement("(PROPERTIES_DATA[propIdx].code shl 16) or PROPERTIES_DATA[propIdx + 1].code")
            }
        }
    }.writeTo(writer)
}

private fun generateSparseCharacterData(
    writer: Writer,
    planeResult: PlaneTableResult.Sparse,
    unicodeVersion: String,
    additionalComment: String
) {
    val plane = planeResult.plane
    val ranges = planeResult.ranges

    kotlinFile("me.zolotov.kodepoint.generated") {
        import("me.zolotov.kodepoint.internal.binarySearchRange")

        kdoc {
            line("Auto-generated Unicode character property data for ${plane.name}.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration(plane.className) {
            intArrayProperty(
                "RANGES",
                ranges.flatMap {
                    listOf(
                        "0x${it.startCodepoint.toString(16).uppercase()}",
                        "0x${it.endCodepoint.toString(16).uppercase()}",
                        "0x${it.value.toUInt().toString(16).uppercase()}"
                    )
                }
            )
            emptyLine()

            expressionFunction(
                "getProperties",
                listOf("offset" to "Int"),
                "Int",
                expression = "binarySearchRange(offset, RANGES, 0)"
            )
        }
    }.writeTo(writer)
}

private fun generateCharacterDataFacade(
    writer: Writer,
    largeCaseDeltaRanges: LastCaseDeltaRanges,
    unicodeVersion: String,
    additionalComment: String
) {
    val toLowercaseRanges = largeCaseDeltaRanges.toLower
    val toUppercaseRanges = largeCaseDeltaRanges.toUpper

    kotlinFile("me.zolotov.kodepoint.generated") {
        kdoc {
            line("Auto-generated Unicode character property data facade.")
            line("Unicode version: $unicodeVersion")
            emptyLine()
            multiline(additionalComment)
        }

        objectDeclaration("CharacterData") {
            // Bit constants (internal for use by Codepoints)
            const("CASE_DELTA_MASK", "0x3FF", private = false)
            const("DELTA_TO_LOWERCASE_BIT", "1 shl 10", private = false)
            const("CATEGORY_MASK", "0x1F shl 11", private = false)
            const("CATEGORY_SHIFT", 11, private = false)
            const("IS_OTHER_UPPERCASE_BIT", "1 shl 16", private = false)
            const("IS_OTHER_LOWERCASE_BIT", "1 shl 17", private = false)
            const("IS_WHITESPACE_BIT", "1 shl 18", private = false)
            const("IS_IDEOGRAPHIC_BIT", "1 shl 19", private = false)
            const("IS_UNICODE_ID_START_BIT", "1 shl 20", private = false)
            const("IS_UNICODE_ID_PART_BIT", "1 shl 21", private = false)
            const("IS_JAVA_ID_START_BIT", "1 shl 22", private = false)
            const("IS_JAVA_ID_PART_BIT", "1 shl 23", private = false)
            const("HAS_LARGE_LOWERCASE_DELTA_BIT", "1 shl 24", private = false)
            const("HAS_LARGE_UPPERCASE_DELTA_BIT", "1 shl 25", private = false)
            emptyLine()

            // Category constants
            const("CAT_LU", 1, private = false)
            const("CAT_LL", 2, private = false)
            const("CAT_LT", 3, private = false)
            const("CAT_LM", 4, private = false)
            const("CAT_LO", 5, private = false)
            const("CAT_ND", 9, private = false)
            const("CAT_ZS", 23, private = false)
            const("CAT_ZL", 24, private = false)
            const("CAT_ZP", 25, private = false)
            const("CAT_CC", 26, private = false)
            const("CAT_CF", 27, private = false)
            emptyLine()

            // Large case delta ranges
            if (toLowercaseRanges.isNotEmpty()) {
                intArrayProperty(
                    "largeLowercaseRanges",
                    toLowercaseRanges.flatMap {
                        listOf(
                            "0x${it.startCodepoint.toString(16).uppercase()}",
                            "0x${it.endCodepoint.toString(16).uppercase()}",
                            "${it.value}"
                        )
                    },
                    private = false
                )
            }

            if (toUppercaseRanges.isNotEmpty()) {
                intArrayProperty(
                    "largeUppercaseRanges",
                    toUppercaseRanges.flatMap {
                        listOf(
                            "0x${it.startCodepoint.toString(16).uppercase()}",
                            "0x${it.endCodepoint.toString(16).uppercase()}",
                            "${it.value}"
                        )
                    },
                    private = false
                )
            }

            emptyLine()

            // getProperties function with Latin1 fast-path
            function("getProperties", listOf("cp" to "Int"), "Int") {
                returnWhen {
                    branch("cp < 0", "0")
                    branch("cp < 0x100", "CharacterDataLatin1.getProperties(cp)")
                    branch("cp <= 0xFFFF", "CharacterDataBMP.getProperties(cp - 0x100)")
                    branch("cp <= 0x1FFFF", "CharacterDataSMP.getProperties(cp - 0x10000)")
                    branch("cp <= 0x2FFFF", "CharacterDataSIP.getProperties(cp - 0x20000)")
                    branch("cp <= 0x10FFFF", "CharacterDataSSP.getProperties(cp - 0x30000)")
                    elseCase("0")
                }
            }
        }
    }.writeTo(writer)
}
