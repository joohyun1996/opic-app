package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 모의고사 한 번의 요약. */
data class MockSummaryRow(val mockId: Long, val createdAt: Long, val answered: Int, val avgWpm: Double, val totalFillers: Int)

@Dao
interface SpeakingDao {
    @Insert suspend fun insert(answer: SpeakingAnswerEntity): Long

    @Query("UPDATE speaking_answers SET editedText = :editedText, wordCount = :wordCount, wordsPerMinute = :wpm, fillerCount = :fillers, sentenceCount = :sentences WHERE id = :id AND language = :language")
    suspend fun updateEdit(language: String, id: Long, editedText: String, wordCount: Int, wpm: Int, fillers: Int, sentences: Int)

    /** 이 질문의 지난 답변 (최근 순). */
    @Query("SELECT * FROM speaking_answers WHERE language = :language AND questionId = :questionId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun byQuestion(language: String, questionId: String, limit: Int = 20): List<SpeakingAnswerEntity>

    @Query("SELECT * FROM speaking_answers WHERE language = :language ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(language: String, limit: Int = 50): List<SpeakingAnswerEntity>

    @Query("SELECT mockId, MIN(createdAt) AS createdAt, COUNT(*) AS answered, AVG(wordsPerMinute) AS avgWpm, SUM(fillerCount) AS totalFillers FROM speaking_answers WHERE language = :language AND mockId IS NOT NULL GROUP BY mockId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun mockSummaries(language: String, limit: Int = 20): List<MockSummaryRow>

    @Query("SELECT * FROM speaking_answers WHERE language = :language ORDER BY createdAt")
    suspend fun all(language: String): List<SpeakingAnswerEntity>

    @Query("SELECT COUNT(*) FROM speaking_answers WHERE language = :language AND createdAt = :createdAt AND questionId = :questionId")
    suspend fun countSame(language: String, createdAt: Long, questionId: String): Int

    // ---- 통계 (TASK 39): transcript·editedText는 읽지 않는다 ----
    @Query("SELECT createdAt FROM speaking_answers WHERE language = :language")
    suspend fun createdAt(language: String): List<Long>

    @Query("SELECT wordsPerMinute, fillerCount FROM speaking_answers WHERE language = :language ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentPace(language: String, limit: Int = 10): List<PaceRow>

    @Query("DELETE FROM speaking_answers WHERE language = :language AND id IN (:ids)")
    suspend fun deleteRecords(language: String, ids: List<Long>): Int
}
