package org.gradle.fileevents.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Used from native
@SuppressWarnings("unused")
public class NativeLogger {
    static final Logger LOGGER = LoggerFactory.getLogger(NativeLogger.class);

    enum LogLevel {
        TRACE(LOGGER::trace, LOGGER::isTraceEnabled),
        DEBUG(LOGGER::debug, LOGGER::isDebugEnabled),
        INFO(LOGGER::info, LOGGER::isInfoEnabled),
        WARN(LOGGER::warn, LOGGER::isWarnEnabled),
        ERROR(LOGGER::error, LOGGER::isErrorEnabled),
        OFF(__ -> {}, () -> true);

        private final Consumer<String> logger;
        private final Supplier<Boolean> isEnabled;

        LogLevel(Consumer<String> logger, Supplier<Boolean> isEnabled) {
            this.logger = logger;
            this.isEnabled = isEnabled;
        }

        Consumer<String> getLogger() {
            return logger;
        }

        boolean isEnabled() {
            return isEnabled.get();
        }
    }

    private static final List<LogLevel> logLevels = Arrays.asList(LogLevel.values());

    public static void log(int level, String message) {
        logLevels.get(level).getLogger().accept(message);
    }

    public static int getLogLevel() {
        return Arrays.stream(LogLevel.values())
            .filter(LogLevel::isEnabled)
            .findFirst()
            .map(LogLevel::ordinal)
            .orElseThrow(() -> new AssertionError("Effective log level is not set"));
    }
}
