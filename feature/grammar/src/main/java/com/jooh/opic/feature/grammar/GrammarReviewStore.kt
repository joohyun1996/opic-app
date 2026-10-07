package com.jooh.opic.feature.grammar

import com.jooh.opic.core.database.GrammarReviewDao
import java.time.LocalDate

/** 문법 간격 복습 저장소. 테스트에서는 가짜 구현을 쓴다. */
interface GrammarReviewStore {
    /** 오늘(포함) 이전에 복습할 문제 id, 오래된 순 */
    suspend fun dueIds(): List<String>
    suspend fun recordPractice(exerciseId: String, unitId: String)
    suspend fun recordReview(exerciseId: String, firstTryCorrect: Boolean)
}

class RoomGrammarReviewStore(
    private val dao: GrammarReviewDao,
    private val today: () -> Long = { LocalDate.now().toEpochDay() },
) : GrammarReviewStore {
    override suspend fun dueIds() = dao.due(today()).map { it.exerciseId }
    override suspend fun recordPractice(exerciseId: String, unitId: String) =
        dao.recordPractice(exerciseId, unitId, today(), System.currentTimeMillis())
    override suspend fun recordReview(exerciseId: String, firstTryCorrect: Boolean) =
        dao.recordReview(exerciseId, firstTryCorrect, today(), System.currentTimeMillis())
}

/** grammar.json에서 빠진 문제는 복습 목록에서 건너뛴다 (기록은 지우지 않는다). */
suspend fun GrammarReviewStore.dueExercises(book: GrammarBook): List<GrammarExercise> {
    val byId = book.units.flatMap { it.exercises }.associateBy { it.id }
    return dueIds().mapNotNull { byId[it] }
}
