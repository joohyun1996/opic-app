package com.jooh.opic.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jooh.opic.core.common.scheduleAfterPractice
import com.jooh.opic.core.common.scheduleAfterReview

@Dao
interface GrammarReviewDao {
    @Query("SELECT * FROM grammar_reviews WHERE language = :language AND exerciseId = :exerciseId LIMIT 1")
    suspend fun get(language: String, exerciseId: String): GrammarReviewEntity?

    /** 백업·통계용. */
    @Query("SELECT * FROM grammar_reviews WHERE language = :language ORDER BY exerciseId")
    suspend fun all(language: String): List<GrammarReviewEntity>

    @Query("SELECT * FROM grammar_reviews WHERE language = :language AND dueEpochDay <= :today ORDER BY dueEpochDay, exerciseId")
    suspend fun due(language: String, today: Long): List<GrammarReviewEntity>

    @Insert
    suspend fun insert(review: GrammarReviewEntity)

    @Update
    suspend fun update(review: GrammarReviewEntity)

    @Delete
    suspend fun delete(review: GrammarReviewEntity)

    /** 연습에서 2차 정답·오답: 없으면 1단계로 추가, 있으면 1단계로 되돌림. */
    @Transaction
    suspend fun recordPractice(language: String, exerciseId: String, unitId: String, today: Long, now: Long) {
        val schedule = scheduleAfterPractice(today)
        val existing = get(language, exerciseId)
        if (existing == null) insert(GrammarReviewEntity(language, exerciseId, unitId, schedule.stage, schedule.dueEpochDay, 1, now))
        else update(existing.copy(stage = schedule.stage, dueEpochDay = schedule.dueEpochDay, wrongCount = existing.wrongCount + 1, lastStudiedAt = now))
    }

    /** 복습 결과 반영. 3단계를 1차에 맞히면 졸업(행 삭제). 기록이 없으면 아무것도 하지 않는다. */
    @Transaction
    suspend fun recordReview(language: String, exerciseId: String, firstTryCorrect: Boolean, today: Long, now: Long) {
        val existing = get(language, exerciseId) ?: return
        val schedule = scheduleAfterReview(existing.stage, firstTryCorrect, today)
        if (schedule == null) delete(existing)
        else update(existing.copy(stage = schedule.stage, dueEpochDay = schedule.dueEpochDay,
            wrongCount = existing.wrongCount + if (firstTryCorrect) 0 else 1, lastStudiedAt = now))
    }

    // ---- 통계 (TASK 39) ----
    @Query("SELECT lastStudiedAt FROM grammar_reviews WHERE language = :language")
    suspend fun studiedAt(language: String): List<Long>

    @Query("SELECT unitId AS name, SUM(wrongCount) AS count FROM grammar_reviews WHERE language = :language GROUP BY unitId HAVING SUM(wrongCount) > 0 ORDER BY count DESC, unitId LIMIT :limit")
    suspend fun mostWrongUnits(language: String, limit: Int = 5): List<KeyCount>

    @Query("SELECT COUNT(*) FROM grammar_reviews WHERE language = :language AND dueEpochDay <= :today")
    suspend fun dueCount(language: String, today: Long): Int
}
