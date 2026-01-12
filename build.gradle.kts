plugins {
    id("org.hildan.github.changelog") version "2.2.0"
    id("ru.vyarus.github-info") version "2.0.0"
}

group = "me.zolotov.kodepoint"
description = """
    A Kotlin multiplatform library providing limited Unicode Character Database functionality.
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