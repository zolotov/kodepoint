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
        val jvmMain by getting {
            dependencies {
                // No native EAW API on JVM; use the generated lookup tables for getEastAsianWidth().
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
