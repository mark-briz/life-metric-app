package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.EntryMetric
import com.markbriz.lifemetric.data.EntryMetricKind
import com.markbriz.lifemetric.data.EntryMetrics
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.newId
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val DEFAULT_BP_WINDOW = 10
const val MIN_BP_WINDOW = 2
const val MAX_BP_WINDOW = 60

enum class BpStatus(val label: String, val color: Long, val range: String, val description: String) {
    Normal(
        "Normal",
        0xFF059669,
        "Less than 120 and less than 80",
        "Systolic less than 120 and diastolic less than 80.",
    ),
    Elevated(
        "Elevated",
        0xFFD97706,
        "120–129 and less than 80",
        "Systolic 120 to 129 and diastolic less than 80.",
    ),
    Stage1(
        "Stage 1 Hypertension",
        0xFFEA580C,
        "130–139 or 80–89",
        "Systolic 130 to 139 or diastolic 80 to 89. Also called high blood pressure.",
    ),
    Stage2(
        "Stage 2 Hypertension",
        0xFFDC2626,
        "140 or higher or 90 or higher",
        "Systolic 140 or higher or diastolic 90 or higher. Also called high blood pressure.",
    ),
    Severe(
        "Severe Hypertension",
        0xFF991B1B,
        "Higher than 180 and/or higher than 120",
        "Systolic higher than 180 and/or diastolic higher than 120. If you do not have symptoms, wait 1 minute, take it again, and call your health care professional if it stays this high. If you have chest pain, shortness of breath, back pain, numbness, weakness, vision changes, or trouble speaking, call 911 — that is a hypertensive emergency.",
    );

    companion object {
        val explained = listOf(Normal, Elevated, Stage1, Stage2, Severe)
    }
}

enum class BmiCategory(val label: String, val range: String, val color: Long) {
    Under("Under", "0 – <18.5", 0xFF4F7F9C),
    Healthy("Healthy", "18.5 – 24.9", 0xFF1F6F5B),
    Over("Over", "25.0 – 29.9", 0xFFC4892A),
    Obese("Obese", "> 29.9", 0xFFA33B2D),
}

fun todayIso(): String = LocalDate.now().toString()

fun nowTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
}.getOrDefault(iso)

fun shortDateLabel(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}.getOrDefault(iso)

fun formatTime(time: String): String = runCatching {
    LocalTime.parse(time).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}.getOrDefault(time)

fun roundToTenth(value: Double): Double = (value * 10.0).roundToInt() / 10.0

fun formatWeight(value: Double): String = String.format(Locale.US, "%.1f", roundToTenth(value))

fun clampBpWindow(value: Int): Int = value.coerceIn(MIN_BP_WINDOW, MAX_BP_WINDOW)

fun calculateBmi(weightLb: Double, heightInches: Int): Double {
    if (heightInches <= 0 || weightLb < 0) return Double.NaN
    return (703.0 * weightLb) / (heightInches * heightInches)
}

fun formatBmi(bmi: Double): String = if (bmi.isNaN()) "—" else String.format(Locale.US, "%.1f", (bmi * 10).roundToInt() / 10.0)

fun bmiCategory(bmi: Double): BmiCategory? = when {
    bmi.isNaN() -> null
    bmi < 18.5 -> BmiCategory.Under
    bmi <= 24.9 -> BmiCategory.Healthy
    bmi <= 29.9 -> BmiCategory.Over
    else -> BmiCategory.Obese
}

fun bloodPressureStatus(systolic: Int, diastolic: Int): BpStatus = when {
    systolic > 180 || diastolic > 120 -> BpStatus.Severe
    systolic >= 140 || diastolic >= 90 -> BpStatus.Stage2
    (systolic in 130..139) || (diastolic in 80..89) -> BpStatus.Stage1
    systolic in 120..129 && diastolic < 80 -> BpStatus.Elevated
    else -> BpStatus.Normal
}

fun trailingMovingAverage(values: List<Double>, window: Int): List<Double> {
    val size = max(1, window)
    val result = ArrayList<Double>(values.size)
    var sum = 0.0
    values.forEachIndexed { index, value ->
        sum += value
        if (index >= size) sum -= values[index - size]
        result += sum / min(index + 1, size)
    }
    return result
}

private fun dateTimeKey(date: String, time: String) = "${date}T$time"

