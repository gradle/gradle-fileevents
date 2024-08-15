plugins {
    id("groovy")
    id("java-library")
    id("cpp")
    id("gradlebuild.git-version")
}

abstract class GenerateVersions : DefaultTask() {
    @get:Input
    abstract val version: Property<String>
    @get:OutputDirectory
    abstract val javaOutputDir: DirectoryProperty
    @get:OutputDirectory
    abstract val cOutputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val javaFile =
            javaOutputDir.file("net/rubygrapefruit/platform/internal/jni/FileEventsVersion.java").get().asFile
        javaFile.parentFile.mkdirs()
        javaFile.writeText(
            """
            package net.rubygrapefruit.platform.internal.jni;

            public class FileEventsVersion {
                public static final String VERSION = "${version.get()}";
            }
            """.trimIndent()
        )

        val cFile = cOutputDir.file("file_events_version.h").get().asFile
        cFile.parentFile.mkdirs()
        cFile.writeText(
            """
            #define FILE_EVENTS_VERSION "${version.get()}"
            """.trimIndent()
        )
    }
}

val generateVersionFile by tasks.registering(GenerateVersions::class) {
    version = git.version
    javaOutputDir = layout.buildDirectory.dir("generated/sources/java")
    cOutputDir = layout.buildDirectory.dir("generated/sources/headers/version")
}

sourceSets {
    main {
        java {
            srcDirs(generateVersionFile.flatMap { it.javaOutputDir })
        }
    }
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    api("net.rubygrapefruit:native-platform:0.22-milestone-26")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Spock test framework
            useSpock("2.2-groovy-3.0")
        }
    }
}

// Apply
tasks.javadoc {
    exclude("**/internal/**")
}

val compileJava by tasks.named("compileJava", JavaCompile::class) {
    options.headerOutputDirectory = layout.buildDirectory.dir("generated/sources/headers/java")
}

val zigBuild by tasks.registering {
    group = "build"
    description = "Build the native library using Zig"

    val outputDir = layout.buildDirectory.dir("zig")
    inputs.files(compileJava)
    inputs.files(generateVersionFile)
    inputs.files(fileTree("src/main/zig"))
    outputs.dir(outputDir)
    doLast {
        exec {
            commandLine("zig", "build", "build", "--prefix", outputDir.get().asFile.absolutePath)
        }
    }
}
