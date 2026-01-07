import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
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
