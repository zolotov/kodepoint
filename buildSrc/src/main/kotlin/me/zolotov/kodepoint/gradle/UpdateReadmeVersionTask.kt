package me.zolotov.kodepoint.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Rewrites its own inputs in place")
abstract class UpdateReadmeVersionTask : DefaultTask() {

    /** README-style documents whose Gradle snippets name the published coordinate. */
    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    /**
     * Group and artifact of every published artifact the documents name, for example
     * `me.zolotov.kodepoint:kodepoint`. Each one must appear in every file.
     */
    @get:Input
    abstract val coordinates: ListProperty<String>

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun update() {
        val version = version.get()
        for (file in files) {
            var text = file.readText()
            for (coordinate in coordinates.get()) {
                // The version is whatever follows the coordinate inside a quoted Gradle snippet, so stop at
                // the closing quote: `implementation("me.zolotov.kodepoint:kodepoint:2.0.0")`. The trailing
                // colon keeps `kodepoint:` from matching `kodepoint-graphemes:`.
                val versionPart = Regex("(${Regex.escape("$coordinate:")})[^\"]+")
                val occurrences = versionPart.findAll(text).count()
                require(occurrences > 0) {
                    "Found no `$coordinate:<version>` coordinate in ${file.name}. The release keeps the " +
                        "install snippets copy-pasteable, so either the snippets moved or this task needs " +
                        "updating; releasing with stale install instructions is not the intended outcome."
                }

                val updated = versionPart.replace(text) { it.groupValues[1] + version }
                if (updated == text) {
                    logger.lifecycle("${file.name} already documents $coordinate:$version")
                } else {
                    logger.lifecycle("Documented $coordinate:$version in ${file.name} ($occurrences occurrences)")
                }
                text = updated
            }
            if (text != file.readText()) file.writeText(text)
        }
    }
}
