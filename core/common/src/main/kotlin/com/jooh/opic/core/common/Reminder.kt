package com.jooh.opic.core.common

import java.time.LocalDateTime
import java.time.LocalTime

/** 하루 한 번 학습 알림 (TASK 34). 다음 알림 시각: 오늘 그 시각이 아직 안 지났으면 오늘, 지났으면 내일. */
fun nextReminderAt(now: LocalDateTime, at: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(at)
    return if (now < today) today else today.plusDays(1)
}

/** 오늘 이미 공부했으면 null(알림 안 보냄). [nextDay]는 이어서 할 영단어 Day (없으면 null). */
fun reminderText(studiedToday: Boolean, nextDay: Int?, grammarDue: Int): String? {
    if (studiedToday) return null
    val parts = listOfNotNull(nextDay?.let { "영단어 Day $it" }, grammarDue.takeIf { it > 0 }?.let { "문법 복습 ${it}문제" })
    return if (parts.isEmpty()) "오늘도 5분만 해 볼까요?" else "오늘 할 일: " + parts.joinToString(" · ")
}