fun latestWeight(records: List<WeightRecord>): WeightRecord? =
    records.maxWithOrNull(compareBy<WeightRecord> { dateTimeKey(it.date, it.time) }.thenBy { it.id })

fun latestBloodPressure(readings: List<BloodPressureReading>): BloodPressureReading? =
    readings.maxWithOrNull(compareBy<BloodPressureReading> { dateTimeKey(it.date, it.time) }.thenBy { it.id })

fun weightDelta(records: List<WeightRecord>): Double? {
    val sorted = records.sortedWith(compareByDescending<WeightRecord> { dateTimeKey(it.date, it.time) }.thenByDescending { it.id })
    if (sorted.size < 2) return null
    return sorted[0].weight - sorted[1].weight
}

fun formatDelta(value: Double, unit: String): String {
    val sign = if (value > 0) "+" else ""
    return "$sign${String.format(Locale.US, "%.1f", value)} $unit"
}

fun chronologicalBp(readings: List<BloodPressureReading>): List<BloodPressureReading> =
    readings.sortedWith(compareBy<BloodPressureReading> { dateTimeKey(it.date, it.time) }.thenBy { it.id })

fun recentBloodPressure(readings: List<BloodPressureReading>, limit: Int): List<BloodPressureReading> =
    chronologicalBp(readings).takeLast(limit)

fun recentWeights(records: List<WeightRecord>, limit: Int): List<WeightRecord> =
    records.sortedWith(compareByDescending<WeightRecord> { dateTimeKey(it.date, it.time) }.thenByDescending { it.id })
        .take(limit)

fun chronologicalWeights(records: List<WeightRecord>): List<WeightRecord> =
    records.sortedWith(compareBy<WeightRecord> { dateTimeKey(it.date, it.time) }.thenBy { it.id })

data class WeightGoalProgress(
    val startLb: Double,
    val targetLb: Double,
    val currentLb: Double,
    val targetToLoseLb: Double,
    val weightLostLb: Double,
    val percentComplete: Double,
)

/**
 * Progress from a chosen starting reading toward the profile target, using the latest
 * recorded weight as the current value. Returns null until both a start reading and a
 * target are set.
 */
fun weightGoalProgress(
    records: List<WeightRecord>,
    startId: String?,
    targetLb: Double?,
): WeightGoalProgress? {
    if (startId.isNullOrBlank() || targetLb == null) return null
    val start = records.find { it.id == startId } ?: return null
    val current = latestWeight(records)?.weight ?: start.weight
    val span = start.weight - targetLb
    val percent = when {
        kotlin.math.abs(span) < 0.05 -> if (kotlin.math.abs(current - targetLb) < 0.05) 100.0 else 0.0
        span > 0 -> (start.weight - current) / span * 100.0
        else -> (current - start.weight) / (targetLb - start.weight) * 100.0
    }.coerceIn(0.0, 100.0)
    return WeightGoalProgress(
        startLb = start.weight,
        targetLb = targetLb,
        currentLb = current,
        targetToLoseLb = roundToTenth(start.weight - targetLb),
        weightLostLb = roundToTenth(start.weight - current),
        percentComplete = percent,
    )
}

private fun weekStart(iso: String): LocalDate {
    val date = LocalDate.parse(iso)
    val diff = if (date.dayOfWeek.value == 7) -6 else 1 - date.dayOfWeek.value
    return date.plusDays(diff.toLong())
}

data class WeekBucket(val label: String, val count: Int)

fun workoutSessionsByWeek(workouts: List<Workout>, weeks: Int = 8): List<WeekBucket> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val buckets = (weeks - 1 downTo 0).map { offset ->
        val start = today.minusDays(offset.toLong() * 7)
        val key = weekStart(start.toString())
        key to WeekBucket(start.format(formatter), 0)
    }.toMutableList()
    val index = buckets.mapIndexed { i, pair -> pair.first to i }.toMap()
    workouts.forEach { workout ->
        runCatching { weekStart(workout.date) }.getOrNull()?.let { key ->
            val i = index[key] ?: return@forEach
            val current = buckets[i]
            buckets[i] = key to current.second.copy(count = current.second.count + 1)
        }
    }
    return buckets.map { it.second }
}

