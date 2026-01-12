import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(24)

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.library()
        browser()
    }

    sourceSets {
        val nonJvmMain by creating {
            dependsOn(commonMain.get())
        }
        val nonJvmTest by creating {
            dependsOn(commonTest.get())
        }

        wasmJsMain { dependsOn(nonJvmMain) }
        wasmJsTest { dependsOn(nonJvmTest) }
    }
}

java {
    withSourcesJar()
}

mavenPublishing {
    configure(KotlinMultiplatform(
        javadocJar = JavadocJar.Dokka("dokkaHtml"),
        sourcesJar = true
    ))
    publishToMavenCentral(automaticRelease = true)
    // Only sign if signing key is available
    if (project.findProperty("signingInMemoryKey") != null || System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
        signAllPublications()
    }
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