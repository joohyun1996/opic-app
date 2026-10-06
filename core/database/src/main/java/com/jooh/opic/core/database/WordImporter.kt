package com.jooh.opic.core.database

import androidx.room.withTransaction
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
        database.withTransaction {
            val metadata = database.dataMetaDao()
            val stored = metadata.get(VERSION_KEY)?.toInt() ?: 0
            if (stored >= input.dataVersion) ImportResult.UpToDate(stored)
            else {
                database.wordDao().upsertWords(words)
                metadata.insert(DataMetaEntity(VERSION_KEY, input.dataVersion.toString()))
                metadata.update(VERSION_KEY, input.dataVersion.toString())
                ImportResult.Imported(input.dataVersion, words.size)
            }
        }
    } catch (error: Exception) {
        ImportResult.Failed(error.message ?: "단어 데이터 적재 실패")
    }

    companion object { const val VERSION_KEY = "words_data_version" }
}

@Serializable
private data class WordFile(val dataVersion: Int, val words: List<WordRecord>)

@Serializable
private data class WordRecord(
    val language: String, val word: String, val seq: Int, val phonetic: String?,
    val meaningKo: String, val meaningEn: String?, val example: String?, val exampleKo: String?,
    val level: Int, val category: String, val partOfSpeech: String?, val collocations: List<String>, val deleted: Boolean,
)
