package com.jooh.opic.feature.analysis

import com.jooh.opic.core.common.DailyCount
import com.jooh.opic.core.common.recentDailyCounts
import com.jooh.opic.core.common.studyStreak
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

/**
 * 집계는 SQL로, 날짜 변환(기기 시간대)만 Kotlin에서 한다 (TASK 39). 큰 열(답변 원문 등)은 읽지 않는다.
 * user_words는 마지막 학습 시각만 있어 "마지막으로 그날 공부한 단어 수"로 센다.
 */
suspend fun loadStats(db: OpicDatabase, language: String, today: Long, zone: ZoneId = ZoneId.systemDefault()): StatsData {
    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
    val activity = (db.userWordDao().studiedAt(language) + db.grammarReviewDao().studiedAt(language) +
        db.speakingDao().createdAt(language) + db.shadowingAttemptDao().createdAt(language)).map(::day)
    val pace = db.speakingDao().recentPace(language).reversed()
    val shadowingCount = db.shadowingAttemptDao().createdAt(language).size
    return StatsData(
        streak = studyStreak(activity.toSet(), today),
        week = recentDailyCounts(activity, today),
        wordsMastered = db.userWordDao().masteredCount(language), wordsTotal = db.wordDao().activeCount(language),
        hardWords = db.userWordDao().mostWrong(language).map { it.name to it.count },
        grammarWrong = db.grammarReviewDao().mostWrongUnits(language).map { it.name to it.count },
        grammarDue = db.grammarReviewDao().dueCount(language, today),
        speakingCount = db.speakingDao().createdAt(language).size,
        recentWpm = pace.map { it.wordsPerMinute }, recentFillers = pace.map { it.fillerCount },
        shadowingCount = shadowingCount, shadowingAvg = if (shadowingCount == 0) null else db.shadowingAttemptDao().averageMatch(language),
    )
}