fun workoutsInLastDays(workouts: List<Workout>, days: Int): List<Workout> {
    val cutoff = LocalDate.now().minusDays((days - 1).toLong()).toString()
    val today = todayIso()
    return workouts.filter { it.date >= cutoff && it.date <= today }
}

fun daysSince(iso: String?): Int? {
    if (iso.isNullOrBlank()) return null
    val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return null
    return max(0, (LocalDate.now().toEpochDay() - date.toEpochDay()).toInt())
}

fun formatRelativeDays(iso: String?): String {
    val days = daysSince(iso) ?: return "No data yet"
    return when (days) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> "$days days ago"
    }
}

fun parseDurationInput(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val hms = Regex("""^(\d+):(\d{1,2}):(\d{2})$""").matchEntire(trimmed)
    if (hms != null) {
        val hours = hms.groupValues[1].toInt()
        val minutes = hms.groupValues[2].toInt()
        val seconds = hms.groupValues[3].toInt()
        if (minutes >= 60 || seconds >= 60) return null
        val total = hours * 3600 + minutes * 60 + seconds
        return if (total > 0) total else null
    }
    val ms = Regex("""^(\d+):(\d{1,2})$""").matchEntire(trimmed)
    if (ms != null) {
        val minutes = ms.groupValues[1].toInt()
        val seconds = ms.groupValues[2].toInt()
        if (seconds >= 60) return null
        val total = minutes * 60 + seconds
        return if (total > 0) total else null
    }
    val minutes = trimmed.toDoubleOrNull() ?: return null
    if (minutes <= 0) return null
    return (minutes * 60).roundToInt()
}

