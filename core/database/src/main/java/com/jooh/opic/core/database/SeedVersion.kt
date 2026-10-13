package com.jooh.opic.core.database

import java.io.InputStream

/** 내장 씨드(words.json·affixes.json)의 dataVersion 확인·저장 공통 로직. */
internal object SeedVersion {
    private const val PEEK_BYTES = 4096
    private val VERSION_PATTERN = Regex("\"dataVersion\"\\s*:\\s*(\\d+)")

    /** 파일 앞부분에서 dataVersion만 찾는다. 못 찾으면 null (전체 파싱으로 넘어간다). */
    fun peek(text: String): Int? = VERSION_PATTERN.find(text.take(PEEK_BYTES))?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it >= 1 }

    /** 앞부분만 읽어 이미 적재된 버전이면 null, 아니면 전체 텍스트를 돌려준다. */
    suspend fun readIfNewer(database: OpicDatabase, key: String, source: InputStream): String? {
        val input = source.buffered()
        input.mark(PEEK_BYTES)
        val peeked = peek(input.readNBytes(PEEK_BYTES).toString(Charsets.UTF_8))
        if (peeked != null && stored(database, key) >= peeked) return null
        input.reset()
        return input.bufferedReader().use { it.readText() }
    }

    suspend fun stored(database: OpicDatabase, key: String): Int = database.dataMetaDao().get(key)?.toIntOrNull() ?: 0

    suspend fun save(database: OpicDatabase, key: String, version: Int) {
        // 행이 없으면 insert(IGNORE)로 만들고, 있으면 insert는 무시되므로 update로 값을 바꾼다.
        database.dataMetaDao().insert(DataMetaEntity(key, version.toString()))
        database.dataMetaDao().update(key, version.toString())
    }
}
