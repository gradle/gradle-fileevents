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
        // installDir will route to "\gradle-fileevents\build\zig-install\unpack"
        val installDir = this.installDir.get().asFile
        var installedVersion = ""
        // gets the full executable file path
        val execFile = this.installDir.asFile.get().zigExecutablePath(zigVersion.get())

        try {
            val outputStream = ByteArrayOutputStream()
            val result = exec.exec {
                commandLine = listOf(execFile.path, "version")
                standardOutput = outputStream
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) {
                installedVersion = outputStream.toString().trim()
                println("Found Zig version : ${installedVersion}")
                if (installedVersion != zigVersion.get()) {
                    // if version mismatch delete the unnecessary installations.
                    installDir.deleteRecursively()
                    return
                }
            }
        } catch (e: Exception) {
            println("Error: Zig ${zigVersion.get()} was not found at ${installDir}.")
            println("Exception Cause : ${e.message}")
            println("Going to attempt to install ZigLang. \n")
        }

        // Install Zig
        if (installedVersion.isNullOrEmpty()){
            println("Installing Zig ${zigVersion.get()} for ${os()} ${arch()}.")
        }

        val cacheRoot = cacheDir.get().asFile
        cacheRoot.mkdirs()

        // Set Remote Repository Url
        val baseUrl = "https://repo.gradle.org/ui/native/ziglang/" // https://ziglang.org/
        var zigArchive: File
        var zigExtracted: File

        // Check for windows
        if (os() == "windows"){
            zigArchive = cacheRoot.resolve("${zigName(zigVersion.get())}.zip")
            if (!zigArchive.exists()){
                println("Downloading Zig Zip File To: " + cacheRoot.path)

                if (zigVersion.get().contains('-')) {
                    // TODO for dev builds
                    downloadFile("${baseUrl}builds/${zigName(zigVersion.get())}.zip", zigArchive)
                } else {
                    downloadFile("${baseUrl}download/${zigVersion.get()}/${zigName(zigVersion.get())}.zip", zigArchive)
                }
            }
            // Check to see if the zip file has already been extracted
            try {
                zigExtracted = installDir.resolve(installDir.absolutePath + "\\" + zigName(zigVersion.get()))

                if ( !zigExtracted.exists() ){
                    println("Extracting Zip To: " + zigExtracted.path)
                    // Compatible until 9.5.x. Doesn't work in 9.6.x but will work in 9.7+ until 10
                    //unzipTo(installDir, zigArchive)
                    // Public API replacement using the Project instance: compatible with all versions
                    project.copy {
                        from(project.zipTree(zigArchive))
                        into(installDir)
                    }
                } else {
                    println("Extracted files already exist. Skipped Un-ziping archive.")
                }
            } catch (e : Exception){
                println("Error : ZigInstall.kt failed to unzip the downloaded zip file." + "\n" + e.toString())
            }
            println("ExecutablePath : " + installDir.zigExecutablePath(zigVersion.get()))
        }
        // Check for Linux
        if (os() == "linux" || os() == "macos"){
            zigArchive = cacheRoot.resolve("${zigName(zigVersion.get())}.tar.xz")
            if (!zigArchive.exists()){
                if (zigVersion.get().contains('-')) {
                    downloadFile("${baseUrl}builds/${zigName(zigVersion.get())}.tar.xz", zigArchive)
                } else {
                    downloadFile("${baseUrl}download/${zigVersion.get()}/${zigName(zigVersion.get())}.tar.xz", zigArchive)
                }
            }
            // Check to see if the zip file has already been extracted
            try {
                zigExtracted = installDir.resolve(installDir.absolutePath + zigArchive.absolutePath)

                if ( !zigExtracted.exists() ){
                    unpackTarXz(zigArchive, installDir)
                    println("Extracting Zip To: " + zigExtracted.path)
                }
                else {
                    println("Extracted files already exist. Skipped Un-taring tar file.")
                }
            }  catch (e : Exception){
                println("Error: ZigInstall.kt failed to untar the downloaded tar file." + "\n" + e.toString())
            }
        }
        // get zig executable file path
        val executable = installDir.zigExecutablePath(zigVersion.get())
        executable.setExecutable(true, false)
    }

    // Downloads the file
    fun downloadFile(url: String, destination: File) {
        URI(url).toURL().openStream().use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    // Unpacks the Tar.Xz file for linux and macOS
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

    // Gets the executable file path value
    private fun File.zigExecutablePath(version: String): File {
        // On Windows, Need to return  with the extension otherwise it won't run.
        if (os() == "windows"){
            //return resolve("${zigName(version)}" + "/zig.exe")
            return resolve("${zigName(version)}/zig.exe")
        }
        return resolve("${zigName(version)}/zig")
    }

    // Sets the name scheme for the archive name to download from remote repository
    private fun zigName(zigVersion: String): String {
        var list = zigVersion.split(".", ignoreCase = true)
        var versionString = list[1]
        var version = versionString.toInt()

        // The older versions (0.14.0) name scheme have the OS before the Architecture
        // https://ziglang.org/download/0.8.1/zig-windows-x86_64-0.8.1.zip
        // https://ziglang.org/download/0.15.1/zig-x86_64-windows-0.15.1
        // https://repo.gradle.org/artifactory/ziglang/download/zig-x86-windows-0.15.1.zip
        // https://repo.gradle.org/artifactory/ziglang/download/zig-windows-x86-0.13.0.zip
        if (version < 14){
            return "zig-${os()}-${arch()}-${zigVersion}"
        }
        return "zig-${arch()}-${os()}-${zigVersion}"
    }

    // Identifies the Operating System
    private fun os(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("mac") -> "macos"
            os.contains("win") -> "windows"
            os.contains("linux") -> "linux"
            else -> error("Unsupported OS: $os")
        }
    }

    // Identifies the System Architecture
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
