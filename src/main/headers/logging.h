#pragma once

#include <jni.h>

#include "jni_support.h"

#define LOG_LEVEL_CHECK_INTERVAL_IN_MS 1000

enum class LogLevel : int {
    TRACE_LEVEL,
    DEBUG_LEVEL,
    INFO_LEVEL,
    WARN_LEVEL,
    ERROR_LEVEL,
};

class Logging : public JniSupport {
public:
    Logging(JavaVM* jvm);

    void send(LogLevel level, const char* fmt, ...);

private:
    const JClass clsLogger;
    const jmethodID logMethod;
};

extern Logging* logging;

#define logToJava(level, message, ...) logging->send(level, message, __VA_ARGS__)
