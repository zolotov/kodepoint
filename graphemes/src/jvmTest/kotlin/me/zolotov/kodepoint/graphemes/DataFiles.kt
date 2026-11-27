package me.zolotov.kodepoint.graphemes

/**
 * The Unicode data files the tables were generated from, copied into the test resources by
 * `:graphemes:generateGraphemeData`. The tests parse them on their own, independently of the generator.
 */
internal object DataFiles {
    fun lines(name: String): List<String> {
        val stream = DataFiles::class.java.getResourceAsStream("/grapheme-data/$name")
            ?: error("Missing test resource grapheme-data/$name")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readLines() }
    }

    /** `XXXX..YYYY ; field ; field # comment` lines as code point ranges and fields. */
    fun ranges(name: String): List<Pair<IntRange, List<String>>> = lines(name).mapNotNull { line ->
        val content = line.substringBefore('#').trim()
        if (content.isEmpty()) return@mapNotNull null
        val fields = content.split(';').map { it.trim() }
        val bounds = fields[0].split("..").map { it.toInt(16) }
        bounds.first()..bounds.last() to fields.drop(1)
    }

    /** The values of `# @missing: 0000..10FFFF; ...` lines. */
    fun missing(name: String): List<List<String>> = lines(name)
        .filter { it.startsWith("# @missing:") }
        .map { line -> line.removePrefix("# @missing:").split(';').drop(1).map { it.trim() } }
}
