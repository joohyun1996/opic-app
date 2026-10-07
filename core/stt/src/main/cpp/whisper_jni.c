// whisper.cpp ↔ Kotlin(com.jooh.opic.core.stt.WhisperNative) 연결. 예제(examples/whisper.android)를 우리 패키지에 맞게 줄인 것.
#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define TAG "WhisperJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

JNIEXPORT jlong JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_initContext(JNIEnv *env, jobject thiz, jstring model_path) {
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    struct whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    (*env)->ReleaseStringUTFChars(env, model_path, path);
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_freeContext(JNIEnv *env, jobject thiz, jlong ctx) {
    whisper_free((struct whisper_context *) ctx);
}

// 결과는 UTF-8 바이트로 넘긴다 (NewStringUTF는 4바이트 문자에서 실패할 수 있음)
JNIEXPORT jbyteArray JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_transcribe(JNIEnv *env, jobject thiz, jlong ctx_ptr, jint threads, jfloatArray audio) {
    struct whisper_context *ctx = (struct whisper_context *) ctx_ptr;
    jfloat *data = (*env)->GetFloatArrayElements(env, audio, NULL);
    const jsize n = (*env)->GetArrayLength(env, audio);

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = threads;
    params.language = "en";
    params.translate = false;
    params.no_context = true;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.single_segment = false;

    whisper_reset_timings(ctx);
    const int rc = whisper_full(ctx, params, data, n);
    (*env)->ReleaseFloatArrayElements(env, audio, data, JNI_ABORT);
    if (rc != 0) return NULL;
    whisper_print_timings(ctx);

    const int segments = whisper_full_n_segments(ctx);
    size_t total = 1;
    for (int i = 0; i < segments; i++) total += strlen(whisper_full_get_segment_text(ctx, i));
    char *text = calloc(total, 1);
    for (int i = 0; i < segments; i++) strcat(text, whisper_full_get_segment_text(ctx, i));
    const jsize len = (jsize) strlen(text);
    jbyteArray result = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, result, 0, len, (const jbyte *) text);
    free(text);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_systemInfo(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}
