package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 영상별 연습 요약. */
data class VideoPracticeRow(val videoId: String, val attempts: Int, val bestMatchRate: Double, val lastAt: Long)

@Dao
interface ShadowingAttemptDao {
    @Insert suspend fun insert(attempt: ShadowingAttemptEntity): Long

    @Query("SELECT videoId, COUNT(*) AS attempts, MAX(matchRate) AS bestMatchRate, MAX(createdAt) AS lastAt FROM shadowing_attempts WHERE language = :language GROUP BY videoId ORDER BY lastAt DESC LIMIT :limit")
    suspend fun recentVideos(language: String, limit: Int = 100): List<VideoPracticeRow>

    @Query("SELECT * FROM shadowing_attempts WHERE language = :language ORDER BY createdAt")
    suspend fun all(language: String): List<ShadowingAttemptEntity>

    @Query("SELECT COUNT(*) FROM shadowing_attempts WHERE language = :language AND createdAt = :createdAt AND videoId = :videoId")
    suspend fun countSame(language: String, createdAt: Long, videoId: String): Int
}
