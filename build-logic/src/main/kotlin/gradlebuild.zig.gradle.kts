import gradlebuild.ZigBuild
import java.util.*

tasks.withType<ZigBuild>().configureEach {
    workingDirectory = layout.projectDirectory
    outputDirectory = layout.buildDirectory.dir("zig")
    cacheDirectory = layout.buildDirectory.dir(".zig-cache")
}

interface ZigExtension {
    val targets: SetProperty<String>
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

afterEvaluate {
    extension.targets.get().forEach { target ->
        val task = tasks.register<ZigBuild>("zigBuild${target.kebabToCamelCase()}") {
            group = "zig"
            description = "Builds the project with Zig for $target"
            if (target != "native") {
                this.target = target
            }
        }
        zigBuild.configure { dependsOn(task) }
    }
}