fun formatDurationClock(sec: Int): String {
    if (sec <= 0) return ""
    val hours = sec / 3600
    val minutes = (sec % 3600) / 60
    val seconds = sec % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatDurationLabel(sec: Int): String {
    if (sec <= 0) return ""
    if (sec % 60 == 0) return "${sec / 60} min"
    return formatDurationClock(sec)
}

fun formatPace(distanceMi: Double, durationSec: Int): String {
    if (distanceMi <= 0 || durationSec <= 0) return ""
    val secPerMile = (durationSec / distanceMi).roundToInt()
    val minutes = secPerMile / 60
    val seconds = secPerMile % 60
    return "%d:%02d / mi".format(minutes, seconds)
}

private fun milesLabel(miles: Double): String {
    if (miles == miles.toInt().toDouble()) return miles.toInt().toString()
    return String.format(Locale.US, "%.2f", miles).trimEnd('0').trimEnd('.')
}

data class PreviousLoad(
    val weight: Double,
    val repetitions: Int,
)

fun lastUsedPlanId(workouts: List<Workout>, plans: List<FitnessPlan>): String? {
    if (plans.isEmpty()) return null
    val last = workouts.maxWithOrNull(compareBy<Workout> { it.date }.thenBy { it.id })
    return last?.planId?.takeIf { id -> plans.any { it.id == id } } ?: plans.first().id
}

fun lastUsedMachineId(workouts: List<Workout>, planId: String, machines: List<Machine>): String? {
    if (machines.isEmpty()) return null
    val last = workouts
        .filter { it.planId == planId && it.entries.isNotEmpty() }
        .maxWithOrNull(compareBy<Workout> { it.date }.thenBy { it.id })
    val machineId = last?.entries?.lastOrNull()?.machineId
    return machineId?.takeIf { id -> machines.any { it.id == id } } ?: machines.first().id
}

fun formatPreviousLoadSuggestion(load: PreviousLoad): String =
    "Last time: ${formatLoadWeight(load.weight)} lb × ${load.repetitions}."

fun emptyWorkoutEntry(plan: FitnessPlan): WorkoutEntry {
    val schema = ActivityCatalog.schema(plan.activityTypeId)
    val defaults = ActivityCatalog.intervalDefaults(plan.activityTypeId)
    return WorkoutEntry(
        id = newId("entry"),
        planId = plan.id,
        machineId = plan.machines.firstOrNull()?.id.orEmpty(),
        schema = schema,
        weight = if (schema == "load") 0.0 else null,
        repetitions = if (schema == "load") 0 else null,
        distanceMi = if (schema == "distance_time") 0.0 else null,
        durationSec = if (schema == "distance_time" || schema == "duration" || schema == "sport") 0 else null,
        rounds = if (schema == "interval") defaults.first else null,
        workSec = if (schema == "interval") defaults.second else null,
        restSec = if (schema == "interval") defaults.third else null,
        note = if (schema == "note") "" else null,
    )
}

const val REMOVED_ACTIVITY_NAME = "Removed activity"

/**
 * Logged sets keep their activity id after that activity is removed from its plan.
 * Put a placeholder activity back so an export still satisfies the file's
 * "every set points at an activity on its plan" rule and can be imported.
 */
fun plansWithLoggedActivities(plans: List<FitnessPlan>, workouts: List<Workout>): List<FitnessPlan> {
    val entriesByPlan = workouts.groupBy { it.planId }
    val usedIds = plans.flatMap { it.machines }.map { it.id }.toMutableSet()
    return plans.map { plan ->
        val missing = entriesByPlan[plan.id].orEmpty()
            .flatMap { it.entries }
            .map { it.machineId }
            .filter { it.isNotBlank() && it !in usedIds }
            .distinct()
        missing.forEach { usedIds.add(it) }
        if (missing.isEmpty()) {
            plan
        } else {
            plan.copy(machines = plan.machines + missing.map { id -> Machine(id, REMOVED_ACTIVITY_NAME) })
        }
    }
}

fun activityIdsWithSameName(plans: List<FitnessPlan>, activityId: String): Set<String> {
    if (activityId.isBlank()) return emptySet()
    val activities = plans.flatMap { it.machines }
    val name = activities.find { it.id == activityId }?.name?.trim().orEmpty()
    if (name.isEmpty()) return setOf(activityId)
    return activities.filter { it.name.trim().equals(name, ignoreCase = true) }.map { it.id }.toSet()
}

/**
 * Latest load (weight/reps) for the most recent set whose activity has the same name.
 * Prefers a later date, then a later set in the same workout.
 */
fun findPreviousLoad(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    activityId: String,
    currentWorkoutDate: String,
    currentEntries: List<WorkoutEntry>,
    currentWorkoutId: String? = null,
    excludeEntryId: String? = null,
): PreviousLoad? {
    val matchingIds = activityIdsWithSameName(plans, activityId)
    if (matchingIds.isEmpty()) return null
    data class Candidate(val date: String, val sequence: Int, val weight: Double, val repetitions: Int)
    val candidates = mutableListOf<Candidate>()
    fun consider(entry: WorkoutEntry, date: String, sequence: Int) {
        if (entry.machineId !in matchingIds || entry.schema != "load") return
        if (excludeEntryId != null && entry.id == excludeEntryId) return
        val weight = entry.weight ?: return
        val repetitions = entry.repetitions ?: return
        candidates += Candidate(date, sequence, weight, repetitions)
    }
    workouts.forEach { workout ->
        if (currentWorkoutId != null && workout.id == currentWorkoutId) return@forEach
        workout.entries.forEachIndexed { index, entry -> consider(entry, workout.date, index) }
    }
    currentEntries.forEachIndexed { index, entry -> consider(entry, currentWorkoutDate, index) }
    val best = candidates.maxWithOrNull(compareBy<Candidate> { it.date }.thenBy { it.sequence }) ?: return null
    return PreviousLoad(best.weight, best.repetitions)
}

fun formatLoadWeight(weight: Double): String =
    if (weight == weight.toInt().toDouble()) weight.toInt().toString() else weight.toString()

fun WorkoutEntry.metricDisplay(metric: EntryMetric): String = when (metric.id) {
    EntryMetrics.RESISTANCE -> formatLoadWeight(resistance)
    EntryMetrics.AVG_SPEED -> formatLoadWeight(avgSpeedMph)
    EntryMetrics.AVG_RPM -> avgRpm.toString()
    EntryMetrics.AVG_HEART_RATE -> avgHeartRateBpm.toString()
    EntryMetrics.MAX_HEART_RATE -> maxHeartRateBpm.toString()
    else -> ""
}

fun WorkoutEntry.withMetric(id: String, value: Double): WorkoutEntry = when (id) {
    EntryMetrics.RESISTANCE -> copy(resistance = roundToTenth(value))
    EntryMetrics.AVG_SPEED -> copy(avgSpeedMph = roundToTenth(value))
    EntryMetrics.AVG_RPM -> copy(avgRpm = value.toInt())
    EntryMetrics.AVG_HEART_RATE -> copy(avgHeartRateBpm = value.toInt())
    EntryMetrics.MAX_HEART_RATE -> copy(maxHeartRateBpm = value.toInt())
    else -> this
}

fun parseEntryMetric(metric: EntryMetric, raw: String): Pair<Double?, String?> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return 0.0 to null
    return when (metric.kind) {
        EntryMetricKind.Decimal -> {
            val value = trimmed.toDoubleOrNull()
            when {
                value == null -> null to "Enter a number."
                value < 0 -> null to "Must be 0 or greater."
                else -> roundToTenth(value) to null
            }
        }
        EntryMetricKind.Whole -> {
            val value = trimmed.toIntOrNull()
            when {
                value == null -> null to "Enter a whole number."
                value < 0 -> null to "Must be 0 or greater."
                else -> value.toDouble() to null
            }
        }
    }
}

