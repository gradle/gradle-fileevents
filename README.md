# Gradle File Events Library

[![Build Gradle project](https://github.com/gradle/gradle-fileevents/actions/workflows/build.yaml/badge.svg)](https://github.com/gradle/gradle-fileevents/actions/workflows/build.yaml)

A cross-platform library to watch for changes on the file-system.

### Supported platforms

- macOS – `x86_64` (tested on 13 and 14) and `aarch64` (untested)
- Linux – `x86_64`(tested on Ubuntu 20.04 and 24.04) and `aarch64` (untested)
- Windows – `x86_64` (tested on Windows 2019 and 2022) and `aarch64` (untested)

## Building

The project uses [Gradle](https://gradle.org/) to build the final library. Under the hood it
utilizes [Zig](https://ziglang.org/) to cross-compile the C/C++ code.

### Prerequisites

- [JDK 17](https://adoptopenjdk.net/)

The project currently requires macOS to link to the SDK. It also depends on JDK to be specifically for Darwin. Both of
these limitations should be removable with some legwork in the future.

### Running the build

```shell
./gradlew assemble
```

This should produce a `build/libs/gradle-fileevents.jar` file that contains everything.

### Testing

To run tests with the library built locally:

```shell
./gradlew test
```

To run tests with the library built on another machine, place the JAR in `build/remote/gradle-fileevents.jar`:

```shell
./gradlew externalTest
```

### Releasing

Add an annotated tag, such as:

```shell
git tag -a 0.2.6
```

Then run https://builds.gradle.org/buildConfiguration/Gradle_Fileevents_Publish with the tag.

Make sure to update GitHub's release page.
