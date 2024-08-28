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

abstract class GitVersionExtension(
    val version: Provider<String>,
    val javaOutputDir: Provider<Directory>,
    val headerOutputDir: Provider<Directory>,
)

val gitVersion = providers.of(GitVersionValueSource::class.java) {
    parameters.rootDir = project.rootDir
}

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    version = gitVersion
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java/version")
    headerOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

project.extensions.create<GitVersionExtension>(
    "git",
    gitVersion,
    generateVersionFile.flatMap { it.javaOutputDir },
    generateVersionFile.flatMap { it.headerOutputDir },
)