fun WorkoutEntry.withParsedMetrics(
    metrics: List<EntryMetric>,
    values: Map<String, String>,
): Pair<WorkoutEntry, Map<String, String>> {
    var next = this
    val errors = mutableMapOf<String, String>()
    metrics.forEach { metric ->
        val (value, error) = parseEntryMetric(metric, values[metric.id].orEmpty())
        if (error != null) errors[metric.id] = error
        else if (value != null) next = next.withMetric(metric.id, value)
    }
    return next to errors
}

fun formatOptionalMetrics(entry: WorkoutEntry): String = listOfNotNull(
    entry.resistance.takeIf { it > 0 }?.let { "Resistance ${formatLoadWeight(it)}" },
    entry.avgSpeedMph.takeIf { it > 0 }?.let { "${formatLoadWeight(it)} mph" },
    entry.avgRpm.takeIf { it > 0 }?.let { "${it} rpm" },
    entry.avgHeartRateBpm.takeIf { it > 0 }?.let { "avg ${it} bpm" },
    entry.maxHeartRateBpm.takeIf { it > 0 }?.let { "max ${it} bpm" },
).joinToString(" · ")

fun formatResistance(value: Double): String? =
    value.takeIf { it > 0 }?.let { "Resistance ${formatLoadWeight(it)}" }

fun formatResistanceSummary(entries: List<WorkoutEntry>): String? {
    val values = entries.map { it.resistance }.filter { it > 0 }.distinct().sorted()
    if (values.isEmpty()) return null
    if (values.size == 1) return formatResistance(values.first())
    return "Resistance ${formatLoadWeight(values.first())}–${formatLoadWeight(values.last())}"
}

fun formatWorkoutEntrySummary(entry: WorkoutEntry): String {
    val base = when (entry.schema) {
        "load" -> {
            val reps = entry.repetitions ?: 0
            val weight = entry.weight ?: 0.0
            val weightText = if (weight == weight.toInt().toDouble()) weight.toInt().toString() else formatWeight(weight)
            "$weightText lb · $reps ${if (reps == 1) "rep" else "reps"}"
        }
        "distance_time" -> {
            val miles = milesLabel(entry.distanceMi ?: 0.0)
            val clock = formatDurationClock(entry.durationSec ?: 0)
            val distanceTime = if (clock.isEmpty()) "$miles mi" else "$miles mi · $clock"
            val resistance = formatResistance(entry.resistance)
            if (resistance == null) distanceTime else "$resistance · $distanceTime"
        }
        "duration" -> formatDurationLabel(entry.durationSec ?: 0)
        "sport" -> {
            val label = formatDurationLabel(entry.durationSec ?: 0)
            if (entry.intensity.isNullOrBlank()) label else "$label · ${entry.intensity}"
        }
        "interval" -> {
            val rounds = entry.rounds ?: 0
            val parts = listOfNotNull(
                entry.workSec?.let { "${it}s work" },
                entry.restSec?.let { "${it}s rest" },
            )
            val roundsLabel = "$rounds ${if (rounds == 1) "round" else "rounds"}"
            if (parts.isEmpty()) roundsLabel else "$roundsLabel · ${parts.joinToString(" · ")}"
        }
        "note" -> entry.note?.trim().takeUnless { it.isNullOrEmpty() } ?: "Logged"
        else -> "Logged"
    }
    return base
}

