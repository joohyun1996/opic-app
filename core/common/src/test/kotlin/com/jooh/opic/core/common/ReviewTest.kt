package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewTest {
    private val today = 20_000L

    @Test fun practiceMissStartsAtStageOneTomorrow() {
        assertEquals(ReviewSchedule(1, today + 1), scheduleAfterPractice(today))
    }

    @Test fun firstTryCorrectMovesUpOneThreeSevenThenGraduates() {
        assertEquals(ReviewSchedule(2, today + 3), scheduleAfterReview(1, true, today))
        assertEquals(ReviewSchedule(3, today + 7), scheduleAfterReview(2, true, today))
        assertNull(scheduleAfterReview(3, true, today))
    }

    @Test fun anyMissGoesBackToStageOneTomorrow() {
        for (stage in 1..3) assertEquals(ReviewSchedule(1, today + 1), scheduleAfterReview(stage, false, today))
    }
}
