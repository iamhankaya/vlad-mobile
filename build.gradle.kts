// Kotlin ve lint eklentileri sürümüyle burada bir kez bildirilir (alt projelerde sürümle bildirmek eklentiyi
// birden çok sınıf yükleyicisine yükler). Kotlin Android eklentisi AGP sınıflarını aynı sınıf yükleyicide görmelidir,
// bu yüzden AGP kök buildscript yoluna konur, ama yalnız Android açıkken: -PskipAndroid=true ile hiç yüklenmez.
buildscript {
    if (providers.gradleProperty("skipAndroid").orNull != "true") {
        dependencies {
            classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}
