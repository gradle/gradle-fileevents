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
}

val extension = project.extensions.create<GitVersionExtension>("git")
extension.version = providers.of(GitVersionValueSource::class.java) {}
