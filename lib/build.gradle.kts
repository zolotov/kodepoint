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
        val nonJvmMain by getting {
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
