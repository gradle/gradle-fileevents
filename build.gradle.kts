import java.io.ByteArrayOutputStream

plugins {
    id("groovy")
    id("java-library")
    id("cpp")
}

// nativeVersion {
//     versionClassPackageName = "net.rubygrapefruit.platform.internal.jni"
//     versionClassName = "FileEventsVersion"
// }

val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/version")
    val outputFile = outputDir.map { it.file("net/rubygrapefruit/platform/internal/jni/FileEventsVersion.java") }

    outputs.dir(outputDir)

    doLast {
        val version = ByteArrayOutputStream().use { outputStream ->
            exec {
                commandLine("git", "describe", "--tags")
                standardOutput = outputStream
            }
            outputStream.toString().trim()
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                """
                package net.rubygrapefruit.platform.internal.jni;

                public class FileEventsVersion {
                    public static final String VERSION = "$version";
                }
                """.trimIndent()
            )
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs(generateVersionFile.map { it.outputs.files.first() })
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

tasks.withType<JavaCompile> {
    options.headerOutputDirectory = layout.buildDirectory.dir("generated/sources/headers")
}
