package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE language = :language AND seq BETWEEN (:day - 1) * 40 + 1 AND :day * 40 AND deleted = 0 ORDER BY seq")
    suspend fun getDayWords(language: String, day: Int): List<WordEntity>

    @Query("SELECT MAX(seq) FROM words WHERE language = :language")
    suspend fun maxSeq(language: String): Int?

    @Query("SELECT COUNT(*) FROM words WHERE language = :language")
    suspend fun countByLanguage(language: String): Int

    @Query("SELECT * FROM words WHERE language = :language AND word = :word LIMIT 1")
    suspend fun find(language: String, word: String): WordEntity?

    @Insert
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Transaction
    suspend fun upsertWords(words: List<WordEntity>) {
        for (incoming in words) {
            val normalized = incoming.copy(word = incoming.word.trim().lowercase())
            val existing = find(normalized.language, normalized.word)
            if (existing == null) insert(normalized.copy(id = 0))
            else update(normalized.copy(id = existing.id, seq = existing.seq))
        }
    }
}
