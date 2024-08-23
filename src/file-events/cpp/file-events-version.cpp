#include "file_events_version.h"
#include "org_gradle_fileevents_internal_AbstractNativeFileEventFunctions.h"

JNIEXPORT jstring JNICALL
Java_org_gradle_fileevents_internal_AbstractNativeFileEventFunctions_getVersion0(JNIEnv* env, jclass) {
    return env->NewStringUTF(FILE_EVENTS_VERSION);
}
