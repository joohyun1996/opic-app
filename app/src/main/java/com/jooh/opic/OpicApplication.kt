package com.jooh.opic

import android.app.Application
import androidx.room.Room
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.HttpModelStore
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import com.jooh.opic.core.llm.createOnDeviceLlmEngine

class OpicApplication : Application() {
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
