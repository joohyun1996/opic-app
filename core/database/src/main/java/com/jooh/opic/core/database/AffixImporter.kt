package com.jooh.opic.core.database

import androidx.room.withTransaction
import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 검수된 씨드만 읽는다. 단어와 학습 기록은 조회만 한다. */
class AffixImporter(private val database: OpicDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 앞부분의 dataVersion으로 이미 적재된 씨드면 전체 파싱을 건너뛴다. */
    suspend fun importAffixes(source: InputStream): ImportResult = try {
        SeedVersion.readIfNewer(database, VERSION_KEY, source)?.let { importAffixes(it) } ?: ImportResult.UpToDate(SeedVersion.stored(database, VERSION_KEY))
    } catch (error: kotlin.coroutines.cancellation.CancellationException) {
        throw error
    } catch (error: Exception) {
        ImportResult.Failed(error.message ?: "접사 데이터 적재 실패")
    }

    suspend fun importAffixes(raw: String): ImportResult = try {
        val input = json.decodeFromString<AffixFile>(raw)
        require(input.dataVersion >= 1)
        val stored = SeedVersion.stored(database, VERSION_KEY)
        if (stored >= input.dataVersion) ImportResult.UpToDate(stored)
        else load(input)
    } catch (error: kotlin.coroutines.cancellation.CancellationException) {
        throw error
    } catch (error: Exception) {
        ImportResult.Failed(error.message ?: "접사 데이터 적재 실패")
    }

    private suspend fun load(input: AffixFile): ImportResult = database.withTransaction {
        val stored = SeedVersion.stored(database, VERSION_KEY)
        if (stored >= input.dataVersion) return@withTransaction ImportResult.UpToDate(stored)
        val dao = database.affixDao()
        val words = database.wordDao()
        val affixKeys = mutableSetOf<Triple<String, String, String>>()
        val affixes = input.affixes.associate { item ->
            require(item.language.isNotBlank() && item.form.isNotBlank() && item.meaningKo.isNotBlank())
            require(item.type == "prefix" || item.type == "suffix")
            val key = Triple(item.language, item.form, item.type)
            require(affixKeys.add(key)) { "중복 접사: $key" }
            key to item
        }
        val selected = mutableSetOf<Pair<String, String>>()
        val resolved = input.words.map { item ->
            require(item.transparent) { "검수 미통과 단어: ${item.word}" }
            val key = item.language to item.word
            require(selected.add(key)) { "중복 분해: $key" }
            val word = requireNotNull(words.find(item.language, item.word)) { "단어 없음: $key" }
            val base = requireNotNull(words.find(item.language, item.base)) { "어간 없음: ${item.base}" }
            require(!word.deleted && !base.deleted && item.word != item.base && item.glossKo.isNotBlank())
            val affix = requireNotNull(affixes[Triple(item.language, item.affix, item.type)]) { "접사 없음: ${item.affix}" }
            Triple(item, word.id, base.id) to affix
        }
        val ids = affixes.mapValues { (key, item) ->
            val old = dao.findAffix(key.first, key.second, key.third)
            if (old == null) dao.insertAffix(AffixEntity(language = item.language, form = item.form, type = item.type, meaningKo = item.meaningKo))
            else { dao.updateAffix(old.copy(meaningKo = item.meaningKo)); old.id }
        }
        val wanted = mutableMapOf<Triple<String, Long, Int>, WordPartEntity>()
        for ((pair, _) in resolved) {
            val (item, wordId, baseId) = pair
            val affixId = requireNotNull(ids[Triple(item.language, item.affix, item.type)])
            val first = if (item.type == "prefix") WordPartEntity(language = item.language, wordId = wordId, position = 0, affixId = affixId, glossKo = item.glossKo)
                else WordPartEntity(language = item.language, wordId = wordId, position = 0, baseWordId = baseId, glossKo = item.glossKo)
            val second = if (item.type == "prefix") WordPartEntity(language = item.language, wordId = wordId, position = 1, baseWordId = baseId)
                else WordPartEntity(language = item.language, wordId = wordId, position = 1, affixId = affixId)
            wanted[Triple(item.language, wordId, 0)] = first
            wanted[Triple(item.language, wordId, 1)] = second
        }
        val languages = input.affixes.map { it.language }.toSet() + input.words.map { it.language }
        for (language in languages) {
            val existing = dao.allParts(language).associateBy { Triple(it.language, it.wordId, it.position) }
            for ((key, old) in existing) if (key !in wanted) dao.deletePart(language, old.id)
            for ((key, part) in wanted.filterKeys { it.first == language }) {
                val old = existing[key]
                if (old == null) dao.insertPart(part) else dao.updatePart(part.copy(id = old.id))
            }
            dao.deleteUnusedAffixes(language)
        }
        SeedVersion.save(database, VERSION_KEY, input.dataVersion)
        ImportResult.Imported(input.dataVersion, input.words.size)
    }

    companion object { const val VERSION_KEY = "affixes_data_version" }
}

@Serializable private data class AffixFile(val dataVersion: Int, val affixes: List<AffixRecord>, val words: List<AffixWordRecord>)
@Serializable private data class AffixRecord(val language: String, val form: String, val type: String, val meaningKo: String)
@Serializable private data class AffixWordRecord(val language: String, val word: String, val base: String, val affix: String, val type: String, val glossKo: String, val transparent: Boolean)
