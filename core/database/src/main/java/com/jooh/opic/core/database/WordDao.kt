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
               SUM(CASE WHEN u.wrongCount > 0 AND u.correctCount < 3 THEN 1 ELSE 0 END) AS wrong
        FROM words w LEFT JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.deleted = 0
        GROUP BY (w.seq - 1) / :wordsPerDay ORDER BY day
    """)
    suspend fun dayStats(language: String, wordsPerDay: Int): List<DayStats>

    /** 같은 집계를 기록이 바뀔 때만 다시 계산해 흘려보낸다 (TASK 39, Room 무효화 추적). */
    @Query("""
        SELECT (w.seq - 1) / :wordsPerDay + 1 AS day, COUNT(*) AS total,
               SUM(CASE WHEN u.correctCount >= 3 THEN 1 ELSE 0 END) AS mastered,
               SUM(CASE WHEN u.wrongCount > 0 AND u.correctCount < 3 THEN 1 ELSE 0 END) AS wrong
        FROM words w LEFT JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.deleted = 0
        GROUP BY (w.seq - 1) / :wordsPerDay ORDER BY day
    """)
    fun observeDayStats(language: String, wordsPerDay: Int): kotlinx.coroutines.flow.Flow<List<DayStats>>

    @Query("SELECT COUNT(*) FROM words WHERE language = :language AND deleted = 0")
    suspend fun activeCount(language: String): Int

    /** 오답 단어: 틀린 적이 있고 아직 습득(정답 3회) 전. 전체는 firstSeq = 1, lastSeq = Int.MAX_VALUE. */
    @Query("""
        SELECT w.*, u.correctCount AS correctCount, u.wrongCount AS wrongCount
        FROM words w JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.seq BETWEEN :firstSeq AND :lastSeq AND w.deleted = 0
          AND u.wrongCount > 0 AND u.correctCount < 3
        ORDER BY w.seq
    """)
    suspend fun getWrongWords(language: String, firstSeq: Int, lastSeq: Int): List<WordWithProgress>

    /** 단어 복습 후보: 오답 단어와 맞은 횟수·마지막 학습 시각 (날짜 판단은 core/common isWordReviewDue). */
    @Query("""
        SELECT w.*, u.correctCount AS correctCount, u.lastStudiedAt AS lastStudiedAt
        FROM words w JOIN user_words u ON u.wordId = w.id
        WHERE w.language = :language AND w.deleted = 0 AND u.wrongCount > 0 AND u.correctCount < 3 AND u.lastStudiedAt IS NOT NULL
        ORDER BY u.lastStudiedAt
    """)
    suspend fun reviewCandidates(language: String): List<ReviewCandidate>

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

/** 통계용 (키, 합계) 한 줄 (TASK 39). */
data class KeyCount(val name: String, val count: Int)
data class PaceRow(val wordsPerMinute: Int, val fillerCount: Int)

data class DayStats(val day: Int, val total: Int, val mastered: Int, val wrong: Int)

/** 학습 기록이 없으면 correctCount·wrongCount가 null (= 신규). */
data class WordRecordRow(val id: Long, val word: String, val meaningKo: String, val correctCount: Int, val wrongCount: Int, val lastStudiedAt: Long?)
data class ReviewCandidate(@Embedded val word: WordEntity, val correctCount: Int, val lastStudiedAt: Long)
data class WordWithProgress(@Embedded val word: WordEntity, val correctCount: Int?, val wrongCount: Int?)
