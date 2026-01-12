package me.zolotov.kodepoint.generator

import me.zolotov.kodepoint.generator.code.generateCharacterDataClasses
import me.zolotov.kodepoint.generator.code.generateScriptDataClasses
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

/**
 * Main entry point for generating Unicode data classes.
 *
 * This orchestrates the following steps:
 * 1. Download/cache Unicode data files
 * 2. Parse Unicode data
 * 3. Build property and script tables
 * 4. Generate Kotlin source files
 */
@Suppress("unused")
@OptIn(ExperimentalPathApi::class)
fun generateUnicodeData(outputDir: Path, cacheDir: Path, additionalComment: String) {
    println("Running Kodepoint Generator...")
    println("Unicode version: $UNICODE_VERSION")

    val dataFiles = listOf(
        "UnicodeData.txt",
        "PropList.txt",
        "DerivedCoreProperties.txt",
        "Scripts.txt",
        "CaseFolding.txt",
        "SpecialCasing.txt"
    )
    val dataDir = cacheDir.resolve("unicode-data-$UNICODE_VERSION")
    UnicodeDataDownloader.ensureUnicodeFilesDownloaded(dataDir, dataFiles)

    println("Parsing Unicode data files...")
    val unicodeData = parsedUnicodeData(
        unicodeDataFile = dataDir.resolve("UnicodeData.txt"),
        derivedPropertiesFile = dataDir.resolve("DerivedCoreProperties.txt"),
        caseFoldingsFile = dataDir.resolve("CaseFolding.txt"),
        specialCasingFile = dataDir.resolve("SpecialCasing.txt"),
        scriptsFile = dataDir.resolve("Scripts.txt"),
        propListFile = dataDir.resolve("PropList.txt")
    )
    println("Parsed ${unicodeData.scripts.size} script mappings")

    println("Packing character properties...")
    val characterProperties = unicodeData.characters.map { cp -> PropertyPacker.pack(cp) }.toIntArray()
    println("Packed ${characterProperties.size} character properties")

    println("Packing properties by plane...")
    val planeResults = buildPropertyTables(characterProperties)
    val totalPropertySize = planeResults.sumOf { it.size }
    println("Total property table size: $totalPropertySize bytes")

    println("Build large case deltas...")
    val largeCaseDeltaRanges = buildLargeCaseDeltas(unicodeData.characters, characterProperties)
    println("Large case delta ranges: ${largeCaseDeltaRanges.toLower.size} toLower ranges, ${largeCaseDeltaRanges.toUpper.size} toUpper ranges")

    println("Building script tables by plane...")
    val scriptBuildResult = buildScriptTables(unicodeData.scripts)
    var totalScriptSize = 0
    for (planeResult in scriptBuildResult.planeResults) {
        when (planeResult) {
            is ScriptPlaneResult.Sparse -> {
                println("  ${planeResult.plane.name}: sparse (${planeResult.ranges.size} ranges), size=${planeResult.size} bytes")
            }

            is ScriptPlaneResult.Table -> {
                println("  ${planeResult.plane.name}: indexBits=${planeResult.table.indexBits}, blockBits=${planeResult.table.blockBits}, size=${planeResult.size} bytes")
            }
        }
        totalScriptSize += planeResult.size
    }
    println("Total script table size: $totalScriptSize bytes")
    println("Script count: ${scriptBuildResult.scriptNames.size}")

    val totalSize = totalPropertySize + totalScriptSize
    println("Total generated data size: $totalSize bytes")

    outputDir.deleteRecursively()
    val generatedDir = outputDir.resolve("me/zolotov/kodepoint/generated").createDirectories()

    val latin1Properties = characterProperties.sliceArray(0..255)
    generateCharacterDataClasses(generatedDir, latin1Properties, additionalComment, planeResults, largeCaseDeltaRanges)
    generateScriptDataClasses(generatedDir, scriptBuildResult, additionalComment)

    println("Generation complete!")
}
