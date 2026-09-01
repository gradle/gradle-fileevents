const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const java_home = b.graph.environ_map.get("JAVA_HOME") orelse @panic("JAVA_HOME is not set");
    const java_include_path = std.fmt.allocPrint(b.allocator, "{s}/include", .{java_home}) catch unreachable;
    const java_darwin_include_path = std.fmt.allocPrint(b.allocator, "{s}/include/darwin", .{java_home}) catch unreachable;

    const module = b.createModule(.{
        .target = target,
        .optimize = optimize,
        // Link against libc and libstdc++
        .link_libc = true,
        .link_libcpp = true,
    });

    // Add include directories
    module.addIncludePath(b.path("build/generated/sources/headers/java"));
    module.addIncludePath(b.path("build/generated/sources/headers/version"));
    module.addIncludePath(b.path("src/main/headers"));
    module.addSystemIncludePath(.{ .cwd_relative = java_include_path });
    module.addSystemIncludePath(.{ .cwd_relative = java_darwin_include_path });

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
        // `std::wstring_convert` is deprecated in C++17 without a standard replacement, and the
        // in-source pragma in jni_support.cpp doesn't cover the warning libc++ raises from inside
        // its own headers while instantiating the template.
        "-Wno-deprecated-declarations",
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
    module.addCSourceFiles(.{
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

    if (target.result.os.tag == .macos) {
        module.linkFramework("CoreFoundation", .{});
        module.linkFramework("CoreServices", .{});
        module.addSystemFrameworkPath(.{ .cwd_relative = "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks" });
    }

    const lib = b.addLibrary(.{
        .name = "gradle-fileevents",
        .linkage = .dynamic,
        .root_module = module,
    });

    // lib.verbose_cc = true;
    // lib.verbose_link = true;

    const install = b.addInstallArtifact(lib, .{
        .dest_dir = .{ .override = .{ .custom = "out" } },
    });

    // Ensure the library is built
    const build_step = b.step("build", "Build the file events shared library");
    build_step.dependOn(&install.step);
}
