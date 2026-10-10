package com.jooh.opic

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.jooh.opic.core.database.ImportResult
import com.jooh.opic.core.database.WordImporter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.room.Room
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.ALL_MIGRATIONS
import com.jooh.opic.feature.grammar.GrammarReviewStore
import com.jooh.opic.feature.grammar.RoomGrammarReviewStore
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.HttpModelStore
import com.jooh.opic.core.llm.SharedFirstModelStore
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import com.jooh.opic.core.llm.createOnDeviceLlmEngine
import com.jooh.opic.feature.grammar.GrammarCatalog
import com.jooh.opic.feature.grammar.GrammarLoadResult
import com.jooh.opic.feature.grammar.mergeBooks
import com.jooh.opic.feature.shadowing.ShadowingViewModel
import com.jooh.opic.core.stt.UserWhisper
import com.jooh.opic.core.ui.UiSettings
import com.jooh.opic.core.common.SpeakingCatalog
import com.jooh.opic.core.common.AnswerTemplates
import com.jooh.opic.core.common.parseAnswerTemplates
import com.jooh.opic.core.common.ShadowingVideo
import com.jooh.opic.core.common.parseShadowingLibrary
import com.jooh.opic.core.common.parseSpeakingCatalog

class OpicApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableImport = MutableStateFlow<ImportResult?>(null)
    val importResult = mutableImport.asStateFlow()
    private val mutableGrammar = MutableStateFlow<GrammarLoadResult?>(null)
    val grammarResult = mutableGrammar.asStateFlow()
    /** speaking.json 파싱 결과. 바깥 null = 읽는 중, 안쪽 catalog null = 실패. */
    data class SpeakingLoad(val catalog: SpeakingCatalog?, val templates: AnswerTemplates? = null)
    private val mutableSpeaking = MutableStateFlow<SpeakingLoad?>(null)
    val speakingCatalog = mutableSpeaking.asStateFlow()
    /** 섀도잉 추천 영상 (TASK 21). 실패하면 빈 목록 — 링크 붙여 넣기는 그대로. */
    val shadowingVideos: List<ShadowingVideo> by lazy {
        runCatching { assets.open("shadowing.json").bufferedReader().use { parseShadowingLibrary(it.readText()) } }.getOrNull()?.videos.orEmpty()
    }

    override fun onCreate() {
        super.onCreate()
        UiSettings.load(this)
        scope.launch {
            val start = SystemClock.elapsedRealtime()
            val result = try {
                assets.open("words.json").bufferedReader().use { WordImporter(database).importWords(it.readText()) }
            } catch (error: Exception) {
                ImportResult.Failed(error.message ?: "단어 파일을 읽지 못했습니다")
            }
            mutableImport.value = result
            Log.i("WordImport", "$result elapsedMs=${SystemClock.elapsedRealtime() - start}")
        }
        scope.launch {
            mutableGrammar.value = try {
                val opic = GrammarCatalog.parse(assets.open("grammar.json").bufferedReader().use { it.readText() })
                // 실전 영문법 (TASK 26): 없거나 깨져도 OPIc 문법은 그대로
                val core = runCatching { GrammarCatalog.parse(assets.open("grammar-core.json").bufferedReader().use { it.readText() }) }.getOrNull()
                if (opic is GrammarLoadResult.Loaded && core is GrammarLoadResult.Loaded) mergeBooks(opic, core).takeIf { it is GrammarLoadResult.Loaded } ?: opic else opic
            } catch (_: Exception) { GrammarLoadResult.Failed }
        }
        scope.launch {
            mutableSpeaking.value = SpeakingLoad(runCatching { assets.open("speaking.json").bufferedReader().use { parseSpeakingCatalog(it.readText()) } }.getOrNull(),
                runCatching { assets.open("templates.json").bufferedReader().use { parseAnswerTemplates(it.readText()) } }.getOrNull())
        }
    }

    val whisper by lazy { UserWhisper(this) }
    fun releaseGemmaBeforeWhisper() {
        synchronized(this) { currentLlmEngine?.close(); currentLlmEngine = null }
    }

    val database: OpicDatabase by lazy {
        Room.databaseBuilder(this, OpicDatabase::class.java, "opic.db").addMigrations(*ALL_MIGRATIONS).build()
    }
    val grammarReviews: GrammarReviewStore by lazy { RoomGrammarReviewStore(database.grammarReviewDao()) }
    val hfTokenStore: HfTokenStore by lazy { HfTokenStore(this) }
    private var currentLlmEngine: OnDeviceLlmEngine? = null
    val llmEngine: OnDeviceLlmEngine
        get() = synchronized(this) { currentLlmEngine ?: createOnDeviceLlmEngine(
            context = this,
            config = ModelCatalog.config(this),
            store = SharedFirstModelStore(HttpModelStore(headers = { hfTokenStore.authHeader() })),
        ).also { currentLlmEngine = it } }
}
