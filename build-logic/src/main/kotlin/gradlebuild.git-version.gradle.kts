import gradlebuild.GenerateVersions
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitVersionValueSource @Inject constructor(val exec: ExecOperations) :
    ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String? {
        return ByteArrayOutputStream().use { outputStream ->
            exec.exec {
                commandLine("git", "describe", "--tags")
                standardOutput = outputStream
            }
            outputStream.toString().trim()
        }
    }
}

interface GitVersionExtension {
    val version: Property<String>
    val javaOutputDir: DirectoryProperty
    val headerOutputDir: DirectoryProperty
}

val extension = project.extensions.create<GitVersionExtension>("git")
extension.version = providers.of(GitVersionValueSource::class.java) {}

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    version = extension.version
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java/version")
    headerOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

extension.javaOutputDir = generateVersionFile.flatMap { it.javaOutputDir }
extension.headerOutputDir = generateVersionFile.flatMap { it.headerOutputDir }
