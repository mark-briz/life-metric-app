package com.markbriz.lifemetric

import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.domain.DailyWeight
import com.markbriz.lifemetric.domain.WeekBucket
import com.markbriz.lifemetric.domain.WeightChartModel
import com.markbriz.lifemetric.domain.describeBloodPressureChart
import com.markbriz.lifemetric.domain.describeWeekBars
import com.markbriz.lifemetric.domain.describeWeightChart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDescriptionsTest {
    @Test
    fun bloodPressureChartNamesRangeAndLatest() {
        val readings = listOf(
            BloodPressureReading("a", "2026-10-20", "07:00", 118, 74),
            BloodPressureReading("b", "2026-10-22", "07:00", 131, 84),
            BloodPressureReading("c", "2026-10-28", "20:00", 124, 78),
        )
        assertEquals(
            "Blood pressure, last 3 readings. Systolic 118 to 131, latest 124. Diastolic 74 to 84, latest 78.",
            describeBloodPressureChart(readings),
        )
    }

    @Test
    fun weightChartIncludesTrendAndGoal() {
        val model = WeightChartModel(
            points = listOf(
                DailyWeight("2026-10-20", 184.0, "w1"),
                DailyWeight("2026-10-28", 182.4, "w2"),
            ),
            startDate = "2026-10-20",
            endDate = "2026-10-28",
            trendByDate = mapOf("2026-10-20" to 184.0, "2026-10-28" to 183.2),
        )
        val text = describeWeightChart(model, goalLb = 175.0, useKg = false)
        assertTrue(text, text.startsWith("Weight, "))
        assertTrue(text, text.contains("Daily lows 182.4 lb to 184.0 lb."))
        assertTrue(text, text.contains("7-day trend latest 183.2 lb."))
        assertTrue(text, text.contains("Goal 175.0 lb."))
    }

    @Test
    fun weekBarsListEachWeekAndTotal() {
        val weeks = listOf(WeekBucket("Oct 7", 3, 1000.0), WeekBucket("Oct 14", 2, 800.0))
        assertEquals(
            "Sessions per week, last 2 weeks. Oct 7: 3, Oct 14: 2. Total 5.",
            describeWeekBars(weeks, "Sessions") { it.count },
        )
    }
}
