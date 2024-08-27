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
                commandLine("git", "describe", "--tags", "--dirty")
                standardOutput = outputStream
                workingDir = parameters.rootDir.get().asFile
            }
            outputStream.toString().trim()
        }
    }
}

abstract class GitVersionExtension(val version: Provider<String>) {
    abstract val javaOutputDir: DirectoryProperty
    abstract val headerOutputDir: DirectoryProperty
}

val gitVersion = providers.of(GitVersionValueSource::class.java) {
    parameters.rootDir = project.rootDir
}

val extension = project.extensions.create<GitVersionExtension>("git", gitVersion)

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    version = extension.version
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java/version")
    headerOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

extension.javaOutputDir = generateVersionFile.flatMap { it.javaOutputDir }
extension.headerOutputDir = generateVersionFile.flatMap { it.headerOutputDir }
