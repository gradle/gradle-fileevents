pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.gradle.org/artifactory/libs-releases")
    }
}

plugins {
    id("com.gradle.develocity").version("4.3.2")
    id("io.github.gradle.develocity-conventions-plugin").version("0.16.0")
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

develocity {
    buildScan {
        uploadInBackground = false
    }
}

rootProject.name = "gradle-fileevents"

enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