fun summarizeWorkoutEntries(entries: List<WorkoutEntry>): String {
    if (entries.isEmpty()) return "Nothing logged"
    val schemas = entries.map { it.schema }.toSet()
    if (schemas.size == 1) {
        when (entries.first().schema) {
            "distance_time" -> {
                val miles = entries.sumOf { it.distanceMi ?: 0.0 }
                val clock = formatDurationClock(entries.sumOf { it.durationSec ?: 0 })
                val distanceTime = if (clock.isEmpty()) "${milesLabel(miles)} mi" else "${milesLabel(miles)} mi · $clock"
                val resistance = formatResistanceSummary(entries)
                return if (resistance == null) distanceTime else "$resistance · $distanceTime"
            }
            "duration", "sport" -> {
                val sec = entries.sumOf { it.durationSec ?: 0 }
                return formatDurationLabel(sec).ifEmpty {
                    "${entries.size} ${ActivityCatalog.entryNoun(entries.first().schema, entries.size)}"
                }
            }
            "interval" -> {
                val rounds = entries.sumOf { it.rounds ?: 0 }
                return "$rounds ${if (rounds == 1) "round" else "rounds"}"
            }
            "note" -> {
                return if (entries.size == 1) formatWorkoutEntrySummary(entries.first()) else "${entries.size} notes"
            }
        }
    }
    val schema = entries.first().schema
    return "${entries.size} ${ActivityCatalog.entryNoun(schema, entries.size)}"
}

data class ProfileFormResult(
    val heightInches: Int?,
    val weightGoalLb: Double?,
    val bpMovingAverageReadings: Int,
    val heightFeetError: String? = null,
    val heightInchesError: String? = null,
    val weightGoalError: String? = null,
    val bpError: String? = null,
) {
    val ok: Boolean
        get() = heightFeetError == null && heightInchesError == null && weightGoalError == null && bpError == null
}

fun heightParts(total: Int?): Pair<String, String> {
    if (total == null || total <= 0) return "" to ""
    return (total / 12).toString() to (total % 12).toString()
}

fun formatHeight(heightInches: Int?): String {
    if (heightInches == null || heightInches <= 0) return "Not set"
    val (feet, inches) = heightParts(heightInches)
    return "$feet' $inches\""
}

fun validateProfile(
    heightFeet: String,
    heightInches: String,
    weightGoalText: String,
    bpWindow: Int,
): ProfileFormResult {
    var feetError: String? = null
    var inchesError: String? = null
    var goalError: String? = null
    var bpError: String? = null
    var height: Int? = null
    val feetTrim = heightFeet.trim()
    val inchesTrim = heightInches.trim()
    if (feetTrim.isNotEmpty() || inchesTrim.isNotEmpty()) {
        if (feetTrim.isNotEmpty() && !feetTrim.all { it.isDigit() }) feetError = "Enter whole feet."
        if (inchesTrim.isNotEmpty() && !inchesTrim.all { it.isDigit() }) inchesError = "Enter whole inches (0–11)."
        if (feetError == null && inchesError == null) {
            val feet = feetTrim.toIntOrNull() ?: 0
            val inches = inchesTrim.toIntOrNull() ?: 0
            if (inches > 11) {
                inchesError = "Inches must be 0–11."
            } else {
                val total = feet * 12 + inches
                when {
                    total <= 0 -> feetError = "Enter a valid height."
                    total < 36 || total > 96 -> feetError = "Height must be between 3'0\" and 8'0\"."
                    else -> height = total
                }
            }
        }
    }
    val goal = if (weightGoalText.isBlank()) {
        null
    } else {
        val parsed = weightGoalText.toDoubleOrNull()
        when {
            parsed == null -> {
                goalError = "Enter a weight value."
                null
            }
            parsed < 0 -> {
                goalError = "Weight must be 0 or greater."
                null
            }
            else -> roundToTenth(parsed)
        }
    }
    val window = if (bpWindow < MIN_BP_WINDOW || bpWindow > MAX_BP_WINDOW) {
        bpError = "Use $MIN_BP_WINDOW–$MAX_BP_WINDOW readings."
        DEFAULT_BP_WINDOW
    } else {
        bpWindow
    }
    return ProfileFormResult(height, goal, window, feetError, inchesError, goalError, bpError)
}
