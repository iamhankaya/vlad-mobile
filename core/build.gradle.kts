plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.detekt)
}

// Android tarafı Java 17 bayt kodu bekler; yerel/CI JDK 17+ olabilir, hedef sabit.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}

tasks.test {
    useJUnitPlatform()
    // Sözleşme dosyaları (imza vektörleri, API fixture'ları) repo kökündeki contract/ dizininden okunur.
    systemProperty("vlad.contract.dir", rootProject.file("contract").absolutePath)
    inputs.dir(rootProject.file("contract"))
}
