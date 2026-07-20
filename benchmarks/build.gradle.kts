import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import me.zolotov.kodepoint.gradle.BenchmarkReportTask
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen") version "2.2.20"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.17"
}

tasks.withType<PublishToMavenRepository>().configureEach { enabled = false }
tasks.withType<PublishToMavenLocal>().configureEach { enabled = false }
tasks.withType<Sign>().configureEach { enabled = false }

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
    jvmToolchain(26)
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
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.17")
                implementation(project(":kodepoint"))
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

tasks.register("wasmJsBenchmarkPackage") {
    group = "benchmark"
    description = "Build the packaged Node/Wasm benchmark executable for 'wasmJs'"
    dependsOn("wasmJsWasmJsBenchmarkWasmJsBenchmarkProductionExecutableCompileSync")
}

tasks.register<BenchmarkReportTask>("ciBenchmark") {
    group = "benchmark"
    description = "Run CI benchmark targets, normalize the outputs, emit a GitHub summary, and build a Pages bundle."
    dependsOn("jvmQuickBenchmark", "wasmJsQuickBenchmark", ":unicode:characterDataMetrics")

    benchmarkReportsDirectory.set(layout.buildDirectory.dir("reports/benchmarks"))
    characterDataMetricsFile.set(project(":unicode").layout.buildDirectory.file("reports/character-data/metrics.json"))
    siteTemplateDirectory.set(layout.projectDirectory.dir("site"))
    outputDirectory.set(layout.buildDirectory.dir("ci"))
    siteUrl.convention(providers.gradleProperty("benchmarkSiteUrl"))
    historyLimit.convention(providers.gradleProperty("benchmarkHistoryLimit").map(String::toInt).orElse(90))
    significanceThreshold.convention(
        providers.gradleProperty("benchmarkSignificanceThreshold").map(String::toDouble).orElse(0.03)
    )

    providers.gradleProperty("benchmarkHistoryFile").orNull?.let { historyPath ->
        rootProject.file(historyPath)
            .takeIf { it.isFile }
            ?.let(historyFile::set)
    }

    prNumber.convention(providers.gradleProperty("benchmarkPrNumber"))
    providers.gradleProperty("benchmarkPrHistoryFile").orNull?.let { historyPath ->
        rootProject.file(historyPath)
            .takeIf { it.isFile }
            ?.let(prHistoryFile::set)
    }
}
