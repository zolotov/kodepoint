package me.zolotov.kodepoint.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.forEachLine

internal val UNICODE_SCRIPT_CLASS_NAME = ClassName("me.zolotov.kodepoint.script", "UnicodeScript")

/**
 * Generates the UnicodeScript enum from Unicode data.
 */
@Suppress("unused")
@OptIn(ExperimentalPathApi::class)
fun generateUnicodeScript(unicodeVersion: UnicodeVersion, outputDir: Path, cacheDir: Path, additionalComment: String) {
    println("Running UnicodeScript Generator...")
    println("Unicode version: $unicodeVersion")

    val dataDir = UnicodeDataDownloader.ensureUnicodeFilesDownloaded(cacheDir, unicodeVersion, listOf("Scripts.txt"))

    println("Parsing Scripts.txt...")
    val scripts = parseScriptsFile(dataDir.resolve("Scripts.txt"))
    println("Parsed scripts for ${scripts.size} codepoints")

    println("Extracting script names in codepoint order...")
    val scriptNames = extractScriptNamesInCodepointOrder(scripts)
    println("Found ${scriptNames.size} unique scripts")

    val enumSpec = TypeSpec.enumBuilder("UnicodeScript")
        .addKdoc(
            """
            Unicode Script values.

            Source: ${unicodeVersion.ucdBaseUrl}/Scripts.txt

            $additionalComment
            """.trimIndent()
        )
    scriptNames.forEach {
        enumSpec.addEnumConstant(it.uppercase().replace('-', '_'))
    }

    outputDir.deleteRecursively()
    FileSpec.builder(UNICODE_SCRIPT_CLASS_NAME)
        .addType(enumSpec.build())
        .build()
        .writeTo(outputDir)
    println("Generated UnicodeScript.kt with ${scriptNames.size} scripts")
    println("Generation complete!")
}

/**
 * Parse Scripts.txt and build a map of codepoint -> script name.
 */
fun parseScriptsFile(scriptsFile: Path): Map<Int, String> {
    return buildMap {
        scriptsFile.forEachLine { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
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
    return buildList {
        add("Unknown")
        val seen = mutableSetOf("Unknown")
        for (cp in 0..MAX_CODEPOINT) {
            val script = scripts[cp] ?: "Unknown"
            if (seen.add(script)) {
                add(script)
            }
        }
    }
}