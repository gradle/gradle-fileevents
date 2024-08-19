import gradlebuild.ZigBuild
import java.util.*

interface TargetPlatform : Named {
    val includeDirectories: ConfigurableFileCollection
    val libcFile: RegularFileProperty
    val optimizer: Property<String>
}

interface ZigExtension {
    val outputDir: DirectoryProperty
    val targets: NamedDomainObjectContainer<TargetPlatform>
}

val extension = project.extensions.create<ZigExtension>("zig")

fun String.kebabToCamelCase() = split("-").joinToString("", transform = {
    it.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
    }
})

val zigBuild by tasks.registering {
    group = "build"
    description = "Builds the project with Zig"
}

tasks.withType<ZigBuild>().configureEach {
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
            includeDirectories.from(target.includeDirectories)
            libcFile = target.libcFile
            optimizer = target.optimizer
        }
        zigBuild.configure { dependsOn(task) }
    }
}
