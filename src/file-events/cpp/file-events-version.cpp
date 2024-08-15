#include "file_events_version.h"
#include "net_rubygrapefruit_platform_internal_jni_AbstractNativeFileEventFunctions.h"

JNIEXPORT jstring JNICALL
Java_net_rubygrapefruit_platform_internal_jni_AbstractNativeFileEventFunctions_getVersion0(JNIEnv* env, jclass) {
    return env->NewStringUTF(FILE_EVENTS_VERSION);
}
