package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
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

enum class BpStatus(val label: String, val color: Long) {
    Normal("Normal", 0xFF059669),
    Elevated("Elevated", 0xFFD97706),
    Stage1("Stage 1 Hypertension", 0xFFEA580C),
    Stage2("Stage 2 Hypertension", 0xFFDC2626),
    Crisis("Hypertensive Crisis", 0xFF991B1B),
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
    systolic > 180 || diastolic > 120 -> BpStatus.Crisis
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

/**
 * Latest load (weight/reps) for the same plan and activity, matching the Expo app's
 * `findPreviousLoad`. Prefers a later date, then a later set in the same workout.
 */
fun findPreviousLoad(
    workouts: List<Workout>,
    planId: String,
    machineId: String,
    currentWorkoutDate: String,
    currentEntries: List<WorkoutEntry>,
    currentWorkoutId: String? = null,
    excludeEntryId: String? = null,
): PreviousLoad? {
    if (planId.isBlank() || machineId.isBlank()) return null
    data class Candidate(val date: String, val sequence: Int, val weight: Double, val repetitions: Int)
    val candidates = mutableListOf<Candidate>()
    fun consider(entry: WorkoutEntry, date: String, sequence: Int) {
        if (entry.machineId != machineId || entry.schema != "load") return
        if (excludeEntryId != null && entry.id == excludeEntryId) return
        val weight = entry.weight ?: return
        val repetitions = entry.repetitions ?: return
        candidates += Candidate(date, sequence, weight, repetitions)
    }
    workouts.forEach { workout ->
        if (workout.planId != planId) return@forEach
        if (currentWorkoutId != null && workout.id == currentWorkoutId) return@forEach
        workout.entries.forEachIndexed { index, entry -> consider(entry, workout.date, index) }
    }
    currentEntries.forEachIndexed { index, entry -> consider(entry, currentWorkoutDate, index) }
    val best = candidates.maxWithOrNull(compareBy<Candidate> { it.date }.thenBy { it.sequence }) ?: return null
    return PreviousLoad(best.weight, best.repetitions)
}

fun formatLoadWeight(weight: Double): String =
    if (weight == weight.toInt().toDouble()) weight.toInt().toString() else weight.toString()

fun formatWorkoutEntrySummary(entry: WorkoutEntry): String = when (entry.schema) {
    "load" -> {
        val reps = entry.repetitions ?: 0
        val weight = entry.weight ?: 0.0
        val weightText = if (weight == weight.toInt().toDouble()) weight.toInt().toString() else formatWeight(weight)
        "$weightText lb · $reps ${if (reps == 1) "rep" else "reps"}"
    }
    "distance_time" -> {
        val miles = milesLabel(entry.distanceMi ?: 0.0)
        val clock = formatDurationClock(entry.durationSec ?: 0)
        val pace = formatPace(entry.distanceMi ?: 0.0, entry.durationSec ?: 0)
        if (pace.isNotEmpty()) "$miles mi · $clock · $pace" else "$miles mi · $clock"
    }
    "duration" -> formatDurationLabel(entry.durationSec ?: 0)
    "sport" -> {
        val base = formatDurationLabel(entry.durationSec ?: 0)
        if (entry.intensity.isNullOrBlank()) base else "$base · ${entry.intensity}"
    }
    "interval" -> {
        val rounds = entry.rounds ?: 0
        val extras = listOfNotNull(
            entry.workSec?.let { "${it}s work" },
            entry.restSec?.let { "${it}s rest" },
        )
        val roundsLabel = "$rounds ${if (rounds == 1) "round" else "rounds"}"
        if (extras.isEmpty()) roundsLabel else "$roundsLabel · ${extras.joinToString(" · ")}"
    }
    "note" -> entry.note?.trim().takeUnless { it.isNullOrEmpty() } ?: "Logged"
    else -> "Logged"
}

fun summarizeWorkoutEntries(entries: List<WorkoutEntry>): String {
    if (entries.isEmpty()) return "0 entries"
    val schemas = entries.map { it.schema }.toSet()
    if (schemas.size == 1) {
        when (entries.first().schema) {
            "distance_time" -> {
                val miles = entries.sumOf { it.distanceMi ?: 0.0 }
                return "${milesLabel(miles)} mi"
            }
            "duration", "sport" -> {
                val sec = entries.sumOf { it.durationSec ?: 0 }
                return formatDurationLabel(sec).ifEmpty { "${entries.size} entries" }
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
    return "${entries.size} ${if (entries.size == 1) "entry" else "entries"}"
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
