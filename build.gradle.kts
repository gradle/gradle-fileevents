@file:Suppress("UnstableApiUsage")

import gradlebuild.ZigBuild

plugins {
    id("groovy")
    id("java-library")
    id("cpp")
    id("gradlebuild.git-version")
    id("gradlebuild.zig")
}

group = "org.gradle.file-events"

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

// Define a custom configuration that only includes the test sources
val testOnlyClasspath by configurations.creating {
    // This configuration extends from 'testImplementation'
    extendsFrom(configurations.testImplementation.get())

    // Exclude the main source set classes
    exclude(group = project.group.toString(), module = project.name)
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Spock test framework
            useSpock("2.2-groovy-3.0")
        }

        // Test suite for running against a remotely built artifact
        val externalTest by registering(JvmTestSuite::class) {
            useSpock("2.2-groovy-3.0")

            // Use the same source directories as the main test suite
            sources {
                groovy {
                    setSrcDirs(listOf("src/test/groovy"))
                }
                resources {
                    setSrcDirs(listOf("src/test/resources"))
                }
            }

            // Configure the dependencies
            dependencies {
                // Use the custom configuration that includes only the test dependencies
                implementation(testOnlyClasspath)

                // Add the external JAR as a dependency
                implementation(files(layout.buildDirectory.file("remote/file-events.jar")))
            }
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

sourceSets {
    main {
        java {
            srcDirs(git.javaOutputDir)
        }
    }
}

zig {
    outputDir = layout.buildDirectory.dir("zig")
    targets {
        create("x86_64-linux-gnu")
        create("aarch64-linux-gnu")
        create("x86_64-windows-gnu")
        create("aarch64-windows-gnu")
        create("x86_64-macos") {
            libcFile = layout.projectDirectory.file("libc-macos.txt")
        }
        create("aarch64-macos") {
            libcFile = layout.projectDirectory.file("libc-macos.txt")
        }
    }
}

zig.targets.configureEach {
    includeDirectories.from(compileJava.options.headerOutputDirectory)
    includeDirectories.from(git.headerOutputDir)
    optimizer = "ReleaseSmall"
}

tasks.withType<ZigBuild>().all {
    val zigBuild = this
    tasks.named<ProcessResources>("processResources") {
        into("net/rubygrapefruit/platform") {
            into(zigBuild.target) {
                from(zigBuild.outputDirectory.dir("out"))
            }
        }
    }
}
