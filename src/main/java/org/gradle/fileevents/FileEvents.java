package org.gradle.fileevents;

import net.rubygrapefruit.platform.NativeException;
import net.rubygrapefruit.platform.NativeIntegration;
import net.rubygrapefruit.platform.NativeIntegrationUnavailableException;
import net.rubygrapefruit.platform.ThreadSafe;
import net.rubygrapefruit.platform.internal.LibraryDef;
import net.rubygrapefruit.platform.internal.NativeLibraryLocator;
import net.rubygrapefruit.platform.internal.Platform;
import org.gradle.fileevents.internal.AbstractNativeFileEventFunctions;
import org.gradle.fileevents.internal.FileEventsVersion;
import org.gradle.fileevents.internal.LinuxFileEventFunctions;
import org.gradle.fileevents.internal.OsxFileEventFunctions;
import org.gradle.fileevents.internal.WindowsFileEventFunctions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class FileEvents {
    private static final Map<Class<?>, Object> integrations = new HashMap<>();
    private static boolean initialized;

    @ThreadSafe
    static public void init(File extractDir) throws NativeException {
        synchronized (FileEvents.class) {
            if (initialized) {
                return;
            }
            Platform platform = Platform.current();
            String platformName = getPlatformName(platform);
            try {
                NativeLibraryLocator loader = new NativeLibraryLocator(extractDir, FileEventsVersion.VERSION);
                File library = loader.find(new LibraryDef(determineLibraryName(platform), platformName));
                if (library == null) {
                    throw new NativeIntegrationUnavailableException(String.format("Native file events integration is not available for %s.", platform));
                }
                System.load(library.getCanonicalPath());
                initialized = true;

                String nativeVersion = AbstractNativeFileEventFunctions.getVersion();
                if (!nativeVersion.equals(FileEventsVersion.VERSION)) {
                    throw new NativeException(String.format(
                        "Unexpected file events library version loaded. Expected %s, was %s.",
                        FileEventsVersion.VERSION,
                        nativeVersion
                    ));
                }
            } catch (NativeException e) {
                throw e;
            } catch (Throwable t) {
                throw new NativeException("Failed to initialise native integration.", t);
            }
        }
    }

    private static String getPlatformName(Platform platform) {
        switch (platform.getId()) {
            case "windows-i386":
                return "i386-windows-gnu";
            case "windows-amd64":
                return "x86_64-windows-gnu";
            case "windows-aarch64":
                return "aarch64-windows-gnu";
            case "linux-i386":
                return "i386-linux-" + getLinuxVariant();
            case "linux-amd64":
                return "x86_64-linux-" + getLinuxVariant();
            case "linux-aarch64":
                return "aarch64-linux-" + getLinuxVariant();
            case "osx-amd64":
                return "x86_64-macos";
            case "osx-aarch64":
                return "aarch64-macos";
            default:
                throw new NativeIntegrationUnavailableException(String.format("Native file events integration is not available for %s.", platform));
        }
    }

    private static String getLinuxVariant() {
        return isLinuxWithMusl() ? "musl" : "gnu";
    }

    /**
     * Our native libraries don't currently support musl libc.
     * See <a href="https://github.com/gradle/gradle/issues/24875">#24875</a>.
     */
    private static boolean isLinuxWithMusl() {
        // Musl libc maps /lib/ld-musl-aarch64.so.1 into memory, let's try to find it
        try {
            File mapFilesDir = new File("/proc/self/map_files");
            if (!mapFilesDir.isDirectory()) {
                return false;
            }
            File[] files = mapFilesDir.listFiles();
            if (files == null) {
                return false;
            }
            for (File file : files) {
                if (file.getCanonicalFile().getName().contains("-musl-")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignored
        }

        return false;
    }

    /**
     * Locates a native integration of the given type.
     *
     * @return The native integration. Never returns null.
     * @throws NativeIntegrationUnavailableException When the given native integration is not available on the current
     *                                               machine.
     * @throws NativeException                       On failure to load the native integration.
     */
    @ThreadSafe
    public static <T extends NativeIntegration> T get(Class<T> type)
        throws NativeIntegrationUnavailableException, NativeException {
        synchronized (FileEvents.class) {
            if (!initialized) {
                throw new NativeException(String.format("File-system watching native library has not been initialized.", type.getSimpleName()));
            }
            Platform platform = Platform.current();
            Object instance = integrations.get(type);
            if (instance == null) {
                try {
                    instance = getEventFunctions(type, platform);
                } catch (NativeException e) {
                    throw e;
                } catch (Throwable t) {
                    throw new NativeException(String.format("Failed to load native integration %s.", type.getSimpleName()), t);
                }
                integrations.put(type, instance);
            }
            return type.cast(instance);
        }
    }

    private static <T extends NativeIntegration> T getEventFunctions(Class<T> type, Platform platform) {
        if (platform.isWindows() && type.equals(WindowsFileEventFunctions.class)) {
            return type.cast(new WindowsFileEventFunctions());
        }
        if (platform.isLinux() && type.equals(LinuxFileEventFunctions.class)) {
            return type.cast(new LinuxFileEventFunctions());
        }
        if (platform.isMacOs() && type.equals(OsxFileEventFunctions.class)) {
            return type.cast(new OsxFileEventFunctions());
        }
        throw new NativeIntegrationUnavailableException(String.format(
            "Native integration %s is not supported for %s.",
            type.getSimpleName(), platform.toString())
        );
    }

    private static String determineLibraryName(Platform platform) {
        if (platform.isLinux()) {
            return "libgradle-fileevents.so";
        }
        if (platform.isMacOs()) {
            return "libgradle-fileevents.dylib";
        }
        if (platform.isWindows()) {
            return "gradle-fileevents.dll";
        }
        throw new NativeIntegrationUnavailableException(String.format("Native file events integration is not available for %s.", platform));
    }
}
