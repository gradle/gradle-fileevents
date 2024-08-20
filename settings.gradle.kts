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
    id("com.gradle.develocity").version("3.17.5")
//    id("io.github.gradle.gradle-enterprise-conventions-plugin").version("0.10.1")
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

rootProject.name = "file-events"

enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
