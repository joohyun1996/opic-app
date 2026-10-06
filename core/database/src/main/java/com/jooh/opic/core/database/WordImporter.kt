package com.jooh.opic.core.database

import androidx.room.withTransaction
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface ImportResult {
    data class Imported(val version: Int, val count: Int) : ImportResult
    data class UpToDate(val version: Int) : ImportResult
    data class Failed(val reason: String) : ImportResult
}

class WordImporter(private val database: OpicDatabase) {
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun importWords(raw: String): ImportResult = try {
        // 파일 앞부분의 dataVersion만 먼저 보고, 이미 적재된 버전이면 2MB 넘는 전체 파싱을 건너뛴다.
        val peeked = VERSION_PATTERN.find(raw.take(256))?.groupValues?.get(1)?.toIntOrNull()
        val storedBefore = database.dataMetaDao().get(VERSION_KEY)?.toIntOrNull() ?: 0
        if (peeked != null && peeked >= 1 && storedBefore >= peeked) ImportResult.UpToDate(storedBefore) else load(raw)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ImportResult.Failed(error.message ?: "단어 데이터 적재 실패")
    }

    private suspend fun load(raw: String): ImportResult {
        val input = json.decodeFromString<WordFile>(raw)
        require(input.dataVersion >= 1) { "dataVersion은 1 이상이어야 합니다." }
        val keys = mutableSetOf<Pair<String, String>>()
        val sequences = mutableSetOf<Pair<String, Int>>()
        val words = input.words.map { item ->
            val word = item.word.trim().lowercase()
            require(item.language in setOf("en", "zh") && word.isNotBlank() && item.meaningKo.isNotBlank() && item.seq >= 1 && item.level >= 1) { "단어 기본 필드 오류: $word" }
            require(keys.add(item.language to word)) { "중복 단어: $word" }
            require(sequences.add(item.language to item.seq)) { "중복 seq: ${item.seq}" }
            if (item.language == "en") require(listOf(item.phonetic, item.meaningEn, item.example, item.exampleKo).all { !it.isNullOrBlank() }) { "영어 필드 누락: $word" }
            WordEntity(language = item.language, word = word, seq = item.seq,
                phonetic = item.phonetic.orEmpty(), meaningKo = item.meaningKo, meaningEn = item.meaningEn.orEmpty(),
                example = item.example.orEmpty(), exampleKo = item.exampleKo.orEmpty(), level = item.level,
                category = item.category, partOfSpeech = item.partOfSpeech.orEmpty(), collocations = item.collocations, deleted = item.deleted)
        }
        return database.withTransaction {
            val metadata = database.dataMetaDao()
            val stored = metadata.get(VERSION_KEY)?.toInt() ?: 0
            if (stored >= input.dataVersion) ImportResult.UpToDate(stored)
            else {
                database.wordDao().upsertWords(words)
                // 행이 없으면 insert(IGNORE)로 만들고, 있으면 insert는 무시되므로 update로 값을 바꾼다.
                metadata.insert(DataMetaEntity(VERSION_KEY, input.dataVersion.toString()))
                metadata.update(VERSION_KEY, input.dataVersion.toString())
                ImportResult.Imported(input.dataVersion, words.size)
            }
        }
    }

    companion object { const val VERSION_KEY = "words_data_version"
        private val VERSION_PATTERN = Regex("\"dataVersion\"\\s*:\\s*(\\d+)")
    }
}

@Serializable
private data class WordFile(val dataVersion: Int, val words: List<WordRecord>)

@Serializable
private data class WordRecord(
    val language: String, val word: String, val seq: Int, val phonetic: String?,
    val meaningKo: String, val meaningEn: String?, val example: String?, val exampleKo: String?,
    val level: Int, val category: String, val partOfSpeech: String?, val collocations: List<String>, val deleted: Boolean,
)
