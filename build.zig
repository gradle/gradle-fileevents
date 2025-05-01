const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const lib = b.addSharedLibrary(.{ .name = "gradle-fileevents", .target = target, .optimize = optimize });

    const env = std.process.getEnvMap(b.allocator) catch unreachable;
    const java_home = env.get("JAVA_HOME") orelse unreachable;
    const java_include_path = std.fmt.allocPrint(b.allocator, "{s}/include", .{java_home}) catch unreachable;
    const java_darwin_include_path = std.fmt.allocPrint(b.allocator, "{s}/include/darwin", .{java_home}) catch unreachable;

    // Add include directories
    lib.addIncludePath(b.path("build/generated/sources/headers/java"));
    lib.addIncludePath(b.path("build/generated/sources/headers/version"));
    lib.addIncludePath(b.path("src/main/headers"));
    lib.addSystemIncludePath(.{ .cwd_relative = java_include_path });
    lib.addSystemIncludePath(.{ .cwd_relative = java_darwin_include_path });

    const base_cpp_args = &[_][]const u8{
        "--std=c++17",
        "-g",
        "-pedantic",
        "-Wall",
        "-Wextra",
        "-Wformat=2",
        "-Werror",
        "-Wno-format-nonliteral",
        "-Wno-unguarded-availability-new",
    };

    const cpp_args = if (target.result.os.tag == .windows)
        base_cpp_args ++ &[_][]const u8{
            "-DNTDDI_VERSION=NTDDI_WIN10_RS3",
            // Need this to actually get our functions in the export table
            "-DJNIEXPORT=__declspec(dllexport)",
        }
    else
        base_cpp_args;

    // Add source files
    lib.addCSourceFiles(.{
        .files = &.{
            "src/main/cpp/apple_fsnotifier.cpp",
            "src/main/cpp/fileevents_version.cpp",
            "src/main/cpp/generic_fsnotifier.cpp",
            "src/main/cpp/jni_support.cpp",
            "src/main/cpp/linux_fsnotifier.cpp",
            "src/main/cpp/logging.cpp",
            "src/main/cpp/services.cpp",
            "src/main/cpp/win_fsnotifier.cpp",
        },
        .flags = cpp_args,
    });

    // Link against libc and libstdc++
    lib.linkLibCpp();

    // Link libc unless on Linux; this preserves compatibility with Ubunty 16.04
    if (target.result.os.tag != .linux) {
        // We don't link libc
        lib.linkLibC();
    }

    if (target.result.os.tag == .macos) {
        lib.linkFramework("CoreFoundation");
        lib.linkFramework("CoreServices");
        lib.addSystemFrameworkPath(.{ .cwd_relative = "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks" });
    }

    // lib.verbose_cc = true;
    // lib.verbose_link = true;

    const install = b.addInstallArtifact(lib, .{
        .dest_dir = .{ .override = .{ .custom = "out" } },
    });

    // Ensure the library is built
    const build_step = b.step("build", "Build the file events shared library");
    build_step.dependOn(&install.step);
}
