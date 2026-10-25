pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vlad-mobile"

include(":core")

// -PskipAndroid=true: :core saf JVM kalır, Android Gradle Plugin hiç yüklenmez (Android SDK'sız / ARM kutu).
if (providers.gradleProperty("skipAndroid").orNull != "true") {
    include(":app")
}
