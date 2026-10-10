package com.jooh.opic.core.common

/** 학습 통계 집계 (TASK 30). 날짜는 epochDay(기기 시간대 기준으로 바꾼 값)로 받는다. */
data class DailyCount(val epochDay: Long, val count: Int)

/** 오늘을 포함한 최근 [days]일의 일별 횟수, 오래된 날부터. 빈 날은 0. */
fun recentDailyCounts(activityDays: List<Long>, today: Long, days: Int = 7): List<DailyCount> {
    val counts = activityDays.groupingBy { it }.eachCount()
    return (days - 1 downTo 0).map { back -> (today - back).let { DailyCount(it, counts[it] ?: 0) } }
}

/** 연속 학습일. 오늘 기록이 없으면 어제부터 센다 (오늘 아직 안 했어도 끊기지 않게). */
fun studyStreak(activityDays: Set<Long>, today: Long): Int {
    var day = if (today in activityDays) today else today - 1
    var streak = 0
    while (day in activityDays) { streak++; day-- }
    return streak
}

/** 묶음별 오답 수 상위 [limit]개 (키, 합계), 많은 순 → 같으면 키 순. */
fun <K : Comparable<K>> topByCount(pairs: List<Pair<K, Int>>, limit: Int = 5): List<Pair<K, Int>> =
    pairs.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }.toList()
        .filter { it.second > 0 }.sortedWith(compareByDescending<Pair<K, Int>> { it.second }.thenBy { it.first }).take(limit)
