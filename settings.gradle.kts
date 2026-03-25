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
    id("io.github.gradle.develocity-conventions-plugin").version("0.14.1")
}

develocity {
    server = "https://ge.gradle.org"
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        uploadInBackground = false
    }
}

rootProject.name = "gradle-fileevents"

enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
