package com.jooh.opic.core.common

/** 단계별 다음 복습까지 남은 날짜: 1단계 → 1일, 2단계 → 3일, 3단계 → 7일 (Cepeda et al., 2006의 간격 학습). */
val REVIEW_INTERVAL_DAYS = listOf(1, 3, 7)
const val REVIEW_LAST_STAGE = 3
const val REVIEW_SESSION_SIZE = 10

/** stage는 1~3, dueEpochDay는 LocalDate.toEpochDay() 기준. */
data class ReviewSchedule(val stage: Int, val dueEpochDay: Long)

/** 연습에서 힌트가 필요했거나(2차 정답) 끝내 틀린 문제는 1단계로, 다음 날 복습한다. 이미 복습 중이어도 1단계로 되돌린다. */
fun scheduleAfterPractice(today: Long): ReviewSchedule = ReviewSchedule(1, today + REVIEW_INTERVAL_DAYS[0])

/**
 * 복습 결과로 다음 일정을 정한다.
 * - 1차에 맞힘: 다음 단계로 (1 → 2는 3일 뒤, 2 → 3은 7일 뒤). 3단계에서 맞히면 null = 졸업(복습 목록에서 빠짐)
 * - 힌트가 필요했거나 틀림: 1단계로 돌아가 다음 날
 */
fun scheduleAfterReview(stage: Int, firstTryCorrect: Boolean, today: Long): ReviewSchedule? {
    require(stage in 1..REVIEW_LAST_STAGE)
    if (!firstTryCorrect) return scheduleAfterPractice(today)
    if (stage == REVIEW_LAST_STAGE) return null
    return ReviewSchedule(stage + 1, today + REVIEW_INTERVAL_DAYS[stage])
}
