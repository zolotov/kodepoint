import me.zolotov.kodepoint.gradle.UpdateReadmeVersionTask

plugins {
    id("org.jetbrains.changelog") version "2.5.0"
}

group = "me.zolotov.kodepoint"
description = """
    Lightweight Unicode code-point APIs for Kotlin Multiplatform strings: code-point-safe iteration and indexing, character classification, case conversion, and Unicode script/category lookup across JVM, Android, Apple, JS, Wasm, and native targets – without depending on ICU.
""".trimIndent()

subprojects {
    group = rootProject.group
    version = rootProject.version
    description = rootProject.description
}

// Changelog entries are hand-written into the "Unreleased" section of CHANGELOG.md as changes land
// (see RELEASING.md). `patchChangelog` only promotes that section to a version section at release
// time, and `getChangelog` hands the very same text to the GitHub release, so what ships is exactly
// what was reviewed.
changelog {
    title = "Change Log"
    // Releases are tagged with the bare version, e.g. `2.0.0`, not `v2.0.0`.
    versionPrefix = ""
    repositoryUrl = "https://github.com/zolotov/kodepoint"
    // No group headings are seeded into a fresh Unreleased section: an author adds the ones they
    // need (Breaking, Added, Changed, Fixed, Performance) rather than deleting five empty headings
    // after every release.
    groups = emptyList()
    // Never turn an empty Unreleased section into a release section: patchChangelog fails instead,
    // which is what stops a release from shipping without notes.
    patchEmpty = false
    outputFile = layout.buildDirectory.file("reports/changelog/latest-release-body.md")
}

tasks.register<UpdateReadmeVersionTask>("updateReadmeVersion") {
    group = "release"
    description = "Points the dependency snippets in README.md and llms.txt at the version being released."
    files.from("README.md", "llms.txt")
    coordinate = "me.zolotov.kodepoint:kodepoint"
    version = project.version.toString()
}
