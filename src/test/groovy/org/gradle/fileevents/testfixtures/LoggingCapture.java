package org.gradle.fileevents.testfixtures;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.LinkedHashMap;
import java.util.Map;

public class LoggingCapture {

    private final ch.qos.logback.classic.Logger logger;
    private final ch.qos.logback.classic.Level level;
    private ch.qos.logback.classic.Level oldLevel;
    private RecordingAppender recorder;

    public LoggingCapture(Class<?> clazz, Level level) {
        this(LoggerFactory.getLogger(clazz), level);
    }

    private LoggingCapture(Logger logger, Level level) {
        this.logger = (ch.qos.logback.classic.Logger) logger;
        this.level = ch.qos.logback.classic.Level.convertAnSLF4JLevel(level);
    }

    public void beforeTestExecution() {
        oldLevel = logger.getLevel();
        logger.setLevel(level);

        recorder = new RecordingAppender();
        logger.addAppender(recorder);
        recorder.start();
    }

    public void afterTestExecution() {
        recorder.stop();
        logger.detachAppender(recorder);
        logger.setLevel(oldLevel);
    }

    public Map<String, Level> getMessages() {
        return recorder.messages;
    }

    public void clear() {
        recorder.messages.clear();
    }

    private static class RecordingAppender extends AppenderBase<ILoggingEvent> {

        private final Map<String, Level> messages = new LinkedHashMap<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            messages.put(eventObject.getFormattedMessage(), convertLevel(eventObject.getLevel()));
        }
    }

    private static Level convertLevel(ch.qos.logback.classic.Level logbackLevel) {
        switch (logbackLevel.toInt()) {
            case ch.qos.logback.classic.Level.TRACE_INT:
                return Level.TRACE;
            case ch.qos.logback.classic.Level.DEBUG_INT:
                return Level.DEBUG;
            case ch.qos.logback.classic.Level.INFO_INT:
                return Level.INFO;
            case ch.qos.logback.classic.Level.WARN_INT:
                return Level.WARN;
            case ch.qos.logback.classic.Level.ERROR_INT:
                return Level.ERROR;
            default:
                throw new IllegalArgumentException("Unknown level: " + logbackLevel);
        }
    }
}
