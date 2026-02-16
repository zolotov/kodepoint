import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen") version "2.3.10"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.16"
}

tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
tasks.withType<Sign>().configureEach { enabled = false }

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
    jvmToolchain(24)
    applyDefaultHierarchyTemplate()
    jvm()
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        binaries.executable()
    }
    
    js {
        browser()
        nodejs()
    }
    
    macosArm64()
    
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.15")
                implementation(project(":unicode"))
                implementation(project(":common"))
            }
        }
    }
}

// Fix implicit dependency issues with Gradle 9.x and kotlinx-benchmark wasmJs
// https://docs.gradle.org/current/userguide/validation_problems.html#implicit_dependency
afterEvaluate {
    tasks.findByName("wasmJsBrowserProductionWebpack")?.dependsOn("wasmJsProductionLibraryCompileSync")
    tasks.matching { it.name.contains("wasmJs") && it.name.contains("LibraryDistribution") }.configureEach {
        dependsOn("wasmJsProductionExecutableCompileSync")
    }
}

benchmark {
    configurations {
        named("main") {
            warmups = 5
            iterations = 5
            iterationTime = 1000
            iterationTimeUnit = "ms"
            mode = "thrpt"
            outputTimeUnit = "us"
        }

        register("quick") {
            warmups = 2
            iterations = 3
            iterationTime = 500
            iterationTimeUnit = "ms"
        }
    }
    targets {
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion  = "1.37"
        }
        register("wasmJs")
        register("macosArm64")
    }
}
