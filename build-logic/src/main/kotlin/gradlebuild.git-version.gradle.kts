import gradlebuild.GenerateVersions

plugins {
    id("fr.brouillard.oss.gradle.jgitver")
}

jgitver {
    nonQualifierBranches("main")
}

abstract class GitVersionExtension(
    val javaOutputDir: Provider<Directory>,
    val headerOutputDir: Provider<Directory>,
)

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    // Project version is set by the jgitver plugin
    version = providers.provider { project.version.toString() }
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java/version")
    headerOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

project.extensions.create<GitVersionExtension>(
    "git",
    generateVersionFile.flatMap { it.javaOutputDir },
    generateVersionFile.flatMap { it.headerOutputDir },
)
