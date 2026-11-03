package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.UserProfile
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout

data class BloodPressurePrintRow(
    val date: String,
    val amPressure: String,
    val amHeartRate: String,
    val pmPressure: String,
    val pmHeartRate: String,
)

data class BloodPressurePrintTable(
    val title: String,
    val lines: List<String>,
    val rows: List<BloodPressurePrintRow>,
)

fun formatPrintPressure(systolic: Int, diastolic: Int): String = "$systolic/$diastolic"

fun buildBloodPressurePrintTable(
    readings: List<BloodPressureReading>,
    generatedAt: String,
    deviceName: String? = null,
): BloodPressurePrintTable {
    val chronological = chronologicalBp(readings)
    val lines = buildList {
        normalizeReportDeviceName(deviceName)?.let(::add)
        add("Generated $generatedAt")
    }
    val rows = chronological
        .groupBy { it.date }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayReadings) ->
            val morning = lowestPrintReading(dayReadings.filter(::isMorningPrintReading))
            val evening = lowestPrintReading(dayReadings.filterNot(::isMorningPrintReading))
            BloodPressurePrintRow(
                date = formatPrintDate(date),
                amPressure = morning?.let { formatPrintPressure(it.systolic, it.diastolic) }.orEmpty(),
                amHeartRate = morning?.heartRate?.toString().orEmpty(),
                pmPressure = evening?.let { formatPrintPressure(it.systolic, it.diastolic) }.orEmpty(),
                pmHeartRate = evening?.heartRate?.toString().orEmpty(),
            )
        }
    return BloodPressurePrintTable("Life Metric - Blood pressure personal log", lines, rows)
}

private fun isMorningPrintReading(reading: BloodPressureReading): Boolean =
    when (bloodPressurePeriod(reading)) {
        BloodPressurePeriod.Morning -> true
        BloodPressurePeriod.Evening -> false
        BloodPressurePeriod.Other -> inferBloodPressurePeriod(reading.time) == BloodPressurePeriod.Morning
    }

private fun lowestPrintReading(readings: List<BloodPressureReading>): BloodPressureReading? =
    readings.minWithOrNull(
        compareBy<BloodPressureReading> { it.systolic }
            .thenBy { it.diastolic }
            .thenBy { it.time }
            .thenBy { it.id },
    )

data class WeightPrintRow(
    val date: String,
    val time: String,
    val weight: String,
    val direction: WeightChangeDirection?,
)

data class WeightPrintTable(
    val title: String,
    val lines: List<String>,
    val rows: List<WeightPrintRow>,
)

fun formatPrintDate(iso: String): String = runCatching {
    java.time.LocalDate.parse(iso).format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy"))
}.getOrDefault(iso)

fun formatPrintTime(time: String): String = runCatching {
    java.time.LocalTime.parse(time).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(time)

fun buildWeightPrintTable(
    records: List<WeightRecord>,
    profile: UserProfile,
    generatedAt: String,
    deviceName: String? = null,
): WeightPrintTable {
    val useKg = profile.weightDisplayKg
    val chronological = chronologicalWeights(records)
    val lines = buildList {
        normalizeReportDeviceName(deviceName)?.let(::add)
        add("Generated $generatedAt")
        add("${chronological.size} reading${if (chronological.size == 1) "" else "s"} · ${weightUnit(useKg)}")
    }
    val rows = chronological.mapIndexed { index, record ->
        val previous = chronological.getOrNull(index - 1)?.weight
        WeightPrintRow(
            date = formatPrintDate(record.date),
            time = formatPrintTime(record.time),
            weight = formatWeightWithUnit(record.weight, useKg),
            direction = weightChangeDirection(record.weight, previous),
        )
    }.asReversed()
    return WeightPrintTable("Life Metric — Weight history", lines, rows)
}

data class WorkoutPrintRow(
    val date: String,
    /** One cell per [WorkoutPrintTable.columns], empty when that category had no work that day. */
    val cells: List<String>,
)

data class WorkoutPrintTable(
    val title: String,
    val lines: List<String>,
    val columns: List<String>,
    val rows: List<WorkoutPrintRow>,
)

private val workoutPrintFamilyIds = setOf("strength_resistance", "cardio_endurance")

private val workoutPrintFamilies = ActivityCatalog.families.filter { it.id in workoutPrintFamilyIds }

fun buildWorkoutPrintTable(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    profile: UserProfile,
    generatedAt: String,
    deviceName: String? = null,
): WorkoutPrintTable {
    val useKg = profile.weightDisplayKg
    val unit = weightUnit(useKg)
    val columns = workoutPrintFamilies.map { family ->
        if (family.id == "strength_resistance") "${family.chip} ($unit)" else "${family.chip} (min)"
    }
    val planById = plans.associateBy { it.id }
    val printed = workouts.filter { workout ->
        workoutPrintFamilyId(workout, planById[workout.planId]) in workoutPrintFamilyIds
    }
    val rows = printed
        .groupBy { it.date }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayWorkouts) ->
            val totals = LinkedHashMap<String, Double>().apply {
                workoutPrintFamilies.forEach { put(it.id, 0.0) }
            }
            dayWorkouts.forEach { workout ->
                val familyId = workoutPrintFamilyId(workout, planById[workout.planId])
                totals[familyId] = (totals[familyId] ?: 0.0) + workoutPrintFamilyAmount(workout, familyId)
            }
            WorkoutPrintRow(
                date = formatPrintDate(date),
                cells = workoutPrintFamilies.map { family ->
                    formatWorkoutPrintCell(family.id, totals[family.id] ?: 0.0, useKg)
                },
            )
        }
    val lines = buildList {
        normalizeReportDeviceName(deviceName)?.let(::add)
        add("Generated $generatedAt")
        add("${printed.size} session${if (printed.size == 1) "" else "s"} · ${rows.size} day${if (rows.size == 1) "" else "s"}")
        add("Strength is total weight lifted. Cardio is total minutes.")
    }
    return WorkoutPrintTable("Life Metric — Workout history", lines, columns, rows)
}

/** Non-blank device name for report headers; null or blank is omitted. */
fun normalizeReportDeviceName(deviceName: String?): String? =
    deviceName?.trim()?.takeIf { it.isNotEmpty() }

internal fun workoutPrintFamilyId(workout: Workout, plan: FitnessPlan?): String {
    val typeId = workout.activityTypeId.ifBlank { plan?.activityTypeId.orEmpty() }
        .ifBlank { ActivityCatalog.DEFAULT_TYPE }
    return ActivityCatalog.familyOf(typeId)
}

/** Strength totals are pounds; other categories are whole minutes. */
internal fun workoutPrintFamilyAmount(workout: Workout, familyId: String): Double =
    if (familyId == "strength_resistance") {
        sessionVolumeLb(workout.entries)
    } else {
        val entrySec = workout.entries.sumOf(::aerobicSeconds)
        val sec = if (entrySec > 0) entrySec else (workout.durationSec ?: 0).coerceAtLeast(0)
        ((sec + 30) / 60).toDouble()
    }

private fun formatWorkoutPrintCell(familyId: String, amount: Double, useKg: Boolean): String {
    if (amount <= 0.0) return ""
    return if (familyId == "strength_resistance") {
        formatVolume(amount, useKg)
    } else {
        amount.toInt().toString()
    }
}
