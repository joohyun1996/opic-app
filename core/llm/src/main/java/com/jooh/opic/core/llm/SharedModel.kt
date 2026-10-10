package com.jooh.opic.core.llm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * 여러 앱이 같이 쓰는 공용 모델 파일 (2026-10-10 사용자 결정: `내장 저장공간/Develop/Core/llm/`).
 * 사용자가 시스템 파일 선택 창으로 한 번 고르면 읽기 권한을 계속 유지한다 (새 권한 없음).
 * MediaPipe는 파일 경로만 받으므로 열린 파일 번호 경로 `/proc/self/fd/N`을 넘긴다.
 */
object SharedModel {
    const val FOLDER_HINT = "Develop/Core/llm"
    private const val PREFS = "opic_shared_model"
    private const val KEY_URI = "uri"
    /** 엔진이 살아 있는 동안 파일 번호가 유효해야 해서 프로세스에 하나만 열어 둔다. */
    @Volatile private var opened: Pair<Uri, ParcelFileDescriptor>? = null

    fun uri(context: Context): Uri? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URI, null)?.let(Uri::parse)

    /** 고른 파일 크기가 [expectedBytes]와 같을 때만 저장한다. */
    fun save(context: Context, uri: Uri, expectedBytes: Long): Boolean {
        val size = runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } }.getOrNull()
        if (size != expectedBytes) return false
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply()
        return true
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_URI).apply()
    }

    /** 공용 파일이 있고 크기가 맞으면 true. */
    fun isValid(context: Context, expectedBytes: Long): Boolean {
        val uri = uri(context) ?: return false
        return runCatching { context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } }.getOrNull() == expectedBytes
    }

    /** 엔진에 넘길 경로. 없거나 크기가 다르면 null. */
    @Synchronized
    fun path(context: Context, expectedBytes: Long): File? {
        val uri = uri(context) ?: return null
        opened?.let { (u, fd) -> if (u == uri && fd.statSize == expectedBytes) return File("/proc/self/fd/${fd.fd}") }
        val fd = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
        if (fd.statSize != expectedBytes) { fd.close(); return null }
        opened?.second?.close()
        opened = uri to fd
        return File("/proc/self/fd/${fd.fd}")
    }
}

/** 공용 파일을 골라 뒀으면 그걸, 아니면 앱 안 파일·다운로드([fallback])를 쓴다. */
class SharedFirstModelStore(private val context: Context, private val fallback: ModelStore) : ModelStore {
    override suspend fun ensure(model: ModelSpec, progress: (Long, Long) -> Unit): File =
        SharedModel.path(context.applicationContext, model.expectedBytes) ?: fallback.ensure(model, progress)
}
