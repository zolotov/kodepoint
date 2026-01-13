rootProject.name = "kodepoint"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":common")
include(":lib")
project(":lib").name = "kodepoint"
include(":unicode")
include(":benchmarks")
