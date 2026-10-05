package com.jooh.opic.core.llm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hugging Face 토큰(게이트된 모델 저장소 다운로드용) 저장소. 일반 SharedPreferences/DataStore가
 * 아니라 [EncryptedSharedPreferences]를 쓴다 — API 키와 동급 비밀값이라 평문 저장 금지.
 * 토큰 값은 여기서만 다루고, 로그(Logcat 등)에 절대 찍지 않는다 — 호출부도 이 규칙을 지켜야 한다.
 */
class HfTokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context.applicationContext,
        "opic_hf_token_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /** [HttpModelStore]의 headers 람다에 그대로 꽂는다. 토큰 없으면 빈 맵(요청은 401/403으로 실패). */
    fun authHeader(): Map<String, String> =
        getToken()?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

    private companion object {
        const val KEY_TOKEN = "hf_token"
    }
}
