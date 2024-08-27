import gradlebuild.ZigBuild
import gradlebuild.ZigInstall
import java.util.*

interface TargetPlatform : Named {
    val headers: ConfigurableFileCollection
    val sources: ConfigurableFileCollection
    val libcFile: RegularFileProperty
    val optimizer: Property<String>
}

interface ZigExtension {
    val zigVersion: Property<String>
    val outputDir: DirectoryProperty
    val targets: NamedDomainObjectContainer<TargetPlatform>
}

val extension = project.extensions.create<ZigExtension>("zig")

fun String.kebabToCamelCase() = split("-").joinToString("", transform = {
    it.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }
})

val zigInstall by tasks.registering(ZigInstall::class) {
    group = "build"
    description = "Installs Zig unless the right version is present on the path already"
    zigVersion.convention(extension.zigVersion)
    installDir.convention(layout.buildDirectory.dir("zig-install/unpack"))
    cacheDir.convention(layout.buildDirectory.dir("zig-install/cache"))
}

val zigBuild by tasks.registering {
    group = "build"
    description = "Builds the project with Zig"
}

tasks.withType<ZigBuild>().configureEach {
    executablePath = zigInstall.flatMap { it.executablePath }
    workingDirectory = layout.projectDirectory
    target.convention("native")
    outputDirectory = extension.outputDir.dir(target)
    cacheDirectory = layout.buildDirectory.dir(".zig-cache")
}

afterEvaluate {
    extension.targets.configureEach {
        val target = this
        val task = tasks.register<ZigBuild>("zigBuild${target.name.kebabToCamelCase()}") {
            group = "zig"
            description = "Builds the project with Zig for $target"
            if (target.name != "native") {
                this.target = target.name
            }
            headers.from(target.headers)
            sources.from(target.sources)
            libcFile = target.libcFile
            optimizer = target.optimizer
        }
        zigBuild.configure { dependsOn(task) }
    }
}
