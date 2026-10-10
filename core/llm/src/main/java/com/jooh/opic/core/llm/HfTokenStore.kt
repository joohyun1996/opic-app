package com.jooh.opic.core.llm

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Hugging Face 토큰(게이트된 모델 저장소 다운로드용) 저장소. 일반 SharedPreferences/DataStore가
 * 아니라 [EncryptedSharedPreferences]를 쓴다 — API 키와 동급 비밀값이라 평문 저장 금지.
 * 토큰 값은 여기서만 다루고, 로그(Logcat 등)에 절대 찍지 않는다 — 호출부도 이 규칙을 지켜야 한다.
 */
class HfTokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences? = openPreferences()

    private fun openPreferences(): SharedPreferences? {
        fun open(name: String, alias: String): SharedPreferences {
            val key = MasterKey.Builder(appContext, alias)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                appContext, name, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).also { it.getString(KEY_TOKEN, null) }
        }

        // 손상된 기존 암호문은 삭제하지 않는다. 새 키와 파일로 다시 입력할 수 있게 한다.
        val recoveryExists = File(appContext.applicationInfo.dataDir, "shared_prefs/$RECOVERY_FILE.xml").exists()
        if (recoveryExists) return try { open(RECOVERY_FILE, RECOVERY_ALIAS) } catch (_: Exception) { null }
        return try {
            open(PRIMARY_FILE, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        } catch (_: Exception) {
            try { open(RECOVERY_FILE, RECOVERY_ALIAS) } catch (_: Exception) { null }
        }
    }

    fun getToken(): String? = try {
        prefs?.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
    } catch (_: Exception) { null }

    fun setToken(token: String): Boolean = try {
        prefs?.edit()?.putString(KEY_TOKEN, token.trim())?.commit() ?: false
    } catch (_: Exception) { false }

    fun clearToken() {
        try { prefs?.edit()?.remove(KEY_TOKEN)?.apply() } catch (_: Exception) { }
    }

    /** [HttpModelStore]의 headers 람다에 그대로 꽂는다. 토큰 없으면 빈 맵(요청은 401/403으로 실패). */
    fun authHeader(): Map<String, String> =
        getToken()?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

    private companion object {
        const val KEY_TOKEN = "hf_token"
        const val PRIMARY_FILE = "opic_hf_token_store"
        const val RECOVERY_FILE = "opic_hf_token_store_recovery"
        const val RECOVERY_ALIAS = "opic_hf_token_store_recovery_key"
    }
}
