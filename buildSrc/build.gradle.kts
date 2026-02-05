plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.10")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.36.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.1.0")
}
