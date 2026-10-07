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
    @Query("SELECT * FROM grammar_reviews WHERE exerciseId = :exerciseId LIMIT 1")
    suspend fun get(exerciseId: String): GrammarReviewEntity?

    @Query("SELECT * FROM grammar_reviews WHERE dueEpochDay <= :today ORDER BY dueEpochDay, exerciseId")
    suspend fun due(today: Long): List<GrammarReviewEntity>

    @Insert
    suspend fun insert(review: GrammarReviewEntity)

    @Update
    suspend fun update(review: GrammarReviewEntity)

    @Delete
    suspend fun delete(review: GrammarReviewEntity)

    /** 연습에서 2차 정답·오답: 없으면 1단계로 추가, 있으면 1단계로 되돌림. */
    @Transaction
    suspend fun recordPractice(exerciseId: String, unitId: String, today: Long, now: Long) {
        val schedule = scheduleAfterPractice(today)
        val existing = get(exerciseId)
        if (existing == null) insert(GrammarReviewEntity(exerciseId, unitId, schedule.stage, schedule.dueEpochDay, 1, now))
        else update(existing.copy(stage = schedule.stage, dueEpochDay = schedule.dueEpochDay, wrongCount = existing.wrongCount + 1, lastStudiedAt = now))
    }

    /** 복습 결과 반영. 3단계를 1차에 맞히면 졸업(행 삭제). 기록이 없으면 아무것도 하지 않는다. */
    @Transaction
    suspend fun recordReview(exerciseId: String, firstTryCorrect: Boolean, today: Long, now: Long) {
        val existing = get(exerciseId) ?: return
        val schedule = scheduleAfterReview(existing.stage, firstTryCorrect, today)
        if (schedule == null) delete(existing)
        else update(existing.copy(stage = schedule.stage, dueEpochDay = schedule.dueEpochDay,
            wrongCount = existing.wrongCount + if (firstTryCorrect) 0 else 1, lastStudiedAt = now))
    }
}
