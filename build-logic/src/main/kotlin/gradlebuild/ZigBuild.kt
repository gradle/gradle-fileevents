package gradlebuild

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ZigBuild @Inject constructor(@Inject val exec: ExecOperations) : DefaultTask() {
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:LocalState
    abstract val cacheDirectory: DirectoryProperty

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val libcFile: RegularFileProperty

    @get:InputFiles
    abstract val includeDirectories: ConfigurableFileCollection

    @get:Input
    abstract val target: Property<String>

    @get:Optional
    @get:Input
    abstract val optimizer: Property<String>

    @TaskAction
    fun execute() {
        exec.exec {
            commandLine(
                "zig", "build", "build",
                "--prefix", outputDirectory.get().asFile.absolutePath,
                "--cache-dir", cacheDirectory.get().asFile.absolutePath
            )
            includeDirectories.forEach { includeDirectory ->
                args("--search-prefix", includeDirectory.absolutePath)
            }
            if (target.isPresent) {
                args("-Dtarget=${target.get()}")
            }
            if (libcFile.isPresent) {
                args("--libc", libcFile.get().asFile.absolutePath)
            }
            if (optimizer.isPresent) {
                args("-Doptimize=${optimizer.get()}")
            }
            workingDir = workingDirectory.get().asFile
        }
    }
}