plugins {
    id("org.hildan.github.changelog") version "2.2.0"
    id("ru.vyarus.github-info") version "2.0.0"
}

group = "me.zolotov.kodepoint"
description = """
    Lightweight Unicode code-point APIs for Kotlin Multiplatform strings: code-point-safe iteration and indexing, character classification, case conversion, and Unicode script/category lookup across JVM, Android, Apple, JS, Wasm, and native targets — without depending on ICU.
""".trimIndent()

subprojects {
    group = rootProject.group
    version = rootProject.version
    description = rootProject.description
}

github {
    user = "zolotov"
    license = "Apache"
}

changelog {
    githubUser = github.user
    futureVersionTag = project.version.toString()
    outputFile = file("${rootProject.projectDir}/CHANGELOG.md")
}