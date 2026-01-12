package me.zolotov.kodepoint.generator

import me.zolotov.kodepoint.generator.dsl.kotlinFile
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Generates the UnicodeScript enum from Unicode data.
 */
@Suppress("unused")
@OptIn(ExperimentalPathApi::class)
fun generateUnicodeScript(outputDir: Path, cacheDir: Path, additionalComment: String) {
    println("Running UnicodeScript Generator...")
    println("Unicode version: $UNICODE_VERSION")

    val dataDir = cacheDir.resolve("unicode-data-$UNICODE_VERSION")
    UnicodeDataDownloader.ensureUnicodeFilesDownloaded(dataDir, listOf("Scripts.txt"))

    println("Parsing Scripts.txt...")
    val scripts = parseScriptsFile(dataDir.resolve("Scripts.txt"))
    println("Parsed scripts for ${scripts.size} codepoints")

    println("Extracting script names in codepoint order...")
    val scriptNames = extractScriptNamesInCodepointOrder(scripts)
    println("Found ${scriptNames.size} unique scripts")

    val generator = kotlinFile("me.zolotov.kodepoint.script") {
        kdoc {
            line("Unicode Script values.")
            emptyLine()
            line("Source: $UNICODE_BASE_URL/Scripts.txt")
            emptyLine()
            multiline(additionalComment)
        }

        enumClass("UnicodeScript", annotations = listOf("@Suppress(\"SpellCheckingInspection\")")) {
            entries(scriptNames.map { it.asEnumValue() })
        }
    }

    outputDir.deleteRecursively()
    outputDir.resolve("me/zolotov/kodepoint/script/UnicodeScript.kt")
        .createParentDirectories()
        .bufferedWriter().use { writer ->
            generator.writeTo(writer)
            println("Generated UnicodeScript.kt with ${scriptNames.size} scripts")
            println("Generation complete!")
        }
}

/**
 * Parse Scripts.txt and build a map of codepoint -> script name.
 */
fun parseScriptsFile(scriptsFile: Path): Map<Int, String> {
    return buildMap {
        scriptsFile.forEachLine { line ->
            if (!line.isBlank() && !line.startsWith("#")) {
                val data = line.substringBefore('#')
                val (range, script) = data.split(';', limit = 2).map { it.trim() }
                for (cp in parseRange(range.trim())) {
                    put(cp, script)
                }
            }
        }
    }
}

/**
 * Extract script names in the order they first appear when iterating by codepoint.
 * This must match exactly how unicode/generator assigns script IDs so that
 * the enum ordinals match the script IDs in the lookup tables.
 */
private fun extractScriptNamesInCodepointOrder(scripts: Map<Int, String>): List<String> {
    val scriptNames = mutableListOf("Unknown")
    val seen = mutableSetOf("Unknown")
    for (cp in 0..MAX_CODEPOINT) {
        val script = scripts[cp] ?: "Unknown"
        if (seen.add(script)) {
            scriptNames.add(script)
        }
    }
    return scriptNames
}

private fun String.asEnumValue(): String = uppercase().replace('-', '_')