@file:Suppress("UnstableApiUsage")

import gradlebuild.ZigBuild
import org.gradle.util.internal.VersionNumber

plugins {
    id("groovy")
    id("java-library")
    id("cpp")
    id("maven-publish")
    id("gradlebuild.git-version")
    id("gradlebuild.zig")
}

group = "org.gradle.fileevents"

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    api("net.rubygrapefruit:native-platform:0.22-milestone-26")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        // JDK 17 is needed for the `jni_md.h` that allows `JNIEXPORT` to be overriden
        languageVersion = JavaLanguageVersion.of(17)
    }

    // Consumers require Java 8 compatibility
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
                implementation(files(layout.buildDirectory.file("remote/gradle-fileevents.jar")))
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
    zigVersion = "0.14.0-dev.1320+492cc2ef8"
    outputDir = layout.buildDirectory.dir("zig")
    targets {
        create("x86_64-linux-gnu")
        create("aarch64-linux-gnu")
        create("x86_64-linux-musl")
        create("aarch64-linux-musl")
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
    optimizer = "ReleaseSmall"
    headers.from(compileJava.options.headerOutputDirectory)
    sources.from(git.headerOutputDir)
    headers.from(layout.projectDirectory.dir("src/main/headers"))
    sources.from(layout.projectDirectory.dir("src/main/cpp"))
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

fun toMavenVersion(gitVersion: String): Pair<String, Boolean> {
    val matcher = Regex("(.*?)(-g[0-9a-f]+)?(-dirty)?").matchEntire(gitVersion) ?: error("Invalid version: $gitVersion")
    val version = VersionNumber.parse(matcher.groupValues[1])
    // TODO Prevent publishing dirty stuff?
    // If it's not a tagged version, or if there are local changes, this is a snapshot
    val snapshot = matcher.groupValues[2].isNotEmpty() || matcher.groupValues[3].isNotEmpty()
    val mavenVersion = if (snapshot) {
        VersionNumber(version.major, version.minor + 1, 0, "SNAPSHOT").toString()
    } else {
        version.toString()
    }
    return Pair(mavenVersion, snapshot)
}

val (mavenVersion, snapshot) = toMavenVersion(git.version.get())

println("Publishing version $mavenVersion to ${if (snapshot) "snapshot" else "release"} repository (Git version: ${git.version.get()})")

publishing {
    repositories {
        maven {
            val artifactoryUrl = providers.environmentVariable("GRADLE_INTERNAL_REPO_URL").orNull
            val artifactoryUsername = providers.environmentVariable("ORG_GRADLE_PROJECT_publishUserName").orNull
            val artifactoryPassword = providers.environmentVariable("ORG_GRADLE_PROJECT_publishApiKey").orNull

            println("Artifactory URL: $artifactoryUrl")
            println("Artifactory Username: $artifactoryUsername")
            println("Artifactory Password: ${artifactoryPassword?.replace(".", "*")}")

            name = "remote"
            val libsType = if (snapshot) "snapshots" else "releases"
            url = uri("${artifactoryUrl}/libs-${libsType}-local")
            credentials {
                username = artifactoryUsername
                password = artifactoryPassword
            }
        }
    }

    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = mavenVersion
            description = project.description

            pom {
                packaging = "jar"
                // TODO Update to final GitHub URL
                url = "https://github.com/lptr/gradle-fileevents"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "The Gradle team"
                        organization = "Gradle Inc."
                        organizationUrl = "https://gradle.org"
                    }
                }
                scm {
                    // TODO Update to final GitHub URLs
                    connection = "scm:git:git://github.com/lptr/gradle-fileevents.git"
                    developerConnection = "scm:git:ssh://github.com:lptr/gradle-fileevents.git"
                    url = "https://github.com/lptr/gradle-fileevents"
                }
            }
        }
    }
}
