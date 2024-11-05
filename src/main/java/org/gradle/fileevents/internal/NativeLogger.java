package org.gradle.fileevents.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// Used from native
@SuppressWarnings("unused")
public class NativeLogger {
    static final Logger LOGGER = LoggerFactory.getLogger(NativeLogger.class);

    enum LogLevel {
        TRACE(LOGGER::trace),
        DEBUG(LOGGER::debug),
        INFO(LOGGER::info),
        WARN(LOGGER::warn),
        ERROR(LOGGER::error);

        private final Consumer<String> logger;

        LogLevel(Consumer<String> logger) {
            this.logger = logger;
        }

        Consumer<String> getLogger() {
            return logger;
        }
    }

    private static final List<LogLevel> logLevels = Arrays.asList(LogLevel.values());

    public static void log(int level, String message) {
        logLevels.get(level).getLogger().accept(message);
    }
}
