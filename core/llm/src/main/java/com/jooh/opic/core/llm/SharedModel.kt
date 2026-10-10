package com.jooh.opic.core.llm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import java.io.File

/**
 * 여러 앱이 같이 쓰는 공용 모델 파일 (ADR 002): `내장 저장공간/Develop/Core/llm/gemma-3n-e4b-it.task`.
 * MediaPipe는 파일 경로로만 열고, 그 경로를 다시 열 때 저장공간 권한을 검사한다 → 파일 선택(SAF) 권한으로는 안 됨 (2026-10-10 실기기).
 * 그래서 "모든 파일 접근"(MANAGE_EXTERNAL_STORAGE, 사용자가 설정에서 허용)으로 경로를 직접 연다. 모델 파일 읽기에만 쓴다.
 */
object SharedModel {
    const val FOLDER_HINT = "Develop/Core/llm"
    private const val FILE_NAME = "gemma-3n-e4b-it.task"

    @Suppress("DEPRECATION")
    fun file(): File = File(Environment.getExternalStorageDirectory(), "$FOLDER_HINT/$FILE_NAME")

    fun hasAccess(): Boolean = Environment.isExternalStorageManager()

    /** 권한이 있고 공용 파일 크기가 맞으면 true. */
    fun isValid(expectedBytes: Long): Boolean = hasAccess() && runCatching { file().length() == expectedBytes }.getOrDefault(false)

    /** 이 앱의 "모든 파일 접근 허용" 설정 화면. */
    fun accessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
}

/** 공용 파일을 쓸 수 있으면 그걸, 아니면 앱 안 파일·다운로드([fallback])를 쓴다. */
class SharedFirstModelStore(private val fallback: ModelStore) : ModelStore {
    override suspend fun ensure(model: ModelSpec, progress: (Long, Long) -> Unit): File =
        if (SharedModel.isValid(model.expectedBytes)) SharedModel.file() else fallback.ensure(model, progress)
}
