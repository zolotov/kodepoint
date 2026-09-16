package me.zolotov.kodepoint.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
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

    /** Group and artifact, for example `me.zolotov.kodepoint:kodepoint`. */
    @get:Input
    abstract val coordinate: Property<String>

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun update() {
        val coordinate = coordinate.get()
        val version = version.get()
        // The version is whatever follows the coordinate inside a quoted Gradle snippet, so stop at
        // the closing quote: `implementation("me.zolotov.kodepoint:kodepoint:2.0.0")`.
        val versionPart = Regex("(${Regex.escape("$coordinate:")})[^\"]+")

        for (file in files) {
            val original = file.readText()
            val occurrences = versionPart.findAll(original).count()
            require(occurrences > 0) {
                "Found no `$coordinate:<version>` coordinate in ${file.name}. The release keeps the " +
                    "install snippets copy-pasteable, so either the snippets moved or this task needs " +
                    "updating; releasing with stale install instructions is not the intended outcome."
            }

            val updated = versionPart.replace(original) { it.groupValues[1] + version }
            if (updated == original) {
                logger.lifecycle("${file.name} already documents $coordinate:$version")
            } else {
                file.writeText(updated)
                logger.lifecycle("Documented $coordinate:$version in ${file.name} ($occurrences occurrences)")
            }
        }
    }
}
