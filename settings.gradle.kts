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
    id("com.gradle.develocity").version("3.18")
//    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.10.1")
}

develocity {
    server = "https://ge.gradle.org"
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "gradle-fileevents"

enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
