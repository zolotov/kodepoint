plugins {
    id("kodepoint.multiplatform")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":common"))
            }
        }
        named("nonJvmMain") {
            dependencies {
                implementation(project(":unicode"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
