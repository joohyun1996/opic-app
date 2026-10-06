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
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.HttpModelStore
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import com.jooh.opic.core.llm.createOnDeviceLlmEngine

class OpicApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableImport = MutableStateFlow<ImportResult?>(null)
    val importResult = mutableImport.asStateFlow()

    override fun onCreate() {
        super.onCreate()
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
    }

    val database: OpicDatabase by lazy {
        Room.databaseBuilder(this, OpicDatabase::class.java, "opic.db").build()
    }
    val hfTokenStore: HfTokenStore by lazy { HfTokenStore(this) }
    val llmEngine: OnDeviceLlmEngine by lazy {
        createOnDeviceLlmEngine(
            context = this,
            config = ModelCatalog.config(this),
            store = HttpModelStore(headers = { hfTokenStore.authHeader() }),
        )
    }
}
