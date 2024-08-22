/**
 * This is a workaround cross-platform variant of the jni_md.h file from the JDK.
 */
#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

// For Windows we have to use __declspec(dllexport) and __declspec(dllimport) to export/import symbols.
#if (defined(__MINGW32__) || defined(__MINGW64__))

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall

#else

#ifndef __has_attribute
  #define __has_attribute(x) 0
#endif
#if (defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4) && (__GNUC_MINOR__ > 2))) || __has_attribute(visibility)
  #ifdef ARM
    #define JNIEXPORT     __attribute__((externally_visible,visibility("default")))
    #define JNIIMPORT     __attribute__((externally_visible,visibility("default")))
  #else
    #define JNIEXPORT     __attribute__((visibility("default")))
    #define JNIIMPORT     __attribute__((visibility("default")))
  #endif
#else
  #define JNIEXPORT
  #define JNIIMPORT
#endif

#define JNICALL

#endif

typedef int jint;
#ifdef _LP64
typedef long jlong;
#else
typedef long long jlong;
#endif

typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
