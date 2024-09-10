plugins {
    `kotlin-dsl`
}

group = "gradlebuild"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.tukaani:xz:1.10")
    implementation("gradle.plugin.fr.brouillard.oss.gradle:gradle-jgitver-plugin:0.10.0-rc03")
}
