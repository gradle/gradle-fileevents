import gradlebuild.GenerateVersions
import java.io.ByteArrayOutputStream

abstract class GitVersionValueSource @Inject constructor(val exec: ExecOperations) :
    ValueSource<String, GitVersionValueSource.Params> {
        interface Params : ValueSourceParameters {
            val rootDir: DirectoryProperty
        }

    override fun obtain(): String? {
        return ByteArrayOutputStream().use { outputStream ->
            exec.exec {
                commandLine("git", "describe", "--tags")
                standardOutput = outputStream
                workingDir = parameters.rootDir.get().asFile
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
extension.version = providers.of(GitVersionValueSource::class.java) {
    parameters.rootDir = project.rootDir
}

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    version = extension.version
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java/version")
    headerOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

extension.javaOutputDir = generateVersionFile.flatMap { it.javaOutputDir }
extension.headerOutputDir = generateVersionFile.flatMap { it.headerOutputDir }
