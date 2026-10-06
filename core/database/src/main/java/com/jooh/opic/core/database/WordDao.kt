package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE language = :language AND seq BETWEEN :firstSeq AND :lastSeq AND deleted = 0 ORDER BY seq")
    suspend fun getDayWords(language: String, firstSeq: Int, lastSeq: Int): List<WordEntity>

    @Query("""
        SELECT w.*, u.correctCount AS correctCount, u.wrongCount AS wrongCount
        FROM words w LEFT JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.seq BETWEEN :firstSeq AND :lastSeq AND w.deleted = 0
        ORDER BY w.seq
    """)
    suspend fun getDayWordsWithProgress(language: String, firstSeq: Int, lastSeq: Int): List<WordWithProgress>

    @Query("""
        SELECT (w.seq - 1) / :wordsPerDay + 1 AS day, COUNT(*) AS total,
               SUM(CASE WHEN u.correctCount >= 3 THEN 1 ELSE 0 END) AS mastered,
               SUM(CASE WHEN u.wrongCount > 0 THEN 1 ELSE 0 END) AS wrong
        FROM words w LEFT JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.deleted = 0
        GROUP BY (w.seq - 1) / :wordsPerDay ORDER BY day
    """)
    suspend fun dayStats(language: String, wordsPerDay: Int): List<DayStats>

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

data class DayStats(val day: Int, val total: Int, val mastered: Int, val wrong: Int)

/** 학습 기록이 없으면 correctCount·wrongCount가 null (= 신규). */
data class WordWithProgress(@Embedded val word: WordEntity, val correctCount: Int?, val wrongCount: Int?)
