package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    @Test fun recentDaysFillEmptyDaysWithZero() {
        val counts = recentDailyCounts(listOf(100, 100, 98, 90), today = 100)
        assertEquals((94L..100L).toList(), counts.map { it.epochDay })
        assertEquals(listOf(0, 0, 0, 0, 1, 0, 2), counts.map { it.count })
    }

    @Test fun streakCountsFromTodayOrYesterday() {
        assertEquals(3, studyStreak(setOf(100, 99, 98, 96), today = 100))
        assertEquals(2, studyStreak(setOf(99, 98), today = 100))
        assertEquals(0, studyStreak(setOf(97), today = 100))
        assertEquals(0, studyStreak(emptySet(), today = 100))
    }

    @Test fun topByCountSumsAndSorts() {
        assertEquals(listOf("c4" to 5, "c1" to 2, "c2" to 2), topByCount(listOf("c1" to 2, "c4" to 3, "c4" to 2, "c2" to 2, "c3" to 0)))
    }
}
