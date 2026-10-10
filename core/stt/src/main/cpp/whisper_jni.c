#include <stdio.h>
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

typedef struct { JavaVM *vm; jobject flag; jmethodID get; } abort_state;
static bool should_abort(void *user_data) {
    abort_state *state = (abort_state *) user_data;
    JNIEnv *env = NULL;
    bool attached = false;
    if ((*state->vm)->GetEnv(state->vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*state->vm)->AttachCurrentThread(state->vm, &env, NULL) != JNI_OK) return true;
        attached = true;
    }
    bool cancelled = (*env)->CallBooleanMethod(env, state->flag, state->get);
    if (attached) (*state->vm)->DetachCurrentThread(state->vm);
    return cancelled;
}

// 결과는 UTF-8 바이트로 넘긴다 (NewStringUTF는 4바이트 문자에서 실패할 수 있음)
JNIEXPORT jbyteArray JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_transcribe(JNIEnv *env, jobject thiz, jlong ctx_ptr, jint threads, jfloatArray audio, jobject cancelled, jstring prompt, jboolean with_tokens) {
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
    params.token_timestamps = with_tokens;
    // 선택: 앞 문맥 힌트 (예: 머뭇거림을 지우지 않게 "Um, uh, ..."). null이면 기본 동작
    const char *prompt_chars = prompt != NULL ? (*env)->GetStringUTFChars(env, prompt, NULL) : NULL;
    if (prompt_chars != NULL) params.initial_prompt = prompt_chars;

    abort_state abort = {0};
    if (cancelled != NULL) {
        (*env)->GetJavaVM(env, &abort.vm);
        abort.flag = (*env)->NewGlobalRef(env, cancelled);
        jclass flag_class = (*env)->GetObjectClass(env, cancelled);
        abort.get = (*env)->GetMethodID(env, flag_class, "get", "()Z");
        (*env)->DeleteLocalRef(env, flag_class);
        params.abort_callback = should_abort;
        params.abort_callback_user_data = &abort;
    }
    whisper_reset_timings(ctx);
    const int rc = whisper_full(ctx, params, data, n);
    if (abort.flag != NULL) (*env)->DeleteGlobalRef(env, abort.flag);
    if (prompt_chars != NULL) (*env)->ReleaseStringUTFChars(env, prompt, prompt_chars);
    (*env)->ReleaseFloatArrayElements(env, audio, data, JNI_ABORT);
    if (rc != 0) return NULL;
    whisper_print_timings(ctx);

    // 결과: 전체 텍스트. with_tokens면 뒤에 "\n#TOKENS\n" + 줄마다 "t0\tt1\tp\ttext" (t0·t1은 10ms 단위)
    const int segments = whisper_full_n_segments(ctx);
    size_t cap = 256;
    for (int i = 0; i < segments; i++) {
        cap += strlen(whisper_full_get_segment_text(ctx, i));
        if (with_tokens) for (int j = 0; j < whisper_full_n_tokens(ctx, i); j++) cap += strlen(whisper_full_get_token_text(ctx, i, j)) + 48;
    }
    char *text = calloc(cap, 1);
    size_t used = 0;
    for (int i = 0; i < segments; i++) used += snprintf(text + used, cap - used, "%s", whisper_full_get_segment_text(ctx, i));
    if (with_tokens) {
        used += snprintf(text + used, cap - used, "\n#TOKENS\n");
        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < whisper_full_n_tokens(ctx, i); j++) {
                const whisper_token_data data = whisper_full_get_token_data(ctx, i, j);
                used += snprintf(text + used, cap - used, "%lld\t%lld\t%.4f\t%s\n",
                    (long long) data.t0, (long long) data.t1, data.p, whisper_full_get_token_text(ctx, i, j));
            }
        }
    }
    const jsize len = (jsize) used;
    jbyteArray result = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, result, 0, len, (const jbyte *) text);
    free(text);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_jooh_opic_core_stt_WhisperNative_systemInfo(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}
