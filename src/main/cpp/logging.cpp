#include <iostream>

#include "logging.h"

Logging::Logging(JavaVM* jvm)
    : JniSupport(jvm)
    , clsLogger(getThreadEnv(), "org/gradle/fileevents/internal/NativeLogger")
    , logMethod(getThreadEnv()->GetStaticMethodID(clsLogger.get(), "log", "(ILjava/lang/String;)V")) {
}

void Logging::send(LogLevel level, const char* fmt, ...) {
    char buffer[1024];
    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);

    JNIEnv* env = getThreadEnv();
    if (env == NULL) {
        cerr << buffer << endl;
    } else {
        jstring logString = env->NewStringUTF(buffer);
        env->CallStaticVoidMethod(clsLogger.get(), logMethod, level, logString);
        env->DeleteLocalRef(logString);
        rethrowJavaException(env);
    }
}
