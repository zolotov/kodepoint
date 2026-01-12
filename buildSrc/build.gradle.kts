plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
}
