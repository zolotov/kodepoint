import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(24)

    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("nonJvm") {
                withJs()
                withWasmJs()
                withWasmWasi()
                group("native")
            }
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.library()
        browser()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        binaries.library()
        nodejs()
    }
    js {
        browser()
        nodejs {
            testTask {
                useMocha {
                    timeout = "60s"
                }
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()
}

java {
    withSourcesJar()
}

mavenPublishing {
    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Dokka("dokkaGenerate"),
        sourcesJar = true
    ))
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/zolotov/kodepoint")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("zolotov")
                name.set("Alexander Zolotov")
                email.set("goldifit@gmail.com")
                url.set("https://github.com/zolotov/")
            }
        }
        scm {
            url.set("https://github.com/zolotov/kodepoint")
            connection.set("scm:git:git://github.com/zolotov/kodepoint.git")
            developerConnection.set("scm:ssh://github.com/zolotov/kodepoint.git")
        }
    }
}
