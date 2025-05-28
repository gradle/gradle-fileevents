package gradlebuild

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import javax.inject.Inject

abstract class ZigInstall @Inject constructor(@Inject val exec: ExecOperations) : DefaultTask() {
    @get:Input
    abstract val zigVersion: Property<String>

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @get:LocalState
    abstract val cacheDir: DirectoryProperty

    @get:Internal
    val executablePath: Provider<String>
        get() = installDir.zip(zigVersion) { installDir, zigVersion ->
            val executable = installDir.asFile.zigExecutablePath(zigVersion)
            if (executable.isFile) {
                executable.absolutePath
            } else {
                "zig"
            }
        }

    @TaskAction
    fun execute() {
        val installDir = this.installDir.get().asFile
        try {
            val outputStream = ByteArrayOutputStream()
            val result = exec.exec {
                commandLine = listOf("zig", "version")
                standardOutput = outputStream
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                val installedVersion = outputStream.toString().trim()
                println("Found Zig ${installedVersion}")
                if (installedVersion == zigVersion.get()) {
                    installDir.deleteRecursively()
                    return
                }
            }
        } catch (e: Exception) {
            println("Zig not found on path")
        }

        // Install Zig
        println("Installing Zig ${zigVersion.get()} for ${os()} ${arch()}")
        val cacheRoot = cacheDir.get().asFile
        cacheRoot.mkdirs()
        val zigArchive = cacheRoot.resolve("${zigName(zigVersion.get())}.tar.xz")
        val baseUrl = "https://repo.gradle.org/artifactory/ziglang/" // https://ziglang.org/
        // TODO Figure out OS and architecture
        if (zigVersion.get().contains('-')) {
          downloadFile("${baseUrl}builds/${zigName(zigVersion.get())}.tar.xz", zigArchive)
        } else {
          downloadFile("${baseUrl}download/${zigVersion.get()}/${zigName(zigVersion.get())}.tar.xz", zigArchive)
        }
        unpackTarXz(zigArchive, installDir)
        val executable = installDir.zigExecutablePath(zigVersion.get())
        executable.setExecutable(true, false)
    }

    fun downloadFile(url: String, destination: File) {
        URI(url).toURL().openStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun unpackTarXz(file: File, outputDir: File) {
        XZCompressorInputStream(BufferedInputStream(FileInputStream(file))).use { xzIn ->
            TarArchiveInputStream(xzIn).use { tarIn ->
                var entry: TarArchiveEntry? = tarIn.nextEntry
                while (entry != null) {
                    val outputFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile.mkdirs() // Create parent directories if they don't exist
                        outputFile.outputStream().use { output ->
                            tarIn.copyTo(output)
                        }
                    }
                    entry = tarIn.nextEntry
                }
            }
        }
    }

    private fun File.zigExecutablePath(version: String): File =
        resolve("${zigName(version)}/zig")

    private fun zigName(zigVersion: String) =
        "zig-${arch()}-${os()}-${zigVersion}"

    private fun os(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> "macos"
            os.contains("win") -> "windows"
            os.contains("linux") -> "linux"
            else -> error("Unsupported OS: $os")
        }
    }

    private fun arch(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            arch.contains("x86_64") -> "x86_64"
            arch.contains("amd64") -> "x86_64"
            arch.contains("aarch64") -> "aarch64"
            else -> error("Unsupported architecture: $arch")
        }
    }
}
