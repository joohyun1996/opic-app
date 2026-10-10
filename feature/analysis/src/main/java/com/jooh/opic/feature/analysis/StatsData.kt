package com.jooh.opic.feature.analysis

import com.jooh.opic.core.common.DailyCount
import com.jooh.opic.core.common.recentDailyCounts
import com.jooh.opic.core.common.studyStreak
import com.jooh.opic.core.common.topByCount
import com.jooh.opic.core.database.OpicDatabase
import java.time.Instant
import java.time.ZoneId

/** 학습 통계 화면 데이터 (TASK 30). 모두 한 언어 기준. */
data class StatsData(
    val streak: Int,
    val week: List<DailyCount>,
    val wordsMastered: Int, val wordsTotal: Int, val hardWords: List<Pair<String, Int>>,
    val grammarWrong: List<Pair<String, Int>>, val grammarDue: Int,
    val speakingCount: Int, val recentWpm: List<Int>, val recentFillers: List<Int>,
    val shadowingCount: Int, val shadowingAvg: Double?,
) {
    val empty: Boolean get() = week.all { it.count == 0 } && wordsMastered == 0 && speakingCount == 0 && shadowingCount == 0 && grammarWrong.isEmpty()
}

/** 기존 읽기 쿼리만 써서 집계한다 (스키마 변경 없음). user_words는 마지막 학습 시각만 있어 "그날 공부한 단어 수"로 센다. */
suspend fun loadStats(db: OpicDatabase, language: String, today: Long, zone: ZoneId = ZoneId.systemDefault()): StatsData {
    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
    val words = db.userWordDao().backupRows(language)
    val reviews = db.grammarReviewDao().all(language)
    val speaking = db.speakingDao().all(language)
    val shadowing = db.shadowingAttemptDao().all(language)
    val activity = words.mapNotNull { it.lastStudiedAt?.let(::day) } + reviews.map { day(it.lastStudiedAt) } +
        speaking.map { day(it.createdAt) } + shadowing.map { day(it.createdAt) }
    val wordsTotal = db.wordDao().dayStats(language, 40).sumOf { it.total }
    val recent = speaking.sortedByDescending { it.createdAt }.take(10).reversed()
    return StatsData(
        streak = studyStreak(activity.toSet(), today),
        week = recentDailyCounts(activity, today),
        wordsMastered = words.count { it.correctCount >= 3 }, wordsTotal = wordsTotal,
        hardWords = topByCount(words.map { it.word to it.wrongCount }),
        grammarWrong = topByCount(reviews.map { it.unitId to it.wrongCount }),
        grammarDue = reviews.count { it.dueEpochDay <= today },
        speakingCount = speaking.size, recentWpm = recent.map { it.wordsPerMinute }, recentFillers = recent.map { it.fillerCount },
        shadowingCount = shadowing.size, shadowingAvg = shadowing.takeIf { it.isNotEmpty() }?.map { it.matchRate }?.average(),
    )
}
