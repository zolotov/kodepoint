package me.zolotov.kodepoint.generator

import java.nio.file.Path
import kotlin.io.path.forEachLine

/**
 * Parses Unicode data files and builds character property maps.
 */
fun parsedUnicodeData(
    unicodeDataFile: Path,
    derivedPropertiesFile: Path,
    caseFoldingsFile: Path,
    specialCasingFile: Path,
    scriptsFile: Path,
    propListFile: Path
): UnicodeData {
    val characters = Array(MAX_CODEPOINT + 1) { CharacterData(it) }
    parseUnicodeData(unicodeDataFile, characters)
    parseDerivedCoreProperties(derivedPropertiesFile, characters)
    parseCaseFolding(caseFoldingsFile, characters)
    parsePropList(propListFile, characters)
    parseSpecialCasing(specialCasingFile, characters)
    val scripts = parseScriptsFile(scriptsFile)

    // Compute derived boolean properties from category and JVM Character class
    computeDerivedProperties(characters)

    return UnicodeData(characters = characters, scripts = scripts)
}

private fun computeDerivedProperties(characters: Array<CharacterData>) {
    characters.forEach { char ->
        val cat = char.category

        // Derive from category
        char.isLetter = cat in GeneralCategory.Lu..GeneralCategory.Lo
        char.isDigit = cat == GeneralCategory.Nd
        char.isUpperCase = cat == GeneralCategory.Lu || char.isOtherUppercase
        char.isLowerCase = cat == GeneralCategory.Ll || char.isOtherLowercase
        char.isTitleCase = cat == GeneralCategory.Lt
        char.isSpaceChar = cat == GeneralCategory.Zs || cat == GeneralCategory.Zl || cat == GeneralCategory.Zp
        char.isISOControl = cat == GeneralCategory.Cc
        char.isFormatChar = cat == GeneralCategory.Cf

        // Get Java-specific properties directly from JVM Character class
        char.isJavaIdentifierStart = Character.isJavaIdentifierStart(char.codepoint)
        char.isJavaIdentifierPart = Character.isJavaIdentifierPart(char.codepoint)
    }
}

private fun parsePropList(propListFile: Path, characters: Array<CharacterData>) {
    propListFile.forEachLine { line ->
        if (!line.isBlank() && !line.startsWith("#")) {
            val commentIdx = line.indexOf('#')
            val data = if (commentIdx >= 0) line.substring(0, commentIdx) else line
            val parts = data.split(';').map { it.trim() }
            if (parts.size == 2) {
                val range = parseRange(parts[0])
                val property = parts[1]

                for (cp in range) {
                    val char = characters[cp]
                    when (property) {
                        "White_Space" -> char.isWhitespace = true
                        "Ideographic" -> char.isIdeographic = true
                        "Other_Lowercase" -> char.isOtherLowercase = true
                        "Other_Uppercase" -> char.isOtherUppercase = true
                    }
                }
            }
        }
    }
}

private fun parseUnicodeData(unicodeDataFile: Path, characters: Array<CharacterData>) {
    var rangeStart: Int? = null
    unicodeDataFile.forEachLine { line ->
        if (!line.isBlank() && !line.startsWith("#")) {
            val fields = line.split(';')
            if (fields.size == 15) {
                val codepoint = fields[0].trim().toInt(16)
                val name = fields[1].trim()
                val category = GeneralCategory.fromAbbrev(fields[2].trim())
                val upperCase = fields[12].trim().takeIf { it.isNotEmpty() }?.toInt(16) ?: -1
                val lowerCase = fields[13].trim().takeIf { it.isNotEmpty() }?.toInt(16) ?: -1

                // Handle ranges like "<CJK Ideograph, First>"
                when {
                    name.endsWith(", First>") -> {
                        rangeStart = codepoint
                    }

                    name.endsWith(", Last>") && rangeStart != null -> {
                        // Fill the range with the same category
                        for (cp in rangeStart..codepoint) {
                            val data = characters[cp]
                            data.category = category
                        }
                        rangeStart = null
                    }

                    else -> {
                        val data = characters[codepoint]
                        data.category = category
                        data.upperCase = upperCase
                        data.lowerCase = lowerCase
                    }
                }
            }
        }
    }
}

private fun parseDerivedCoreProperties(
    derivedCorePropertiesFile: Path,
    characters: Array<CharacterData>
) {
    derivedCorePropertiesFile.forEachLine { line ->
        if (!line.isBlank() && !line.startsWith("#")) {
            val commentIdx = line.indexOf('#')
            val data = if (commentIdx >= 0) line.substring(0, commentIdx) else line
            val parts = data.split(';').map { it.trim() }
            if (parts.size == 2) {
                val range = parseRange(parts[0])
                val property = parts[1]

                for (cp in range) {
                    val char = characters[cp]
                    when (property) {
                        "ID_Start" -> char.isIdStart = true
                        "ID_Continue" -> char.isIdContinue = true
                        "XID_Start" -> char.isXidStart = true
                        "XID_Continue" -> char.isXidContinue = true
                    }
                }
            }
        }
    }
}

private fun parseCaseFolding(caseFoldingFile: Path, characters: Array<CharacterData>) {
    caseFoldingFile.forEachLine { line ->
        if (!line.isBlank() && !line.startsWith("#")) {
            val commentIdx = line.indexOf('#')
            val data = if (commentIdx >= 0) line.substring(0, commentIdx) else line
            val parts = data.split(';').map { it.trim() }
            if (parts.size == 3) {
                val codepoint = parts[0].toInt(16)
                val status = parts[1]
                val mapping = parts[2]
                when (status) {
                    // C = common case folding, S = simple case folding
                    "C", "S" -> {
                        val foldedTo = mapping.toInt(16)
                        characters[codepoint].caseFold = foldedTo
                    }
                    // For F (full) mappings, mark as special case
                    "F" -> {
                        characters[codepoint].hasSpecialCase = true
                    }
                }
            }
        }
    }
}

private fun parseSpecialCasing(specialCasingFile: Path, characters: Array<CharacterData>) {
    specialCasingFile.forEachLine { line ->
        if (!line.isBlank() && !line.startsWith("#")) {
            val commentIdx = line.indexOf('#')
            val data = if (commentIdx >= 0) line.substring(0, commentIdx) else line
            val parts = data.split(';').map { it.trim() }
            if (parts.size == 4 || parts.size == 5 && parts[4].isEmpty()) {
                val codepoint = parts[0].toInt(16)
                val lowercase = parseCodepoints(parts[1])
                val titlecase = parseCodepoints(parts[2])
                val uppercase = parseCodepoints(parts[3])
                // Only store if it's actually a special case (multi-char)
                if (lowercase.size > 1 || titlecase.size > 1 || uppercase.size > 1) {
                    characters[codepoint].hasSpecialCase = true
                }
            }
        }
    }
}

private fun parseCodepoints(s: String): IntArray {
    if (s.isEmpty()) return intArrayOf()
    return s.split(' ').filter { it.isNotEmpty() }.map { it.toInt(16) }.toIntArray()
}

/**
 * Parse a codepoint range like "0041..005A" or single codepoint like "0041".
 */
fun parseRange(s: String): IntRange {
    val separatorIndex = s.indexOf("..")
    return if (separatorIndex >= 0) {
        s.substring(0, separatorIndex).toInt(16)..s.substring(separatorIndex + 2).toInt(16)
    } else {
        s.toInt(16)..s.toInt(16)
    }
}
