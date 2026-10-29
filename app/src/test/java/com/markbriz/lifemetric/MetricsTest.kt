package com.markbriz.lifemetric

import com.markbriz.lifemetric.domain.BloodPressureChartRange
import com.markbriz.lifemetric.domain.BloodPressurePeriod
import com.markbriz.lifemetric.domain.averageBloodPressureSamples
import com.markbriz.lifemetric.domain.bloodPressureDelta
import com.markbriz.lifemetric.domain.bloodPressureGaugeProgress
import com.markbriz.lifemetric.domain.bloodPressureInLastDays
import com.markbriz.lifemetric.domain.bloodPressureLowHint
import com.markbriz.lifemetric.domain.bloodPressurePeriod
import com.markbriz.lifemetric.domain.bloodPressureSaveHint
import com.markbriz.lifemetric.domain.bloodPressureSaveWarning
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.bloodPressureVisitSummary
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.BloodPressureTag
import com.markbriz.lifemetric.domain.buildBloodPressureCsv
import com.markbriz.lifemetric.domain.buildBloodPressureReport
import com.markbriz.lifemetric.domain.chartBloodPressure
import com.markbriz.lifemetric.domain.filterBloodPressureReadings
import com.markbriz.lifemetric.domain.formatBloodPressureHistoryLine
import com.markbriz.lifemetric.domain.formatBloodPressureReminders
import com.markbriz.lifemetric.domain.formatBloodPressureTags
import com.markbriz.lifemetric.domain.formatVisitSummaryLine
import com.markbriz.lifemetric.domain.inferBloodPressurePeriod
import com.markbriz.lifemetric.domain.isolatedBloodPressureNote
import com.markbriz.lifemetric.domain.meetsBloodPressureTarget
import com.markbriz.lifemetric.domain.validateBloodPressureTarget
import com.markbriz.lifemetric.data.BloodPressureSample
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.formatBloodPressure
import com.markbriz.lifemetric.domain.formatBloodPressureDelta
import com.markbriz.lifemetric.domain.formatDerivedBloodPressure
import com.markbriz.lifemetric.domain.formatPreviousBloodPressure
import com.markbriz.lifemetric.domain.meanArterialPressure
import com.markbriz.lifemetric.domain.pulsePressure
import com.markbriz.lifemetric.domain.validateBloodPressureReading
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.domain.bmiCategory
import com.markbriz.lifemetric.domain.BmiCategory
import com.markbriz.lifemetric.domain.formatBmiCaption
import com.markbriz.lifemetric.domain.latestWeightTrend
import com.markbriz.lifemetric.domain.progressChangeLb
import com.markbriz.lifemetric.domain.targetChangeLb
import com.markbriz.lifemetric.domain.validateWeightLb
import com.markbriz.lifemetric.domain.weightChangeDirection
import com.markbriz.lifemetric.domain.WeightChangeDirection
import com.markbriz.lifemetric.domain.weightGoalKind
import com.markbriz.lifemetric.domain.WeightGoalKind
import com.markbriz.lifemetric.domain.weightTrendDelta
import com.markbriz.lifemetric.domain.WeightChartRange
import com.markbriz.lifemetric.domain.dailyWeights
import com.markbriz.lifemetric.domain.formatTenPercentGoalHint
import com.markbriz.lifemetric.domain.formatWeightGoalEta
import com.markbriz.lifemetric.domain.formatWeightReminder
import com.markbriz.lifemetric.domain.homeWeightChartModel
import com.markbriz.lifemetric.domain.tenPercentLossTarget
import com.markbriz.lifemetric.domain.weightChartModel
import com.markbriz.lifemetric.domain.weightGoalEtaDate
import com.markbriz.lifemetric.domain.weightWeeklyRate
import com.markbriz.lifemetric.domain.WeightTag
import com.markbriz.lifemetric.domain.buildWeightCsv
import com.markbriz.lifemetric.domain.buildWeightReport
import com.markbriz.lifemetric.domain.displayWeight
import com.markbriz.lifemetric.domain.filterWeightRecords
import com.markbriz.lifemetric.domain.formatWaistCaption
import com.markbriz.lifemetric.domain.formatWeightHistoryLine
import com.markbriz.lifemetric.domain.formatWeightTags
import com.markbriz.lifemetric.domain.formatWeightWithUnit
import com.markbriz.lifemetric.domain.inferWeightTag
import com.markbriz.lifemetric.domain.parseWeightInput
import com.markbriz.lifemetric.domain.stepDisplayedWeight
import com.markbriz.lifemetric.domain.validateWaistInches
import com.markbriz.lifemetric.domain.weightRecordTags
import com.markbriz.lifemetric.domain.emptyWorkoutEntry
import com.markbriz.lifemetric.domain.activityPersonalRecord
import com.markbriz.lifemetric.domain.buildWorkoutCsv
import com.markbriz.lifemetric.domain.epleyOneRepMaxLb
import com.markbriz.lifemetric.domain.filterWorkouts
import com.markbriz.lifemetric.domain.findPreviousEntry
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatActivityRecord
import com.markbriz.lifemetric.domain.formatActivityVsLastTime
import com.markbriz.lifemetric.domain.formatLiftWeight
import com.markbriz.lifemetric.domain.formatPreviousActivitySuggestion
import com.markbriz.lifemetric.domain.formatPreviousLoadSuggestion
import com.markbriz.lifemetric.domain.formatRestClock
import com.markbriz.lifemetric.domain.formatVolume
import com.markbriz.lifemetric.domain.formatVolumeTrendCaption
import com.markbriz.lifemetric.domain.formatWorkoutChipLine
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.formatWorkoutReminder
import com.markbriz.lifemetric.domain.formatWorkoutRestTimer
import com.markbriz.lifemetric.domain.formatWorkoutWeekCaption
import com.markbriz.lifemetric.domain.formatWorkoutWeekLine
import com.markbriz.lifemetric.domain.sessionVolumeLb
import com.markbriz.lifemetric.domain.summarizeWorkout
import com.markbriz.lifemetric.domain.workoutSessionsByWeek
import com.markbriz.lifemetric.domain.workoutWeekSummary
import com.markbriz.lifemetric.domain.WorkoutHistoryRange
import com.markbriz.lifemetric.domain.hollowWorkoutEntryError
import com.markbriz.lifemetric.domain.isHollowWorkoutEntry
import com.markbriz.lifemetric.domain.lastUsedMachineId
import com.markbriz.lifemetric.domain.lastUsedPlanId
import com.markbriz.lifemetric.domain.latestWorkout
import com.markbriz.lifemetric.domain.parseLiftWeightInput
import com.markbriz.lifemetric.domain.PreviousLoad
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.summarizeWorkoutEntries
import com.markbriz.lifemetric.domain.trailingMovingAverage
import com.markbriz.lifemetric.domain.validateProfile
import com.markbriz.lifemetric.domain.weightGoalProgress
import com.markbriz.lifemetric.domain.withParsedMetrics
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.EntryMetrics
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsTest {
    @Test
    fun bloodPressureCategoriesMatchGuidelines() {
        assertEquals(BpStatus.Normal, bloodPressureStatus(119, 79))
        assertEquals(BpStatus.Elevated, bloodPressureStatus(120, 79))
        assertEquals(BpStatus.Elevated, bloodPressureStatus(129, 79))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(120, 80))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(130, 79))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(139, 89))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(140, 79))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(139, 90))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(180, 120))
        assertEquals(BpStatus.Severe, bloodPressureStatus(181, 100))
        assertEquals(BpStatus.Severe, bloodPressureStatus(140, 121))
        assertEquals(
            listOf(
                "Normal",
                "Elevated",
                "Stage 1 Hypertension",
                "Stage 2 Hypertension",
                "Severe Hypertension",
            ),
            BpStatus.explained.map { it.label },
        )
        assertEquals("Less than 120 and less than 80", BpStatus.Normal.range)
        assertEquals("120–129 and less than 80", BpStatus.Elevated.range)
        assertEquals("130–139 or 80–89", BpStatus.Stage1.range)
        assertEquals("140 or higher or 90 or higher", BpStatus.Stage2.range)
        assertEquals("Higher than 180 and/or higher than 120", BpStatus.Severe.range)
    }

    @Test
    fun pulsePressureAndMapUseStandardFormulas() {
        assertEquals(42, pulsePressure(118, 76))
        assertEquals(90, meanArterialPressure(118, 76))
        assertEquals("PP 42 · MAP 90", formatDerivedBloodPressure(118, 76))
        assertEquals("118 / 76 mmHg", formatBloodPressure(118, 76, null))
        assertEquals("118 / 76 mmHg · 64 bpm", formatBloodPressure(118, 76, 64))
        assertEquals("Last time: 128 / 78 · 64 bpm.", formatPreviousBloodPressure(128, 78, 64))
        assertEquals("Last time: 128 / 78.", formatPreviousBloodPressure(128, 78, null))
    }

    @Test
    fun bloodPressureDeltaUsesNewestThenPrevious() {
        val readings = listOf(
            BloodPressureReading("bp-1", "2026-10-01", "08:00", 120, 80, 60),
            BloodPressureReading("bp-2", "2026-10-02", "08:00", 126, 82, null),
        )
        val delta = bloodPressureDelta(readings)
        assertEquals(6, delta?.systolic)
        assertEquals(2, delta?.diastolic)
        assertEquals("+6 / +2 mmHg vs prior", formatBloodPressureDelta(delta!!))
        assertEquals(
            "No change vs prior",
            formatBloodPressureDelta(com.markbriz.lifemetric.domain.BloodPressureDelta(0, 0)),
        )
        assertNull(bloodPressureDelta(readings.take(1)))
    }

    @Test
    fun bloodPressureValidationAcceptsOptionalHeartRate() {
        val ok = validateBloodPressureReading("2026-10-02", "08:16", "118", "76", "")
        assertTrue(ok.ok)
        assertEquals(118, ok.systolic)
        assertEquals(76, ok.diastolic)
        assertNull(ok.heartRate)
        assertTrue(ok.errors.isEmpty())

        val withHr = validateBloodPressureReading("2026-10-02", "08:16", "118", "76", "64")
        assertEquals(64, withHr.heartRate)

        val low = validateBloodPressureReading("2026-10-02", "08:16", "40", "20", "10")
        assertEquals("Systolic must be 50–250.", low.errors["systolic"])
        assertEquals("Diastolic must be 30–150.", low.errors["diastolic"])
        assertEquals("Heart rate must be 30–220.", low.errors["heartRate"])

        val inverted = validateBloodPressureReading("2026-10-02", "08:16", "110", "120", "")
        assertEquals("Diastolic should be lower than systolic.", inverted.errors["diastolic"])
    }

    @Test
    fun saveWarningCoversSevereAndWidePulsePressure() {
        assertNull(bloodPressureSaveHint(118, 76))
        assertTrue(bloodPressureSaveHint(181, 90)!!.contains("Severe Hypertension"))
        assertTrue(bloodPressureSaveHint(160, 70)!!.contains("Pulse pressure is 90"))
        assertTrue(bloodPressureSaveWarning(181, 90)!!.contains("Save it anyway?"))
        assertTrue(bloodPressureSaveWarning(160, 70)!!.contains("Save this reading?"))
    }

    @Test
    fun gaugeFollowsCategoryNotSystolicAlone() {
        assertEquals(BpStatus.Stage1, bloodPressureStatus(118, 82))
        assertEquals(BpStatus.Normal, bloodPressureStatus(118, 76))
        val diastolicStage1 = bloodPressureGaugeProgress(118, 82)
        val normal = bloodPressureGaugeProgress(118, 76)
        val systolicStage2 = bloodPressureGaugeProgress(148, 72)
        assertTrue(diastolicStage1 > 0f)
        assertTrue(normal > 0f)
        assertTrue(systolicStage2 > 0f)
        assertEquals(BpStatus.Stage2, bloodPressureStatus(148, 72))
    }

    @Test
    fun morningEveningComeFromTimeUnlessOverridden() {
        assertEquals(BloodPressurePeriod.Morning, inferBloodPressurePeriod("07:14"))
        assertEquals(BloodPressurePeriod.Other, inferBloodPressurePeriod("13:00"))
        assertEquals(BloodPressurePeriod.Evening, inferBloodPressurePeriod("18:30"))
        val inferred = BloodPressureReading("bp-1", "2026-10-27", "07:14", 118, 76)
        val evening = inferred.copy(period = "evening")
        assertEquals(BloodPressurePeriod.Morning, bloodPressurePeriod(inferred))
        assertEquals(BloodPressurePeriod.Evening, bloodPressurePeriod(evening))
    }

    @Test
    fun pairAverageUsesBothCuffReadings() {
        val average = averageBloodPressureSamples(
            listOf(
                BloodPressureSample(128, 80, 64),
                BloodPressureSample(132, 78, 68),
            ),
        )
        assertEquals(130, average?.systolic)
        assertEquals(79, average?.diastolic)
        assertEquals(66, average?.heartRate)
    }

    @Test
    fun visitSummaryUsesSevenDayAverageNotASingleReading() {
        val readings = listOf(
            BloodPressureReading("bp-old", "2026-10-01", "08:00", 160, 100, period = "morning"),
            BloodPressureReading("bp-am", "2026-10-26", "08:00", 128, 82, 60, period = "morning"),
            BloodPressureReading("bp-pm", "2026-10-26", "18:00", 118, 74, 58, period = "evening"),
            BloodPressureReading("bp-today", "2026-10-27", "08:00", 124, 80, period = "morning"),
        )
        assertEquals(3, bloodPressureInLastDays(readings, 7, "2026-10-27").size)
        val summary = bloodPressureVisitSummary(readings, 130, 80, "2026-10-27")
        assertEquals(3, summary?.count)
        assertEquals(123, summary?.meanSystolic)
        assertEquals(79, summary?.meanDiastolic)
        assertEquals(BpStatus.Elevated, summary?.status)
        assertEquals(126, summary?.morning?.systolic)
        assertEquals(118, summary?.evening?.systolic)
        assertEquals(2, summary?.target?.last7Met)
        assertEquals(3, summary?.target?.last7Total)
        assertTrue(formatVisitSummaryLine(summary!!).startsWith("7-day average is Elevated"))
        assertTrue(meetsBloodPressureTarget(readings[3], 130, 80))
        assertEquals(4, chartBloodPressure(readings, BloodPressureChartRange.Last7).size)
        assertEquals(4, chartBloodPressure(readings, BloodPressureChartRange.All).size)
    }

    @Test
    fun lowHintDoesNotInventACategory() {
        assertEquals(BpStatus.Normal, bloodPressureStatus(88, 58))
        assertEquals(
            "This is lower than the usual adult range. The AHA chart is for high blood pressure.",
            bloodPressureLowHint(88, 58),
        )
        assertEquals(
            "This is lower than the usual adult range. The AHA chart is for high blood pressure.",
            bloodPressureLowHint(88, 70),
        )
        assertEquals(
            "This is lower than the usual adult range. The AHA chart is for high blood pressure.",
            bloodPressureLowHint(100, 55),
        )
        assertNull(bloodPressureLowHint(118, 76))
    }

    @Test
    fun isolatedNoteOnlyWhenOneNumberDrivesTheCategory() {
        assertEquals(
            "Systolic is in Stage 2 Hypertension; diastolic is not.",
            isolatedBloodPressureNote(148, 72),
        )
        assertEquals(
            "Diastolic is in Stage 1 Hypertension; systolic is not.",
            isolatedBloodPressureNote(118, 82),
        )
        assertNull(isolatedBloodPressureNote(140, 90))
        assertNull(isolatedBloodPressureNote(118, 76))
    }

    @Test
    fun tagsAndPeriodFiltersAreAnd() {
        val leftMorning = BloodPressureReading(
            "bp-1",
            "2026-10-27",
            "08:00",
            118,
            76,
            period = "morning",
            tags = listOf(BloodPressureTag.LeftArm.id),
        )
        val leftEvening = BloodPressureReading(
            "bp-2",
            "2026-10-27",
            "18:00",
            122,
            78,
            period = "evening",
            tags = listOf(BloodPressureTag.LeftArm.id, BloodPressureTag.AfterCaffeine.id),
            note = "coffee",
        )
        val rightMorning = BloodPressureReading(
            "bp-3",
            "2026-10-27",
            "07:30",
            124,
            80,
            period = "morning",
            tags = listOf(BloodPressureTag.RightArm.id),
        )
        val readings = listOf(leftMorning, leftEvening, rightMorning)
        assertEquals(
            listOf(leftMorning),
            filterBloodPressureReadings(readings, BloodPressurePeriod.Morning, listOf(BloodPressureTag.LeftArm.id)),
        )
        assertEquals(2, filterBloodPressureReadings(readings, tags = listOf(BloodPressureTag.LeftArm.id)).size)
        assertEquals(
            listOf(leftEvening),
            filterBloodPressureReadings(readings, tags = listOf(BloodPressureTag.LeftArm.id, BloodPressureTag.AfterCaffeine.id)),
        )
        assertEquals("Left arm, After caffeine", formatBloodPressureTags(leftEvening.tags))
        assertTrue(formatBloodPressureHistoryLine(leftEvening).contains("coffee"))
        assertTrue(formatBloodPressureHistoryLine(leftEvening).contains("Left arm"))
    }

    @Test
    fun clinicianReportIncludesAveragesAndTags() {
        val readings = listOf(
            BloodPressureReading(
                "bp-am",
                "2026-10-26",
                "08:00",
                128,
                82,
                60,
                period = "morning",
                tags = listOf(BloodPressureTag.LeftArm.id),
                note = "before meds",
            ),
            BloodPressureReading("bp-pm", "2026-10-26", "18:00", 118, 74, 58, period = "evening"),
        )
        val report = buildBloodPressureReport(readings, 130, 80, "Oct 27, 2026", "2026-10-27")
        assertTrue(report.contains("Last 7 days"))
        assertTrue(report.contains("Morning average"))
        assertTrue(report.contains("Evening average"))
        assertTrue(report.contains("Left arm"))
        assertTrue(report.contains("before meds"))
        assertTrue(report.contains("This is a personal log, not a diagnosis."))
        val csv = buildBloodPressureCsv(readings)
        assertTrue(csv.startsWith("date,time,systolic,diastolic,heartRate,period,tags,note"))
        assertTrue(csv.contains("left_arm"))
        assertTrue(csv.contains("before meds"))
        val escaped = buildBloodPressureCsv(
            listOf(readings[0].copy(note = "felt tired, after walk")),
        )
        assertTrue(escaped.contains("\"felt tired, after walk\""))
    }

    @Test
    fun reminderSummaryIsOffUntilEnabled() {
        assertEquals("Off", formatBloodPressureReminders(false, false, "07:00", "19:00"))
        val both = formatBloodPressureReminders(true, true, "07:00", "19:00")
        assertTrue(both.contains("Morning"))
        assertTrue(both.contains("Evening"))
    }

    @Test
    fun bloodPressureTargetAllowsClearing() {
        val empty = validateBloodPressureTarget("", "")
        assertTrue(empty.ok)
        assertNull(empty.systolic)
        val partial = validateBloodPressureTarget("130", "")
        assertEquals("Enter both systolic and diastolic, or clear both to remove the target.", partial.error)
        val ok = validateBloodPressureTarget("130", "80")
        assertEquals(130, ok.systolic)
        assertEquals(80, ok.diastolic)
    }

    @Test
    fun bmiUsesStandardFormula() {
        val bmi = calculateBmi(182.3, 70)
        assertEquals(BmiCategory.Over, bmiCategory(bmi))
    }

    @Test
    fun movingAverageUsesTrailingWindow() {
        val averages = trailingMovingAverage(listOf(2.0, 4.0, 6.0, 8.0), 2)
        assertEquals(listOf(2.0, 3.0, 5.0, 7.0), averages)
    }

    @Test
    fun durationAcceptsClockAndMinutes() {
        assertEquals(90, parseDurationInput("1:30"))
        assertEquals(120, parseDurationInput("2"))
        assertNull(parseDurationInput("1:70"))
    }

    @Test
    fun loadEntrySummary() {
        val summary = formatWorkoutEntrySummary(
            WorkoutEntry("e", "p", "m", schema = "load", weight = 135.0, repetitions = 10),
        )
        assertEquals("135 lb · 10 reps", summary)
    }

    @Test
    fun cyclingSummaryShowsOnlyMilesAndTime() {
        val entry = WorkoutEntry(
            "e",
            "p",
            "m",
            schema = "distance_time",
            distanceMi = 12.0,
            durationSec = 3600,
            avgSpeedMph = 16.5,
            avgRpm = 88,
            avgHeartRateBpm = 142,
            maxHeartRateBpm = 171,
        )
        assertEquals("12 mi · 1:00:00 · 5:00 / mi", formatWorkoutEntrySummary(entry))
        assertEquals("12 mi · 1:00:00 · 5:00 / mi", summarizeWorkoutEntries(listOf(entry)))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("mph"))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("rpm"))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("bpm"))
        val withResistance = entry.copy(resistance = 8.0)
        assertEquals("Resistance 8 · 12 mi · 1:00:00 · 5:00 / mi", formatWorkoutEntrySummary(withResistance))
        assertEquals("Resistance 8 · 12 mi · 1:00:00 · 5:00 / mi", summarizeWorkoutEntries(listOf(withResistance)))
        assertEquals(
            "Resistance 6–8 · 24 mi · 2:00:00 · 5:00 / mi",
            summarizeWorkoutEntries(listOf(withResistance, withResistance.copy(id = "e2", resistance = 6.0))),
        )
    }

    @Test
    fun blankOptionalMetricsDefaultToZero() {
        val entry = WorkoutEntry("e", "p", "m", schema = "distance_time", distanceMi = 5.0, durationSec = 1200)
        val (parsed, errors) = entry.withParsedMetrics(
            ActivityCatalog.entryMetrics("cycling"),
            mapOf(
                EntryMetrics.RESISTANCE to "",
                EntryMetrics.AVG_SPEED to "",
                EntryMetrics.AVG_RPM to "",
                EntryMetrics.AVG_HEART_RATE to "",
                EntryMetrics.MAX_HEART_RATE to "",
            ),
        )
        assertTrue(errors.isEmpty())
        assertEquals(0.0, parsed.resistance, 0.0)
        assertEquals(0.0, parsed.avgSpeedMph, 0.0)
        assertEquals(0, parsed.avgRpm)
        assertEquals(0, parsed.avgHeartRateBpm)
        assertEquals(0, parsed.maxHeartRateBpm)
    }

    @Test
    fun lastUsedPlanPrefersMostRecentWorkout() {
        val upper = FitnessPlan("plan-upper", "Upper", activityTypeId = "strength_training", category = "strength")
        val lower = FitnessPlan("plan-lower", "Lower", activityTypeId = "strength_training", category = "strength")
        val workouts = listOf(
            Workout("w1", "2026-10-01", "plan-upper", "strength_training"),
            Workout("w2", "2026-10-10", "plan-lower", "strength_training"),
        )
        assertEquals("plan-lower", lastUsedPlanId(workouts, listOf(upper, lower)))
        assertEquals("plan-upper", lastUsedPlanId(workouts, listOf(upper)))
        assertNull(lastUsedPlanId(workouts, emptyList()))
        assertEquals("plan-upper", lastUsedPlanId(emptyList(), listOf(upper, lower)))
    }

    @Test
    fun lastUsedActivityPrefersLatestSetOnThatPlan() {
        val machines = listOf(Machine("bench", "Bench"), Machine("row", "Row"))
        val older = Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(load("e1", "plan-upper", "bench", 135.0, 10)))
        val newer = Workout("w2", "2026-10-10", "plan-upper", "strength_training", listOf(load("e2", "plan-upper", "row", 90.0, 8)))
        assertEquals("row", lastUsedMachineId(listOf(older, newer), "plan-upper", machines))
        assertEquals("bench", lastUsedMachineId(emptyList(), "plan-upper", machines))
        assertNull(lastUsedMachineId(listOf(newer), "plan-upper", emptyList()))
    }

    @Test
    fun previousLoadSuggestionIsVisibleCopy() {
        assertEquals("Last time: 50 lb × 12.", formatPreviousLoadSuggestion(PreviousLoad(50.0, 12)))
        assertEquals("Last time: 22.7 kg × 12.", formatPreviousLoadSuggestion(PreviousLoad(50.0, 12), useKg = true))
    }

    @Test
    fun emptyTabataBlockUsesDefaults() {
        val plan = FitnessPlan(
            id = "plan-t",
            name = "Tabata",
            activityTypeId = "tabata",
            category = "conditioning",
            machines = listOf(Machine("block", "Main")),
        )
        val entry = emptyWorkoutEntry(plan)
        assertEquals("interval", entry.schema)
        assertEquals(8, entry.rounds)
        assertEquals(20, entry.workSec)
        assertEquals(10, entry.restSec)
    }

    @Test
    fun emptyCyclingSegmentStartsAtZero() {
        val plan = FitnessPlan(
            id = "plan-c",
            name = "Ride",
            activityTypeId = "cycling",
            category = "cardio",
            machines = listOf(Machine("machine-c", "Indoor")),
        )
        val entry = emptyWorkoutEntry(plan)
        assertEquals("distance_time", entry.schema)
        assertEquals(0.0, entry.distanceMi!!, 0.0)
        assertEquals(0, entry.durationSec)
        assertEquals(0.0, entry.avgSpeedMph, 0.0)
        assertEquals(0, entry.avgRpm)
        assertEquals(0.0, entry.resistance, 0.0)
    }

    @Test
    fun cyclingIsTheOnlyActivityWithOptionalMetricsForNow() {
        assertEquals(5, ActivityCatalog.entryMetrics("cycling").size)
        assertTrue(ActivityCatalog.entryMetrics("running").isEmpty())
        assertTrue(ActivityCatalog.entryMetrics("strength_training").isEmpty())
    }

    @Test
    fun profileRejectsOutOfRangeHeight() {
        val result = validateProfile("2", "0", "", 10)
        assertEquals("Height must be between 3'0\" and 8'0\".", result.heightFeetError)
    }

    @Test
    fun previousLoadUsesMostRecentSetWithTheSameActivityName() {
        val upper = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench-a", "Chest Press"), Machine("row", "Seated Row")),
        )
        val lower = FitnessPlan(
            id = "plan-lower",
            name = "Lower",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench-b", "chest press")),
        )
        val older = Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(
            load("e1", "plan-upper", "bench-a", 135.0, 10),
            load("e3", "plan-upper", "row", 90.0, 12),
        ))
        val newer = Workout("w2", "2026-10-10", "plan-upper", "strength_training", listOf(
            load("e2", "plan-upper", "bench-a", 145.0, 8),
        ))
        val otherPlan = Workout("w3", "2026-10-12", "plan-lower", "strength_training", listOf(
            load("e4", "plan-lower", "bench-b", 155.0, 6),
        ))

        val previous = findPreviousLoad(
            workouts = listOf(older, newer, otherPlan),
            plans = listOf(upper, lower),
            activityId = "bench-a",
            currentWorkoutDate = "2026-10-19",
            currentEntries = emptyList(),
        )
        assertEquals(155.0, previous!!.weight, 0.0)
        assertEquals(6, previous.repetitions)
    }

    @Test
    fun previousLoadPrefersLaterSetOnSameDayAndIncludesCurrentWorkout() {
        val plan = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench", "Bench")),
        )
        val first = load("e1", "plan-upper", "bench", 135.0, 10)
        val second = load("e2", "plan-upper", "bench", 140.0, 8)
        val previous = findPreviousLoad(
            workouts = emptyList(),
            plans = listOf(plan),
            activityId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = listOf(first, second),
        )
        assertEquals(140.0, previous!!.weight, 0.0)
        assertEquals(8, previous.repetitions)
    }

    @Test
    fun previousLoadIgnoresTheEntryBeingEdited() {
        val plan = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench", "Bench")),
        )
        val saved = load("e1", "plan-upper", "bench", 155.0, 6)
        val editing = load("e2", "plan-upper", "bench", 40.0, 20)
        val previous = findPreviousLoad(
            workouts = listOf(Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(saved))),
            plans = listOf(plan),
            activityId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = listOf(editing),
            currentWorkoutId = "w-now",
            excludeEntryId = "e2",
        )
        assertEquals(155.0, previous!!.weight, 0.0)
        assertEquals(6, previous.repetitions)
    }

    @Test
    fun weightGoalProgressUsesStartAndLatestTowardTarget() {
        val start = WeightRecord("start", "2026-09-01", "08:00", 180.0)
        val later = WeightRecord("now", "2026-10-19", "08:00", 176.0)
        val progress = weightGoalProgress(listOf(start, later), "start", 170.0)!!
        assertEquals(10.0, progress.targetToLoseLb, 0.0)
        assertEquals(4.0, progress.weightLostLb, 0.0)
        assertEquals(40.0, progress.percentComplete, 0.01)
        assertEquals(WeightGoalKind.Lose, progress.kind)
        assertEquals(10.0, progress.targetChangeLb(), 0.0)
        assertEquals(4.0, progress.progressChangeLb(), 0.0)
    }

    @Test
    fun catalogUsesVocabularyLabelsAndEverySchema() {
        assertEquals(setOf("load", "distance_time", "duration", "interval", "sport", "note"), ActivityCatalog.types.map { it.schema }.toSet())
        assertEquals(6, ActivityCatalog.families.size)
        assertEquals("Add set", ActivityCatalog.addEntryLabel("load"))
        assertEquals("Add segment", ActivityCatalog.addEntryLabel("distance_time"))
        assertEquals("Add block", ActivityCatalog.addEntryLabel("interval"))
        assertEquals("Add note", ActivityCatalog.addEntryLabel("note"))
        assertEquals("Add time", ActivityCatalog.addEntryLabel("duration"))
        assertEquals("Add time", ActivityCatalog.addEntryLabel("sport"))
        assertEquals("Edit time", ActivityCatalog.editEntryLabel("sport"))
        assertEquals("sets", ActivityCatalog.entryNoun("load", 2))
        assertEquals("segment", ActivityCatalog.entryNoun("distance_time", 1))
        assertEquals("time entries", ActivityCatalog.entryNoun("duration", 2))
        val plans = ActivityCatalog.starterPlans()
        assertEquals(listOf("Upper Body Strength", "Lower Body Focus"), plans.map { it.name })
        assertEquals(listOf("Triceps Extension", "Biceps Curl"), plans[0].machines.take(2).map { it.name })
        assertEquals(9, plans[0].machines.size)
        assertEquals(3, plans[1].machines.size)
        assertTrue(plans.all { it.activityTypeId == "strength_training" })
    }

    @Test
    fun emptyWorkoutSummaryAvoidsEntryWording() {
        assertEquals("Nothing logged", summarizeWorkoutEntries(emptyList()))
        assertEquals(
            "2 sets · 1,460 lb · best 110 × 6",
            summarizeWorkoutEntries(listOf(
                load("e1", "p", "m", 100.0, 8),
                load("e2", "p", "m", 110.0, 6),
            )),
        )
    }

    @Test
    fun hollowSetsAreRejectedAndSkippedForLastLoad() {
        assertTrue(isHollowWorkoutEntry(load("e0", "p", "m", 0.0, 0)))
        assertTrue(!isHollowWorkoutEntry(load("e1", "p", "m", 0.0, 10)))
        assertTrue(!isHollowWorkoutEntry(load("e2", "p", "m", 135.0, 0)))
        assertEquals("weight" to "Enter a weight and repetitions.", hollowWorkoutEntryError(load("e0", "p", "m", 0.0, 0)))
        assertEquals(
            "distanceMi" to "Enter a distance or time.",
            hollowWorkoutEntryError(WorkoutEntry("d", "p", "m", schema = "distance_time", distanceMi = 0.0, durationSec = 0)),
        )
        assertEquals(
            "durationSec" to "Enter minutes greater than 0.",
            hollowWorkoutEntryError(WorkoutEntry("t", "p", "m", schema = "duration", durationSec = 0)),
        )
        assertEquals(
            "rounds" to "Enter a whole number of rounds.",
            hollowWorkoutEntryError(WorkoutEntry("i", "p", "m", schema = "interval", rounds = 0)),
        )
        val plan = FitnessPlan("p", "Upper", activityTypeId = "strength_training", category = "strength", machines = listOf(Machine("m", "Press")))
        val hollow = Workout("old", "2026-10-01", "p", "strength_training", listOf(load("z", "p", "m", 0.0, 0)))
        val real = Workout("now", "2026-10-10", "p", "strength_training", listOf(load("a", "p", "m", 135.0, 8)))
        assertEquals(PreviousLoad(135.0, 8), findPreviousLoad(listOf(hollow, real), listOf(plan), "m", "2026-10-12", emptyList()))
    }

    @Test
    fun liftDisplayKeepsStoredPoundsWhenUnchanged() {
        assertEquals("61.2", formatLiftWeight(135.0, useKg = true))
        assertEquals(135.0, parseLiftWeightInput("61.2", useKg = true, previousLb = 135.0)!!, 0.0)
        assertEquals(8_400.0, com.markbriz.lifemetric.domain.sessionVolumeLb(listOf(
            load("e1", "p", "m", 210.0, 10),
            load("e2", "p", "m", 210.0, 10),
            load("e3", "p", "m", 210.0, 10),
            load("e4", "p", "m", 210.0, 10),
        )), 0.0)
        assertEquals("8,400 lb", formatVolume(8400.0))
        val plan = FitnessPlan("p", "Upper Body Strength", activityTypeId = "strength_training", category = "strength")
        val workout = Workout("w", "2026-10-10", "p", "strength_training", listOf(load("e1", "p", "m", 135.0, 8)))
        assertEquals("Upper · 1 set · 1,080 lb", formatWorkoutChipLine(workout, plan))
        assertEquals("w2", latestWorkout(listOf(
            Workout("w1", "2026-10-01", "p", "strength_training"),
            Workout("w2", "2026-10-10", "p", "strength_training"),
        ))?.id)
    }

    @Test
    fun workoutWeekCountsStrengthDaysAndAerobicMinutes() {
        val lift = FitnessPlan("p-lift", "Upper", activityTypeId = "strength_training", category = "strength")
        val walk = FitnessPlan("p-walk", "Walk", activityTypeId = "walking", category = "cardio")
        val hiit = FitnessPlan("p-hiit", "HIIT", activityTypeId = "hiit", category = "conditioning")
        val workouts = listOf(
            Workout("mon", "2026-10-26", "p-lift", "strength_training", listOf(load("e1", "p-lift", "m", 135.0, 8))),
            Workout("wed", "2026-10-28", "p-lift", "strength_training", listOf(load("e2", "p-lift", "m", 145.0, 6))),
            Workout(
                "tue",
                "2026-10-27",
                "p-walk",
                "walking",
                listOf(WorkoutEntry("e3", "p-walk", "m", schema = "distance_time", distanceMi = 3.0, durationSec = 3600)),
            ),
            Workout(
                "thu",
                "2026-10-29",
                "p-hiit",
                "hiit",
                listOf(WorkoutEntry("e4", "p-hiit", "m", schema = "interval", rounds = 8, workSec = 20, restSec = 10)),
            ),
            Workout("old", "2026-10-19", "p-lift", "strength_training", listOf(load("e5", "p-lift", "m", 100.0, 10))),
        )
        val week = workoutWeekSummary(workouts, listOf(lift, walk, hiit), today = "2026-10-27")
        assertEquals(2, week.strengthDays)
        assertEquals(63, week.aerobicMinutes)
        assertEquals("2/2 strength days · 63/150 min", formatWorkoutWeekLine(week))
        assertEquals("HHS adult target is 150 minutes and 2 strength days. Some is better than none.", formatWorkoutWeekCaption())
        assertEquals("Volume is weight × reps × sets. Easy junk sets inflate it.", formatVolumeTrendCaption())
    }

    @Test
    fun weeklyVolumeBucketsUseLoadTonnage() {
        val workouts = listOf(
            Workout("this", "2026-10-27", "p", "strength_training", listOf(load("e1", "p", "m", 135.0, 8))),
            Workout("prior", "2026-10-14", "p", "strength_training", listOf(load("e2", "p", "m", 100.0, 10), load("e3", "p", "m", 100.0, 10))),
        )
        val weeks = workoutSessionsByWeek(workouts, weeks = 3, today = "2026-10-27")
        assertEquals(3, weeks.size)
        assertEquals(2000.0, weeks[0].volumeLb, 0.0)
        assertEquals(0.0, weeks[1].volumeLb, 0.0)
        assertEquals(1080.0, weeks[2].volumeLb, 0.0)
        assertEquals(1, weeks[2].count)
    }

    @Test
    fun vsLastTimeMutesSameLoadAndShowsProgress() {
        val last = PreviousLoad(130.0, 8)
        val same = PreviousLoad(135.0, 8)
        assertEquals("135 × 8 · last time 130 × 8", formatActivityVsLastTime(same, last))
        assertNull(formatActivityVsLastTime(same, same))
        assertEquals("Last time: 135 lb × 8.", formatActivityVsLastTime(null, same))
        assertNull(formatActivityVsLastTime(same, null))
    }

    @Test
    fun previousCardioAndDurationSuggestions() {
        val ride = WorkoutEntry(
            "e",
            "p",
            "m",
            schema = "distance_time",
            distanceMi = 3.1,
            durationSec = 1692,
            resistance = 8.0,
            avgSpeedMph = 16.5,
        )
        val yoga = WorkoutEntry("y", "p", "mat", schema = "duration", durationSec = 1200)
        assertEquals("Last time: 3.1 mi · 28:12 · Resistance 8 · 16.5 mph.", formatPreviousActivitySuggestion(ride))
        assertEquals("Last time: 20 min.", formatPreviousActivitySuggestion(yoga))
        val plan = FitnessPlan("p", "Ride", activityTypeId = "cycling", category = "cardio", machines = listOf(Machine("m", "Indoor")))
        val previous = Workout("old", "2026-10-01", "p", "cycling", listOf(ride))
        assertEquals(3.1, findPreviousEntry(listOf(previous), listOf(plan), "m", "2026-10-12", emptyList())?.distanceMi)
        val timed = Workout("w", "2026-10-10", "p", "strength_training", listOf(load("e1", "p", "m", 135.0, 8)), durationSec = 2700)
        assertEquals("1 set · 1,080 lb · 45 min", summarizeWorkout(timed))
    }

    @Test
    fun workoutReminderCopyMatchesWeightVoice() {
        assertEquals("Off", formatWorkoutReminder(false, "07:00"))
        assertTrue(formatWorkoutReminder(true, "06:30").startsWith("Morning"))
    }

    @Test
    fun warmupSetsAreIgnoredForVolumeAndRecords() {
        val plan = FitnessPlan("p", "Upper", activityTypeId = "strength_training", category = "strength", machines = listOf(Machine("m", "Press")))
        val warmup = load("w", "p", "m", 185.0, 5).copy(setKind = "warmup")
        val working = load("a", "p", "m", 135.0, 8)
        assertEquals(1080.0, sessionVolumeLb(listOf(warmup, working)), 0.0)
        val record = activityPersonalRecord(
            listOf(Workout("s", "2026-10-10", "p", "strength_training", listOf(warmup, working))),
            listOf(plan),
            "m",
        )!!
        assertEquals(135.0, record.weightLb, 0.0)
        assertEquals(8, record.repetitions)
        assertEquals(171.0, epleyOneRepMaxLb(135.0, 8), 0.0)
        assertEquals("PR 135 × 8 · Epley 1RM 171 lb. Estimate only.", formatActivityRecord(record))
        assertEquals("185 lb · 5 reps · Warm-up", formatWorkoutEntrySummary(warmup))
        assertEquals("135 lb · 8 reps · RPE 7", formatWorkoutEntrySummary(working.copy(rpe = 7)))
    }

    @Test
    fun workoutHistoryFiltersByPlanAndFamily() {
        val lift = FitnessPlan("p-lift", "Upper", activityTypeId = "strength_training", category = "strength")
        val walk = FitnessPlan("p-walk", "Walk", activityTypeId = "walking", category = "cardio")
        val workouts = listOf(
            Workout("a", "2026-10-27", "p-lift", "strength_training", listOf(load("e1", "p-lift", "m", 135.0, 8))),
            Workout("b", "2026-10-20", "p-walk", "walking", listOf(WorkoutEntry("e2", "p-walk", "m", schema = "distance_time", distanceMi = 2.0, durationSec = 1800))),
            Workout("c", "2026-09-01", "p-lift", "strength_training", listOf(load("e3", "p-lift", "m", 100.0, 10))),
        )
        val last30 = filterWorkouts(workouts, listOf(lift, walk), WorkoutHistoryRange.Last30, today = "2026-10-27")
        assertEquals(listOf("a", "b"), last30.map { it.id })
        assertEquals(listOf("a", "c"), filterWorkouts(workouts, listOf(lift, walk), familyId = "strength_resistance").map { it.id })
        assertEquals(listOf("b"), filterWorkouts(workouts, listOf(lift, walk), planId = "p-walk").map { it.id })
    }

    @Test
    fun workoutShareCsvAndRestCopy() {
        val plan = FitnessPlan("p", "Upper", activityTypeId = "strength_training", category = "strength", machines = listOf(Machine("m", "Press")))
        val workout = Workout("w", "2026-10-10", "p", "strength_training", listOf(load("e1", "p", "m", 135.0, 8)), note = "travel gym")
        val csv = buildWorkoutCsv(listOf(workout), listOf(plan))
        assertTrue(csv.startsWith("date,plan,activity,lb,reps,miles,seconds,note"))
        assertTrue(csv.contains("2026-10-10,Upper,Press,135,8,,,travel gym"))
        assertEquals("Off", formatWorkoutRestTimer(0))
        assertEquals("90s", formatWorkoutRestTimer(90))
        assertEquals("1:30", formatRestClock(90))
        assertEquals("1 set · 1,080 lb · travel gym", summarizeWorkout(workout))
    }

    @Test
    fun weightGoalProgressNeedsAStartingReading() {
        val record = WeightRecord("w1", "2026-10-01", "08:00", 180.0)
        assertNull(weightGoalProgress(listOf(record), null, 170.0))
        assertNull(weightGoalProgress(listOf(record), "missing", 170.0))
        assertNull(weightGoalProgress(listOf(record), "w1", null))
    }

    @Test
    fun bmiUsesObesityClassesAndKeepsOverweightBoundary() {
        assertEquals(BmiCategory.Over, bmiCategory(calculateBmi(182.3, 70)))
        assertEquals(BmiCategory.Obese1, bmiCategory(30.0))
        assertEquals(BmiCategory.Obese2, bmiCategory(35.0))
        assertEquals(BmiCategory.Obese3, bmiCategory(42.0))
        assertEquals("Overweight · CDC adult screening", formatBmiCaption(calculateBmi(182.3, 70), compact = false))
        assertEquals("Class 3 · CDC adult screening", formatBmiCaption(42.0, compact = true))
    }

    @Test
    fun weightTrendIgnoresALateRawSpike() {
        val declining = (1..13).map { day ->
            WeightRecord("w$day", "2026-10-${day.toString().padStart(2, '0')}", "08:00", 185.0 - day * 0.3)
        }
        val spiked = WeightRecord("w14", "2026-10-14", "20:00", declining.last().weight + 2.0)
        val records = declining + spiked
        val priorRaw = declining.last().weight
        assertTrue(spiked.weight > priorRaw)
        val delta = weightTrendDelta(records)!!
        assertTrue(delta < 0.0)
        assertTrue(latestWeightTrend(records)!! < spiked.weight)
    }

    @Test
    fun weightChangeUnderHalfPoundIsUnchanged() {
        assertEquals(WeightChangeDirection.Unchanged, weightChangeDirection(180.2, 180.0))
        assertEquals(WeightChangeDirection.Up, weightChangeDirection(180.6, 180.0))
        assertEquals(WeightChangeDirection.Down, weightChangeDirection(179.4, 180.0))
        assertNull(weightChangeDirection(180.0, null))
    }

    @Test
    fun weightValidationRejectsEmptyAndOutOfRange() {
        assertEquals("Enter a weight value.", validateWeightLb(null))
        assertEquals("Weight must be 50–700 lb.", validateWeightLb(0.0))
        assertEquals("Weight must be 50–700 lb.", validateWeightLb(49.9))
        assertEquals("Weight must be 50–700 lb.", validateWeightLb(700.1))
        assertNull(validateWeightLb(50.0))
        assertNull(validateWeightLb(182.3))
        val tooLow = validateProfile("5", "10", "20", 10)
        assertEquals("Weight must be 50–700 lb.", tooLow.weightGoalError)
    }

    @Test
    fun weightGoalCopyFollowsLoseGainOrMaintain() {
        assertEquals(WeightGoalKind.Lose, weightGoalKind(180.0, 170.0))
        assertEquals(WeightGoalKind.Gain, weightGoalKind(140.0, 155.0))
        assertEquals(WeightGoalKind.Maintain, weightGoalKind(170.0, 170.0))
        val gain = weightGoalProgress(
            listOf(WeightRecord("start", "2026-09-01", "08:00", 140.0), WeightRecord("now", "2026-10-19", "08:00", 146.0)),
            "start",
            155.0,
        )!!
        assertEquals(WeightGoalKind.Gain, gain.kind)
        assertEquals(15.0, gain.targetChangeLb(), 0.0)
        assertEquals(6.0, gain.progressChangeLb(), 0.0)
        val hold = weightGoalProgress(
            listOf(WeightRecord("start", "2026-09-01", "08:00", 170.0), WeightRecord("now", "2026-10-19", "08:00", 171.2)),
            "start",
            170.0,
        )!!
        assertEquals(WeightGoalKind.Maintain, hold.kind)
        assertEquals(1.2, hold.progressChangeLb(), 0.0)
    }

    @Test
    fun dailyWeightKeepsTheLowestReadingOfTheDay() {
        val records = listOf(
            WeightRecord("am", "2026-10-12", "07:10", 181.4),
            WeightRecord("pm", "2026-10-12", "20:40", 184.1),
            WeightRecord("next", "2026-10-13", "07:05", 180.8),
        )
        val daily = dailyWeights(records)
        assertEquals(2, daily.size)
        assertEquals(181.4, daily[0].weight, 0.0)
        assertEquals("am", daily[0].sourceId)
        assertEquals(180.8, daily[1].weight, 0.0)
    }

    @Test
    fun weightChartUsesCalendarSpanNotReadingIndex() {
        val records = listOf(
            WeightRecord("a", "2026-09-27", "08:00", 180.0),
            WeightRecord("b", "2026-10-27", "08:00", 176.0),
        )
        val all = weightChartModel(records, WeightChartRange.All, today = "2026-10-27")!!
        assertEquals("2026-09-27", all.startDate)
        assertEquals("2026-10-27", all.endDate)
        assertEquals(2, all.points.size)
        val last30 = weightChartModel(records, WeightChartRange.Last30, today = "2026-10-27")!!
        assertEquals("2026-09-28", last30.startDate)
        assertEquals("2026-10-27", last30.endDate)
        assertEquals(listOf("b"), last30.points.map { it.sourceId })
        val home = homeWeightChartModel(records)!!
        assertEquals("2026-09-27", home.startDate)
        assertEquals("2026-10-27", home.endDate)
    }

    @Test
    fun weightWeeklyRateAndEtaFollowTheTrend() {
        val records = (0..28).map { day ->
            val date = java.time.LocalDate.parse("2026-09-29").plusDays(day.toLong()).toString()
            WeightRecord("w$day", date, "08:00", 180.0 - day * (1.0 / 7.0))
        }
        val rate = weightWeeklyRate(records, today = "2026-10-27")!!
        assertEquals(-1.0, rate.lbPerWeek, 0.15)
        assertTrue(rate.days >= 7)
        val eta = weightGoalEtaDate(records, "w0", 170.0, today = "2026-10-27")
        assertTrue(eta != null)
        val etaDate = java.time.LocalDate.parse(eta!!)
        assertTrue(!etaDate.isBefore(java.time.LocalDate.parse("2026-11-15")))
        assertTrue(etaDate.isBefore(java.time.LocalDate.parse("2027-03-01")))
        assertTrue(formatWeightGoalEta(eta).startsWith("At this rate, around "))
        val plateau = listOf(
            WeightRecord("a", "2026-10-01", "08:00", 180.0),
            WeightRecord("b", "2026-10-28", "08:00", 180.1),
        )
        assertNull(weightGoalEtaDate(plateau, "a", 170.0, today = "2026-10-28"))
    }

    @Test
    fun tenPercentGoalAndReminderCopy() {
        assertEquals(162.0, tenPercentLossTarget(180.0), 0.0)
        assertEquals("10% of start is 162.0 lb", formatTenPercentGoalHint(180.0))
        assertEquals("Off", formatWeightReminder(false, "07:00"))
        assertTrue(formatWeightReminder(true, "07:00").startsWith("Morning"))
        assertEquals("10% of start is 73.5 kg", formatTenPercentGoalHint(180.0, useKg = true))
    }

    @Test
    fun weightTagsInferMorningAndEveningFromTime() {
        assertEquals(WeightTag.Morning, inferWeightTag("08:15"))
        assertEquals(WeightTag.Evening, inferWeightTag("20:40"))
        assertNull(inferWeightTag("13:10"))
        val morning = WeightRecord("am", "2026-10-12", "07:10", 181.4)
        val tagged = WeightRecord("pm", "2026-10-12", "20:40", 184.1, tags = listOf(WeightTag.Travel.id), note = "hotel scale")
        assertEquals(listOf(WeightTag.Morning), weightRecordTags(morning))
        assertEquals(listOf(WeightTag.Travel, WeightTag.Evening), weightRecordTags(tagged))
        assertEquals("Travel, Evening", formatWeightTags(tagged))
        assertTrue(formatWeightHistoryLine(tagged).contains("hotel scale"))
    }

    @Test
    fun weightListFiltersByRangeAndInferredTag() {
        val records = listOf(
            WeightRecord("old", "2026-09-01", "07:00", 185.0),
            WeightRecord("am", "2026-10-20", "07:10", 181.4),
            WeightRecord("pm", "2026-10-20", "20:40", 184.1, tags = listOf(WeightTag.Travel.id)),
            WeightRecord("gym", "2026-10-21", "18:00", 180.8, tags = listOf(WeightTag.AfterExercise.id)),
        )
        val last30 = filterWeightRecords(records, WeightChartRange.Last30, today = "2026-10-27")
        assertEquals(listOf("am", "pm", "gym"), last30.map { it.id })
        val mornings = filterWeightRecords(
            records,
            WeightChartRange.Last30,
            tags = listOf(WeightTag.Morning.id),
            today = "2026-10-27",
        )
        assertEquals(listOf("am"), mornings.map { it.id })
        val travel = filterWeightRecords(
            records,
            WeightChartRange.All,
            tags = listOf(WeightTag.Travel.id),
            today = "2026-10-27",
        )
        assertEquals(listOf("pm"), travel.map { it.id })
    }

    @Test
    fun weightDisplayConvertsAndStepsInKilograms() {
        assertEquals(81.7, displayWeight(180.2, useKg = true), 0.05)
        assertEquals(180.8, parseWeightInput("82.0", useKg = true)!!, 0.05)
        assertEquals(180.2, parseWeightInput("180.2", useKg = false)!!, 0.05)
        assertEquals("81.7 kg", formatWeightWithUnit(180.2, useKg = true))
        val stepped = stepDisplayedWeight(180.2, 0.1, useKg = true)
        assertEquals(81.8, displayWeight(stepped, useKg = true), 0.05)
    }

    @Test
    fun waistValidationAndCaptionNameNhlbiCutoffs() {
        assertEquals(null to null, validateWaistInches(""))
        assertEquals(34.5 to null, validateWaistInches("34.5"))
        assertEquals(null to "Waist must be 15–80 in.", validateWaistInches("10"))
        assertEquals(null to "Waist must be 15–80 in.", validateWaistInches("90"))
        assertEquals(null to "Enter a waist measurement.", validateWaistInches("n/a"))
        val caption = formatWaistCaption(36.0)
        assertTrue(caption.contains("36.0 in"))
        assertTrue(caption.contains("40 in"))
        assertTrue(caption.contains("35 in"))
    }

    @Test
    fun weightShareKeepsPoundsAndIncludesNote() {
        val records = listOf(
            WeightRecord("am", "2026-10-12", "07:10", 181.4, note = "after walk"),
            WeightRecord("pm", "2026-10-12", "20:40", 184.1, tags = listOf(WeightTag.Travel.id)),
        )
        val csv = buildWeightCsv(records)
        assertTrue(csv.startsWith("date,time,weightLb,tags,note"))
        assertTrue(csv.contains("2026-10-12,07:10,181.4,,after walk"))
        assertTrue(csv.contains("2026-10-12,20:40,184.1,travel,"))
        val report = buildWeightReport(records, generatedAt = "Tue, Oct 27, 2026 7:00 PM")
        assertTrue(report.contains("Life Metric weight log"))
        assertTrue(report.contains("181.4 lb"))
        assertTrue(report.contains("after walk"))
        assertTrue(report.contains("Travel"))
    }

    private fun load(id: String, planId: String, machineId: String, weight: Double, reps: Int) =
        WorkoutEntry(id, planId, machineId, schema = "load", weight = weight, repetitions = reps)
}
