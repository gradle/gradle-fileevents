package gradlebuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateVersions : DefaultTask() {
    @get:Input
    abstract val version: Property<String>
    @get:OutputDirectory
    abstract val javaOutputDir: DirectoryProperty
    @get:OutputDirectory
    abstract val headerOutputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val javaFile =
            javaOutputDir.file("org/gradle/fileevents/internal/FileEventsVersion.java").get().asFile
        javaFile.parentFile.mkdirs()
        javaFile.writeText(
            """
            package org.gradle.fileevents.internal;

            public class FileEventsVersion {
                public static final String VERSION = "${version.get()}";
            }
            """.trimIndent()
        )

        val cFile = headerOutputDir.file("file_events_version.h").get().asFile
        cFile.parentFile.mkdirs()
        cFile.writeText(
            """
            #define FILE_EVENTS_VERSION "${version.get()}"
            """.trimIndent()
        )
    }
}
