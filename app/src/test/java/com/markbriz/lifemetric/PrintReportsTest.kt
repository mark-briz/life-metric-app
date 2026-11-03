package com.markbriz.lifemetric

import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.UserProfile
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.domain.WeightChangeDirection
import com.markbriz.lifemetric.domain.buildBloodPressurePrintTable
import com.markbriz.lifemetric.domain.buildWeightPrintTable
import com.markbriz.lifemetric.domain.buildWorkoutPrintTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrintReportsTest {
    @Test
    fun bloodPressurePrintTableGroupsOneRowPerDay() {
        val readings = listOf(
            BloodPressureReading("old", "2026-09-01", "07:00", 142, 92, 70),
            BloodPressureReading("new", "2026-10-28", "20:50", 131, 82, 68),
            BloodPressureReading("no-hr", "2026-10-29", "08:05", 118, 76, null),
        )
        val table = buildBloodPressurePrintTable(readings, "Oct 29, 2026 9:00 PM", deviceName = "Mark's Pixel")
        assertEquals("Life Metric - Blood pressure personal log", table.title)
        assertEquals(listOf("Mark's Pixel", "Generated Oct 29, 2026 9:00 PM"), table.lines)
        assertEquals(3, table.rows.size)
        assertEquals("10/29/26", table.rows[0].date)
        assertEquals("118/76", table.rows[0].amPressure)
        assertEquals("", table.rows[0].amHeartRate)
        assertEquals("", table.rows[0].pmPressure)
        assertEquals("", table.rows[0].pmHeartRate)
        assertEquals("10/28/26", table.rows[1].date)
        assertEquals("", table.rows[1].amPressure)
        assertEquals("131/82", table.rows[1].pmPressure)
        assertEquals("68", table.rows[1].pmHeartRate)
        assertEquals("09/01/26", table.rows[2].date)
        assertEquals("142/92", table.rows[2].amPressure)
        assertEquals("70", table.rows[2].amHeartRate)
        assertEquals("", table.rows[2].pmPressure)
    }

    @Test
    fun bloodPressurePrintTableUsesLowestMorningAndPmReadings() {
        val readings = listOf(
            BloodPressureReading("am-high", "2026-10-29", "07:00", 130, 80, 70, period = "morning"),
            BloodPressureReading("am-low", "2026-10-29", "08:00", 118, 76, null, period = "morning"),
            BloodPressureReading("am-mid", "2026-10-29", "09:00", 140, 90, 72, period = "morning"),
            BloodPressureReading("pm-high", "2026-10-29", "18:00", 135, 85, 80, period = "evening"),
            BloodPressureReading("pm-low-sys", "2026-10-29", "20:00", 131, 90, 90, period = "evening"),
            BloodPressureReading("pm-low", "2026-10-29", "21:00", 131, 82, 68, period = "evening"),
            BloodPressureReading("other-pm", "2026-10-28", "13:00", 125, 78, 60, period = "other"),
        )
        val table = buildBloodPressurePrintTable(readings, "Oct 29, 2026 9:00 PM")
        assertEquals(2, table.rows.size)
        assertEquals("10/29/26", table.rows[0].date)
        assertEquals("118/76", table.rows[0].amPressure)
        assertEquals("", table.rows[0].amHeartRate)
        assertEquals("131/82", table.rows[0].pmPressure)
        assertEquals("68", table.rows[0].pmHeartRate)
        assertEquals("10/28/26", table.rows[1].date)
        assertEquals("", table.rows[1].amPressure)
        assertEquals("125/78", table.rows[1].pmPressure)
        assertEquals("60", table.rows[1].pmHeartRate)
    }

    @Test
    fun weightPrintTableUsesShortDateTimeAndDirection() {
        val records = listOf(
            WeightRecord("a", "2026-10-27", "07:00", 193.0),
            WeightRecord("b", "2026-10-28", "06:33", 192.2),
            WeightRecord("c", "2026-10-29", "06:40", 192.3),
        )
        val table = buildWeightPrintTable(records, UserProfile(), "Oct 29, 2026 9:00 PM", deviceName = "Pixel 8")
        assertEquals("Life Metric — Weight history", table.title)
        assertEquals(listOf("Pixel 8", "Generated Oct 29, 2026 9:00 PM", "3 readings · lb"), table.lines)
        assertEquals(3, table.rows.size)
        assertEquals("10/29/26", table.rows[0].date)
        assertEquals("06:40", table.rows[0].time)
        assertEquals("192.3 lb", table.rows[0].weight)
        assertEquals(WeightChangeDirection.Unchanged, table.rows[0].direction)
        assertEquals("10/28/26", table.rows[1].date)
        assertEquals("06:33", table.rows[1].time)
        assertEquals("192.2 lb", table.rows[1].weight)
        assertEquals(WeightChangeDirection.Down, table.rows[1].direction)
        assertEquals("10/27/26", table.rows[2].date)
        assertNull(table.rows[2].direction)
    }

    @Test
    fun workoutPrintTableOneRowPerDayWithCategoryTotals() {
        val plan = FitnessPlan(
            id = "plan-1",
            name = "Indoor Bike",
            activityTypeId = "cycling",
            category = "cardio",
            machines = listOf(Machine("m-bike", "Indoor bike")),
        )
        val lift = FitnessPlan(
            id = "plan-2",
            name = "Upper Body Strength",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("m-bench", "Chest Press")),
        )
        val workouts = listOf(
            Workout(
                "w-bike",
                "2026-10-28",
                "plan-1",
                "cycling",
                listOf(
                    WorkoutEntry(
                        "e-bike",
                        "plan-1",
                        "m-bike",
                        schema = "distance_time",
                        distanceMi = 12.4,
                        durationSec = 2400,
                    ),
                ),
                durationSec = 2460,
            ),
            Workout(
                "w-lift-a",
                "2026-10-28",
                "plan-2",
                "strength_training",
                listOf(
                    WorkoutEntry(
                        "e-lift-a",
                        "plan-2",
                        "m-bench",
                        schema = "load",
                        weight = 135.0,
                        repetitions = 8,
                        setKind = "working",
                    ),
                    WorkoutEntry(
                        "e-warmup",
                        "plan-2",
                        "m-bench",
                        schema = "load",
                        weight = 95.0,
                        repetitions = 10,
                        setKind = "warmup",
                    ),
                ),
            ),
            Workout(
                "w-lift-b",
                "2026-10-27",
                "plan-2",
                "strength_training",
                listOf(
                    WorkoutEntry(
                        "e-lift-b",
                        "plan-2",
                        "m-bench",
                        schema = "load",
                        weight = 100.0,
                        repetitions = 5,
                        setKind = "working",
                    ),
                ),
            ),
        )
        val table = buildWorkoutPrintTable(
            workouts,
            listOf(plan, lift),
            UserProfile(),
            "Oct 28, 2026 9:00 PM",
            deviceName = "Galaxy S24",
        )
        assertEquals("Life Metric — Workout history", table.title)
        assertEquals("Galaxy S24", table.lines[0])
        assertTrue(table.lines[1].contains("Generated Oct 28, 2026 9:00 PM"))
        assertEquals(listOf("Strength (lb)", "Cardio (min)"), table.columns)
        assertEquals(2, table.rows.size)
        assertEquals("10/28/26", table.rows[0].date)
        assertEquals("1,080 lb", table.rows[0].cells[0])
        assertEquals("40", table.rows[0].cells[1])
        assertEquals("10/27/26", table.rows[1].date)
        assertEquals("500 lb", table.rows[1].cells[0])
        assertEquals("", table.rows[1].cells[1])
    }
}
