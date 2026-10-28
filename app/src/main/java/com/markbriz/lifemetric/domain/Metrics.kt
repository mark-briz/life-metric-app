package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.ActivityFamily
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.BloodPressureSample
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
const val MIN_SYSTOLIC = 50
const val MAX_SYSTOLIC = 250
const val MIN_DIASTOLIC = 30
const val MAX_DIASTOLIC = 150
const val MIN_HEART_RATE = 30
const val MAX_HEART_RATE = 220
const val WIDE_PULSE_PRESSURE = 80
const val LOW_SYSTOLIC = 90
const val LOW_DIASTOLIC = 60
const val DEFAULT_BP_MORNING_REMINDER = "07:00"
const val DEFAULT_BP_EVENING_REMINDER = "19:00"
const val DEFAULT_WEIGHT_TREND_WINDOW = 7
const val MIN_WEIGHT_LB = 50.0
const val MAX_WEIGHT_LB = 700.0
const val WEIGHT_STABLE_LB = 0.5
const val WEIGHT_RATE_LOOKBACK_DAYS = 28
const val WEIGHT_RATE_MIN_DAYS = 7
const val WEIGHT_ETA_MIN_LB_PER_WEEK = 0.2
const val DEFAULT_WEIGHT_MORNING_REMINDER = "07:00"
const val DEFAULT_WORKOUT_REMINDER = "07:00"
const val DEFAULT_WORKOUT_REST_SEC = 90
const val HHS_STRENGTH_DAYS = 2
const val HHS_AEROBIC_MINUTES = 150
const val LB_PER_KG = 2.2046226218
const val MIN_WAIST_IN = 15.0
const val MAX_WAIST_IN = 80.0
const val WAIST_HIGH_MEN_IN = 40.0
const val WAIST_HIGH_WOMEN_IN = 35.0

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

enum class BmiCategory(
    val label: String,
    val shortLabel: String,
    val range: String,
    val color: Long,
    val gaugeStart: Float,
    val gaugeEnd: Float,
) {
    Under("Underweight", "Under", "Less than 18.5", 0xFF4F7F9C, 12f, 18.5f),
    Healthy("Healthy weight", "Healthy", "18.5 – 24.9", 0xFF1F6F5B, 18.5f, 25f),
    Over("Overweight", "Over", "25.0 – 29.9", 0xFFC4892A, 25f, 30f),
    Obese1("Obesity class 1", "Class 1", "30.0 – 34.9", 0xFFA33B2D, 30f, 35f),
    Obese2("Obesity class 2", "Class 2", "35.0 – 39.9", 0xFF8B1E1E, 35f, 40f),
    Obese3("Obesity class 3", "Class 3", "40.0 or greater", 0xFF6B1010, 40f, 45f),
    ;

    companion object {
        const val GAUGE_MIN = 12f
        const val GAUGE_MAX = 45f
        val gaugeSegments = entries.toList()
    }
}

enum class WeightGoalKind { Lose, Gain, Maintain }

enum class WeightChangeDirection { Up, Down, Unchanged }

enum class WorkoutSetKind(val id: String, val label: String) {
    Warmup("warmup", "Warm-up"),
    Working("working", "Working"),
    Drop("drop", "Drop"),
    Failure("failure", "Failure"),
    ;

    companion object {
        fun fromId(id: String?): WorkoutSetKind =
            entries.find { it.id.equals(id?.trim().orEmpty(), ignoreCase = true) } ?: Working
    }
}

enum class WorkoutHistoryRange(val label: String, val days: Int?) {
    Last30("30d", 30),
    All("All", null),
}

data class ActivityRecord(
    val weightLb: Double,
    val repetitions: Int,
    val estimated1RmLb: Double,
)

enum class WeightTag(val id: String, val label: String) {
    Morning("morning", "Morning"),
    Evening("evening", "Evening"),
    AfterExercise("after_exercise", "After exercise"),
    Travel("travel", "Travel"),
    ;

    companion object {
        fun fromId(id: String): WeightTag? = entries.find { it.id == id }

        fun parse(ids: List<String>): List<WeightTag> =
            ids.mapNotNull(::fromId).distinct()
    }
}

enum class WeightChartRange(val label: String, val days: Int?) {
    Last7("7", 7),
    Last10("10", 10),
    Last30("30", 30),
    Last90("90", 90),
    All("All", null),
    ;

    companion object {
        val hubChoices = listOf(Last7, Last30, Last90, All)
    }
}

data class DailyWeight(
    val date: String,
    val weight: Double,
    val sourceId: String,
)

data class WeightChartModel(
    val points: List<DailyWeight>,
    val startDate: String,
    val endDate: String,
    val trendByDate: Map<String, Double>,
)

data class WeightWeeklyRate(
    val lbPerWeek: Double,
    val days: Int,
)

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

fun lbToKg(lb: Double): Double = lb / LB_PER_KG

fun kgToLb(kg: Double): Double = kg * LB_PER_KG

fun displayWeight(lb: Double, useKg: Boolean): Double =
    if (useKg) roundToTenth(lbToKg(lb)) else roundToTenth(lb)

fun parseWeightInput(text: String, useKg: Boolean): Double? {
    val parsed = text.toDoubleOrNull() ?: return null
    return roundToTenth(if (useKg) kgToLb(parsed) else parsed)
}

fun weightUnit(useKg: Boolean): String = if (useKg) "kg" else "lb"

fun formatWeightValue(lb: Double, useKg: Boolean): String = formatWeight(displayWeight(lb, useKg))

fun formatWeightWithUnit(lb: Double, useKg: Boolean): String = "${formatWeightValue(lb, useKg)} ${weightUnit(useKg)}"

fun stepDisplayedWeight(currentLb: Double, deltaDisplay: Double, useKg: Boolean): Double {
    val nextDisplay = displayWeight(currentLb, useKg) + deltaDisplay
    val nextLb = if (useKg) kgToLb(nextDisplay) else nextDisplay
    return roundToTenth(nextLb.coerceIn(MIN_WEIGHT_LB, MAX_WEIGHT_LB))
}

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
    bmi < 35.0 -> BmiCategory.Obese1
    bmi < 40.0 -> BmiCategory.Obese2
    else -> BmiCategory.Obese3
}

fun formatBmiCaption(bmi: Double, compact: Boolean): String? {
    val category = bmiCategory(bmi) ?: return null
    val label = if (compact) category.shortLabel else category.label
    return "$label · CDC adult screening"
}

fun bloodPressureStatus(systolic: Int, diastolic: Int): BpStatus = when {
    systolic > 180 || diastolic > 120 -> BpStatus.Severe
    systolic >= 140 || diastolic >= 90 -> BpStatus.Stage2
    (systolic in 130..139) || (diastolic in 80..89) -> BpStatus.Stage1
    systolic in 120..129 && diastolic < 80 -> BpStatus.Elevated
    else -> BpStatus.Normal
}

fun pulsePressure(systolic: Int, diastolic: Int): Int = systolic - diastolic

fun meanArterialPressure(systolic: Int, diastolic: Int): Int =
    ((systolic + 2 * diastolic) / 3.0).roundToInt()

fun formatBloodPressure(systolic: Int, diastolic: Int, heartRate: Int? = null): String {
    val pressure = "$systolic / $diastolic mmHg"
    return if (heartRate == null) pressure else "$pressure · $heartRate bpm"
}

fun formatBloodPressureCompact(systolic: Int, diastolic: Int, heartRate: Int? = null): String {
    val pressure = "$systolic/$diastolic mmHg"
    return if (heartRate == null) pressure else "$pressure, $heartRate bpm"
}

fun formatDerivedBloodPressure(systolic: Int, diastolic: Int): String =
    "PP ${pulsePressure(systolic, diastolic)} · MAP ${meanArterialPressure(systolic, diastolic)}"

fun formatPreviousBloodPressure(systolic: Int, diastolic: Int, heartRate: Int?): String {
    val pressure = "$systolic / $diastolic"
    return if (heartRate == null) "Last time: $pressure." else "Last time: $pressure · $heartRate bpm."
}

data class BloodPressureDelta(val systolic: Int, val diastolic: Int)

fun bloodPressureDelta(readings: List<BloodPressureReading>): BloodPressureDelta? {
    val sorted = readings.sortedWith(
        compareByDescending<BloodPressureReading> { dateTimeKey(it.date, it.time) }.thenByDescending { it.id },
    )
    if (sorted.size < 2) return null
    return BloodPressureDelta(
        systolic = sorted[0].systolic - sorted[1].systolic,
        diastolic = sorted[0].diastolic - sorted[1].diastolic,
    )
}

fun formatBloodPressureDelta(delta: BloodPressureDelta): String {
    if (delta.systolic == 0 && delta.diastolic == 0) return "No change vs prior"
    return "${formatSigned(delta.systolic)} / ${formatSigned(delta.diastolic)} mmHg vs prior"
}

private fun formatSigned(value: Int): String = if (value > 0) "+$value" else value.toString()

data class BloodPressureValidation(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val heartRate: Int? = null,
    val errors: Map<String, String> = emptyMap(),
) {
    val ok: Boolean get() = errors.isEmpty() && systolic != null && diastolic != null
}

fun validateBloodPressureReading(
    date: String,
    time: String,
    systolicText: String,
    diastolicText: String,
    heartRateText: String,
): BloodPressureValidation {
    val errors = mutableMapOf<String, String>()
    if (date.isBlank()) errors["date"] = "Date is required."
    if (time.isBlank()) errors["time"] = "Time is required."
    val systolic = systolicText.toIntOrNull()
    val diastolic = diastolicText.toIntOrNull()
    if (systolic == null) errors["systolic"] = "Enter a whole-number systolic reading."
    else if (systolic !in MIN_SYSTOLIC..MAX_SYSTOLIC) errors["systolic"] = "Systolic must be $MIN_SYSTOLIC–$MAX_SYSTOLIC."
    if (diastolic == null) errors["diastolic"] = "Enter a whole-number diastolic reading."
    else if (diastolic !in MIN_DIASTOLIC..MAX_DIASTOLIC) errors["diastolic"] = "Diastolic must be $MIN_DIASTOLIC–$MAX_DIASTOLIC."
    else if (systolic != null && diastolic >= systolic) errors["diastolic"] = "Diastolic should be lower than systolic."
    val heartRateTextTrimmed = heartRateText.trim()
    val heartRate = if (heartRateTextTrimmed.isEmpty()) {
        null
    } else {
        val parsed = heartRateTextTrimmed.toIntOrNull()
        if (parsed == null) {
            errors["heartRate"] = "Enter a whole-number heart rate."
            null
        } else if (parsed !in MIN_HEART_RATE..MAX_HEART_RATE) {
            errors["heartRate"] = "Heart rate must be $MIN_HEART_RATE–$MAX_HEART_RATE."
            null
        } else {
            parsed
        }
    }
    return BloodPressureValidation(systolic, diastolic, heartRate, errors)
}

fun bloodPressureLowHint(systolic: Int, diastolic: Int): String? =
    if (systolic < LOW_SYSTOLIC || diastolic < LOW_DIASTOLIC) {
        "This is lower than the usual adult range. The AHA chart is for high blood pressure."
    } else {
        null
    }

fun systolicOnlyStatus(systolic: Int): BpStatus = when {
    systolic > 180 -> BpStatus.Severe
    systolic >= 140 -> BpStatus.Stage2
    systolic in 130..139 -> BpStatus.Stage1
    systolic in 120..129 -> BpStatus.Elevated
    else -> BpStatus.Normal
}

fun diastolicOnlyStatus(diastolic: Int): BpStatus = when {
    diastolic > 120 -> BpStatus.Severe
    diastolic >= 90 -> BpStatus.Stage2
    diastolic in 80..89 -> BpStatus.Stage1
    else -> BpStatus.Normal
}

fun isolatedBloodPressureNote(systolic: Int, diastolic: Int): String? {
    val combined = bloodPressureStatus(systolic, diastolic)
    if (combined == BpStatus.Normal) return null
    val sys = systolicOnlyStatus(systolic)
    val dia = diastolicOnlyStatus(diastolic)
    return when {
        sys == combined && dia != combined -> "Systolic is in ${combined.label}; diastolic is not."
        dia == combined && sys != combined -> "Diastolic is in ${combined.label}; systolic is not."
        else -> null
    }
}

fun bloodPressureSaveHint(systolic: Int, diastolic: Int): String? {
    val severe = bloodPressureStatus(systolic, diastolic) == BpStatus.Severe
    val pulse = pulsePressure(systolic, diastolic)
    val wide = pulse >= WIDE_PULSE_PRESSURE
    return when {
        severe && wide ->
            "This reading is Severe Hypertension, and pulse pressure is $pulse mm Hg. Wait 1 minute and take it again if you can."
        severe ->
            "This reading is Severe Hypertension. Wait 1 minute and take it again if you can."
        wide ->
            "Pulse pressure is $pulse mm Hg. A gap of $WIDE_PULSE_PRESSURE or more can be worth mentioning to a clinician."
        else -> null
    }
}

fun bloodPressureSaveWarning(systolic: Int, diastolic: Int): String? {
    val hint = bloodPressureSaveHint(systolic, diastolic) ?: return null
    val suffix = if (bloodPressureStatus(systolic, diastolic) == BpStatus.Severe) {
        "Save it anyway?"
    } else {
        "Save this reading?"
    }
    return "$hint $suffix"
}

/** 0–1 position inside the AHA category so the Home gauge follows status, not systolic alone. */
fun bloodPressureGaugeProgress(systolic: Int, diastolic: Int): Float {
    val inner = when (bloodPressureStatus(systolic, diastolic)) {
        BpStatus.Normal -> maxOf(unitProgress(systolic, 80, 120), unitProgress(diastolic, 40, 80))
        BpStatus.Elevated -> unitProgress(systolic, 120, 130)
        BpStatus.Stage1 -> maxOf(
            if (systolic in 130..139) unitProgress(systolic, 130, 140) else 0f,
            if (diastolic in 80..89) unitProgress(diastolic, 80, 90) else 0f,
        )
        BpStatus.Stage2 -> maxOf(
            if (systolic in 140..180) unitProgress(systolic, 140, 181) else 0f,
            if (diastolic in 90..120) unitProgress(diastolic, 90, 121) else 0f,
        )
        BpStatus.Severe -> maxOf(
            if (systolic > 180) unitProgress(systolic, 181, 201) else 0f,
            if (diastolic > 120) unitProgress(diastolic, 121, 151) else 0f,
        )
    }
    return inner.coerceIn(0f, 1f)
}

private fun unitProgress(value: Int, start: Int, end: Int): Float {
    val span = (end - start).toFloat().coerceAtLeast(1f)
    return ((value - start) / span).coerceIn(0f, 1f)
}

enum class BloodPressurePeriod(val id: String, val label: String) {
    Morning("morning", "Morning"),
    Evening("evening", "Evening"),
    Other("other", "Other");

    companion object {
        fun fromId(id: String?): BloodPressurePeriod? = entries.find { it.id == id }
    }
}

enum class BloodPressureTag(val id: String, val label: String) {
    LeftArm("left_arm", "Left arm"),
    RightArm("right_arm", "Right arm"),
    BeforeMedication("before_medication", "Before medication"),
    AfterMedication("after_medication", "After medication"),
    AfterExercise("after_exercise", "After exercise"),
    AfterCaffeine("after_caffeine", "After caffeine"),
    FeltUnwell("felt_unwell", "Felt unwell");

    companion object {
        fun fromId(id: String): BloodPressureTag? = entries.find { it.id == id }

        fun parse(ids: List<String>): List<BloodPressureTag> =
            ids.mapNotNull(::fromId).distinct()
    }
}

enum class BloodPressureChartRange(val label: String, val limit: Int?) {
    Last7("7", 7),
    Last10("10", 10),
    Last30("30", 30),
    All("All", null),
}

data class BloodPressureSave(
    val date: String,
    val time: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
    val period: String? = null,
    val pairId: String? = null,
    val averagedFrom: List<BloodPressureSample> = emptyList(),
    val tags: List<String> = emptyList(),
    val note: String = "",
)

data class BloodPressureMean(
    val systolic: Int,
    val diastolic: Int,
    val count: Int,
    val heartRate: Int? = null,
)

data class BloodPressureTargetProgress(
    val targetSystolic: Int,
    val targetDiastolic: Int,
    val last7Met: Int,
    val last7Total: Int,
    val last30Met: Int,
    val last30Total: Int,
)

data class BloodPressureVisitSummary(
    val count: Int,
    val meanSystolic: Int,
    val meanDiastolic: Int,
    val meanHeartRate: Int?,
    val status: BpStatus,
    val morning: BloodPressureMean?,
    val evening: BloodPressureMean?,
    val target: BloodPressureTargetProgress?,
)

fun inferBloodPressurePeriod(time: String): BloodPressurePeriod {
    val hour = runCatching { LocalTime.parse(time).hour }.getOrNull()
        ?: time.substringBefore(":").toIntOrNull()
        ?: return BloodPressurePeriod.Other
    return when {
        hour < 12 -> BloodPressurePeriod.Morning
        hour >= 16 -> BloodPressurePeriod.Evening
        else -> BloodPressurePeriod.Other
    }
}

fun bloodPressurePeriod(reading: BloodPressureReading): BloodPressurePeriod =
    BloodPressurePeriod.fromId(reading.period) ?: inferBloodPressurePeriod(reading.time)

fun averageBloodPressureSamples(samples: List<BloodPressureSample>): BloodPressureSample? {
    if (samples.isEmpty()) return null
    val heartRates = samples.mapNotNull { it.heartRate }
    return BloodPressureSample(
        systolic = samples.map { it.systolic }.average().roundToInt(),
        diastolic = samples.map { it.diastolic }.average().roundToInt(),
        heartRate = if (heartRates.isEmpty()) null else heartRates.average().roundToInt(),
    )
}

fun meanBloodPressure(readings: List<BloodPressureReading>): BloodPressureMean? {
    if (readings.isEmpty()) return null
    val heartRates = readings.mapNotNull { it.heartRate }
    return BloodPressureMean(
        systolic = readings.map { it.systolic }.average().roundToInt(),
        diastolic = readings.map { it.diastolic }.average().roundToInt(),
        count = readings.size,
        heartRate = if (heartRates.isEmpty()) null else heartRates.average().roundToInt(),
    )
}

fun bloodPressureInLastDays(
    readings: List<BloodPressureReading>,
    days: Int,
    today: String = todayIso(),
): List<BloodPressureReading> {
    val end = runCatching { LocalDate.parse(today) }.getOrNull() ?: return emptyList()
    val start = end.minusDays((days - 1).coerceAtLeast(1).toLong()).toString()
    return chronologicalBp(readings).filter { it.date in start..today }
}

fun meetsBloodPressureTarget(reading: BloodPressureReading, targetSystolic: Int, targetDiastolic: Int): Boolean =
    reading.systolic <= targetSystolic && reading.diastolic <= targetDiastolic

fun bloodPressureVisitSummary(
    readings: List<BloodPressureReading>,
    targetSystolic: Int?,
    targetDiastolic: Int?,
    today: String = todayIso(),
): BloodPressureVisitSummary? {
    val last7 = bloodPressureInLastDays(readings, 7, today)
    val mean = meanBloodPressure(last7) ?: return null
    val morning = meanBloodPressure(last7.filter { bloodPressurePeriod(it) == BloodPressurePeriod.Morning })
    val evening = meanBloodPressure(last7.filter { bloodPressurePeriod(it) == BloodPressurePeriod.Evening })
    val last30 = bloodPressureInLastDays(readings, 30, today)
    val target = if (targetSystolic != null && targetDiastolic != null) {
        BloodPressureTargetProgress(
            targetSystolic = targetSystolic,
            targetDiastolic = targetDiastolic,
            last7Met = last7.count { meetsBloodPressureTarget(it, targetSystolic, targetDiastolic) },
            last7Total = last7.size,
            last30Met = last30.count { meetsBloodPressureTarget(it, targetSystolic, targetDiastolic) },
            last30Total = last30.size,
        )
    } else {
        null
    }
    return BloodPressureVisitSummary(
        count = mean.count,
        meanSystolic = mean.systolic,
        meanDiastolic = mean.diastolic,
        meanHeartRate = mean.heartRate,
        status = bloodPressureStatus(mean.systolic, mean.diastolic),
        morning = morning,
        evening = evening,
        target = target,
    )
}

fun formatVisitSummaryLine(summary: BloodPressureVisitSummary): String =
    "7-day average is ${summary.status.label} · ${summary.meanSystolic} / ${summary.meanDiastolic}"

fun formatBloodPressureTarget(systolic: Int?, diastolic: Int?): String =
    if (systolic == null || diastolic == null) "Not set" else "$systolic / $diastolic"

fun formatBloodPressureTags(ids: List<String>): String =
    BloodPressureTag.parse(ids).joinToString(", ") { it.label }

fun filterBloodPressureReadings(
    readings: List<BloodPressureReading>,
    period: BloodPressurePeriod? = null,
    tags: Collection<String> = emptyList(),
): List<BloodPressureReading> {
    val required = BloodPressureTag.parse(tags.toList()).map { it.id }
    return readings.filter { reading ->
        (period == null || bloodPressurePeriod(reading) == period) &&
            required.all { tag -> tag in reading.tags }
    }
}

fun formatBloodPressureHistoryLine(reading: BloodPressureReading): String {
    val pair = if (reading.averagedFrom.size >= 2) "2 readings" else null
    val tags = formatBloodPressureTags(reading.tags).takeIf { it.isNotEmpty() }
    val note = reading.note.trim().takeIf { it.isNotEmpty() }
    return listOfNotNull(
        "${formatDate(reading.date)} · ${formatTime(reading.time)} · ${bloodPressurePeriod(reading).label}",
        pair,
        tags,
        note,
    ).joinToString(" · ")
}

fun formatBloodPressureReminders(
    morning: Boolean,
    evening: Boolean,
    morningTime: String,
    eveningTime: String,
): String {
    val parts = buildList {
        if (morning) add("Morning ${formatTime(morningTime)}")
        if (evening) add("Evening ${formatTime(eveningTime)}")
    }
    return parts.joinToString(" · ").ifEmpty { "Off" }
}

fun formatWeightReminder(enabled: Boolean, time: String): String =
    if (enabled) "Morning ${formatTime(time.ifBlank { DEFAULT_WEIGHT_MORNING_REMINDER })}" else "Off"

fun formatWorkoutReminder(enabled: Boolean, time: String): String =
    if (enabled) "Morning ${formatTime(time.ifBlank { DEFAULT_WORKOUT_REMINDER })}" else "Off"

fun formatWorkoutRestTimer(sec: Int): String = if (sec <= 0) "Off" else "${sec}s"

fun formatRestClock(remainingSec: Int): String {
    val safe = remainingSec.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

fun buildBloodPressureReport(
    readings: List<BloodPressureReading>,
    targetSystolic: Int?,
    targetDiastolic: Int?,
    generatedAt: String,
    today: String = todayIso(),
): String {
    val last7 = bloodPressureInLastDays(readings, 7, today)
    val last30 = bloodPressureInLastDays(readings, 30, today)
    val summary7 = bloodPressureVisitSummary(readings, targetSystolic, targetDiastolic, today)
    val mean30 = meanBloodPressure(last30)
    val listed = if (last30.isNotEmpty()) last30.asReversed() else chronologicalBp(readings).takeLast(30).asReversed()
    return buildString {
        appendLine("Life Metric blood pressure report")
        appendLine("Generated $generatedAt")
        appendLine("This is a personal log, not a diagnosis.")
        appendLine()
        if (summary7 != null) {
            appendLine("Last 7 days")
            appendLine("Average ${summary7.meanSystolic} / ${summary7.meanDiastolic} mmHg${summary7.meanHeartRate?.let { " · $it bpm" }.orEmpty()}")
            appendLine("7-day average is ${summary7.status.label} (${summary7.count} ${if (summary7.count == 1) "reading" else "readings"})")
            summary7.morning?.let { appendLine("Morning average ${it.systolic} / ${it.diastolic} (${it.count})") }
            summary7.evening?.let { appendLine("Evening average ${it.systolic} / ${it.diastolic} (${it.count})") }
            if (last7.isNotEmpty()) {
                appendLine("Min ${last7.minOf { it.systolic }}/${last7.minOf { it.diastolic }} · Max ${last7.maxOf { it.systolic }}/${last7.maxOf { it.diastolic }}")
            }
            summary7.target?.let {
                appendLine("At or under ${it.targetSystolic} / ${it.targetDiastolic}: ${it.last7Met} of ${it.last7Total} in 7 days, ${it.last30Met} of ${it.last30Total} in 30 days")
            }
            appendLine()
        }
        if (mean30 != null && last30.size != last7.size) {
            appendLine("Last 30 days")
            appendLine("Average ${mean30.systolic} / ${mean30.diastolic} mmHg${mean30.heartRate?.let { " · $it bpm" }.orEmpty()} (${mean30.count} readings)")
            appendLine()
        }
        appendLine("Readings")
        if (listed.isEmpty()) {
            appendLine("None")
        } else {
            listed.forEach { reading ->
                val tags = formatBloodPressureTags(reading.tags)
                val note = reading.note.trim()
                append(reading.date)
                append(' ')
                append(reading.time)
                append("  ")
                append("${reading.systolic}/${reading.diastolic}")
                reading.heartRate?.let { append("  ${it} bpm") }
                append("  ")
                append(bloodPressurePeriod(reading).label)
                if (tags.isNotEmpty()) append("  $tags")
                if (note.isNotEmpty()) append("  $note")
                appendLine()
            }
        }
    }.trimEnd()
}

fun buildBloodPressureCsv(readings: List<BloodPressureReading>): String {
    val header = "date,time,systolic,diastolic,heartRate,period,tags,note"
    val rows = chronologicalBp(readings).map { reading ->
        listOf(
            reading.date,
            reading.time,
            reading.systolic.toString(),
            reading.diastolic.toString(),
            reading.heartRate?.toString().orEmpty(),
            bloodPressurePeriod(reading).id,
            BloodPressureTag.parse(reading.tags).joinToString("|") { it.id },
            csvEscape(reading.note.trim()),
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun csvEscape(value: String): String {
    if (value.none { it == ',' || it == '"' || it == '\n' }) return value
    return "\"${value.replace("\"", "\"\"")}\""
}

fun chartBloodPressure(readings: List<BloodPressureReading>, range: BloodPressureChartRange): List<BloodPressureReading> {
    val chronological = chronologicalBp(readings)
    val limit = range.limit ?: return chronological
    return chronological.takeLast(limit)
}

data class BloodPressureTargetResult(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val error: String? = null,
) {
    val ok: Boolean get() = error == null
}

fun validateBloodPressureTarget(systolicText: String, diastolicText: String): BloodPressureTargetResult {
    val sysText = systolicText.trim()
    val diaText = diastolicText.trim()
    if (sysText.isEmpty() && diaText.isEmpty()) return BloodPressureTargetResult()
    if (sysText.isEmpty() || diaText.isEmpty()) {
        return BloodPressureTargetResult(error = "Enter both systolic and diastolic, or clear both to remove the target.")
    }
    val systolic = sysText.toIntOrNull()
    val diastolic = diaText.toIntOrNull()
    if (systolic == null || diastolic == null) {
        return BloodPressureTargetResult(error = "Enter whole-number target values.")
    }
    if (systolic !in MIN_SYSTOLIC..MAX_SYSTOLIC) {
        return BloodPressureTargetResult(error = "Systolic must be $MIN_SYSTOLIC–$MAX_SYSTOLIC.")
    }
    if (diastolic !in MIN_DIASTOLIC..MAX_DIASTOLIC) {
        return BloodPressureTargetResult(error = "Diastolic must be $MIN_DIASTOLIC–$MAX_DIASTOLIC.")
    }
    if (diastolic >= systolic) {
        return BloodPressureTargetResult(error = "Diastolic should be lower than systolic.")
    }
    return BloodPressureTargetResult(systolic, diastolic)
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

data class WeightTrendPoint(
    val day: DailyWeight,
    val trendLb: Double,
)

/** Lowest scale reading on each calendar day, oldest first. */
fun dailyWeights(records: List<WeightRecord>): List<DailyWeight> =
    chronologicalWeights(records)
        .groupBy { it.date }
        .map { (date, dayRecords) ->
            val lowest = dayRecords.minWith(compareBy<WeightRecord> { it.weight }.thenBy { it.time }.thenBy { it.id })
            DailyWeight(date, lowest.weight, lowest.id)
        }
        .sortedBy { it.date }

fun weightTrendSeries(
    records: List<WeightRecord>,
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
): List<WeightTrendPoint> {
    val daily = dailyWeights(records)
    val averages = trailingMovingAverage(daily.map { it.weight }, window)
    return daily.mapIndexed { index, day -> WeightTrendPoint(day, averages[index]) }
}

fun latestWeightTrend(
    records: List<WeightRecord>,
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
): Double? = weightTrendSeries(records, window).lastOrNull()?.trendLb

/**
 * Change in the 7-day trend versus the trend from about [window] days earlier.
 * Null until at least two daily points exist.
 */
fun weightTrendDelta(
    records: List<WeightRecord>,
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
    today: String = todayIso(),
): Double? {
    val series = weightTrendSeries(records, window)
    if (series.size < 2) return null
    val lastDate = parseIsoDate(series.last().day.date) ?: parseIsoDate(today) ?: return null
    val target = lastDate.minusDays(window.toLong())
    val priorIndex = series.indexOfLast { day ->
        val date = parseIsoDate(day.day.date) ?: return@indexOfLast false
        !date.isAfter(target)
    }.takeIf { it >= 0 } ?: 0
    if (priorIndex == series.lastIndex) return null
    return roundToTenth(series.last().trendLb - series[priorIndex].trendLb)
}

fun formatWeightTrendDelta(deltaLb: Double, useKg: Boolean = false, compact: Boolean = false): String {
    val delta = formatDelta(displayWeight(deltaLb, useKg), weightUnit(useKg))
    return if (compact) delta else "$delta vs 7-day trend"
}

fun weightChartModel(
    records: List<WeightRecord>,
    range: WeightChartRange,
    today: String = todayIso(),
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
): WeightChartModel? {
    val daily = dailyWeights(records)
    if (daily.isEmpty()) return null
    val todayDate = parseIsoDate(today) ?: return null
    val lastDate = parseIsoDate(daily.last().date) ?: todayDate
    val firstDate = parseIsoDate(daily.first().date) ?: lastDate
    val end = if (range.days == null) lastDate else todayDate
    val start = if (range.days == null) firstDate else end.minusDays((range.days - 1).toLong())
    val trendByDate = weightTrendSeries(records, window).associate { it.day.date to it.trendLb }
    val points = daily.filter { day ->
        val date = parseIsoDate(day.date) ?: return@filter false
        !date.isBefore(start) && !date.isAfter(end)
    }
    return WeightChartModel(
        points = points,
        startDate = start.toString(),
        endDate = end.toString(),
        trendByDate = trendByDate,
    )
}

fun homeWeightChartModel(
    records: List<WeightRecord>,
    limit: Int = 10,
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
): WeightChartModel? {
    val daily = dailyWeights(records).takeLast(limit)
    if (daily.isEmpty()) return null
    return WeightChartModel(
        points = daily,
        startDate = daily.first().date,
        endDate = daily.last().date,
        trendByDate = weightTrendSeries(records, window).associate { it.day.date to it.trendLb },
    )
}

fun weightWeeklyRate(
    records: List<WeightRecord>,
    lookbackDays: Int = WEIGHT_RATE_LOOKBACK_DAYS,
    today: String = todayIso(),
    window: Int = DEFAULT_WEIGHT_TREND_WINDOW,
): WeightWeeklyRate? {
    val series = weightTrendSeries(records, window)
    if (series.size < 2) return null
    val lastDate = parseIsoDate(series.last().day.date) ?: parseIsoDate(today) ?: return null
    val target = lastDate.minusDays(lookbackDays.toLong())
    val priorIndex = series.indexOfLast { day ->
        val date = parseIsoDate(day.day.date) ?: return@indexOfLast false
        !date.isAfter(target)
    }.takeIf { it >= 0 } ?: 0
    if (priorIndex == series.lastIndex) return null
    val priorDate = parseIsoDate(series[priorIndex].day.date) ?: return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(priorDate, lastDate).toInt()
    if (days < WEIGHT_RATE_MIN_DAYS) return null
    val change = series.last().trendLb - series[priorIndex].trendLb
    return WeightWeeklyRate(lbPerWeek = roundToTenth(change * 7.0 / days), days = days)
}

fun formatWeightWeeklyRate(rate: WeightWeeklyRate, useKg: Boolean = false): String =
    "${formatDelta(displayWeight(rate.lbPerWeek, useKg), weightUnit(useKg))} / week"

fun formatWeightRateCaption(days: Int): String =
    "Over the last $days days of the 7-day trend. CDC suggests about 1–2 lb per week when losing."

fun weightGoalEtaDate(
    records: List<WeightRecord>,
    startId: String?,
    targetLb: Double?,
    today: String = todayIso(),
): String? {
    val progress = weightGoalProgress(records, startId, targetLb) ?: return null
    if (progress.kind == WeightGoalKind.Maintain) return null
    val trend = latestWeightTrend(records) ?: return null
    val rate = weightWeeklyRate(records, today = today) ?: return null
    val remaining = when (progress.kind) {
        WeightGoalKind.Lose -> trend - progress.targetLb
        WeightGoalKind.Gain -> progress.targetLb - trend
        WeightGoalKind.Maintain -> return null
    }
    if (remaining <= 0.05) return null
    val toward = when (progress.kind) {
        WeightGoalKind.Lose -> rate.lbPerWeek <= -WEIGHT_ETA_MIN_LB_PER_WEEK
        WeightGoalKind.Gain -> rate.lbPerWeek >= WEIGHT_ETA_MIN_LB_PER_WEEK
        WeightGoalKind.Maintain -> false
    }
    if (!toward) return null
    val weeks = remaining / kotlin.math.abs(rate.lbPerWeek)
    if (weeks > 52 * 5) return null
    val start = parseIsoDate(today) ?: return null
    return start.plusDays((weeks * 7.0).roundToInt().toLong()).toString()
}

fun formatWeightGoalEta(isoDate: String): String {
    val formatted = runCatching {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    }.getOrDefault(isoDate)
    return "At this rate, around $formatted."
}

fun tenPercentLossTarget(startLb: Double): Double = roundToTenth(startLb * 0.9)

fun formatTenPercentGoalHint(startLb: Double, useKg: Boolean = false): String =
    "10% of start is ${formatWeightWithUnit(tenPercentLossTarget(startLb), useKg)}"

fun inferWeightTag(time: String): WeightTag? = when (inferBloodPressurePeriod(time)) {
    BloodPressurePeriod.Morning -> WeightTag.Morning
    BloodPressurePeriod.Evening -> WeightTag.Evening
    BloodPressurePeriod.Other -> null
}

fun weightRecordTags(record: WeightRecord): List<WeightTag> {
    val stored = WeightTag.parse(record.tags)
    val hasPeriod = stored.any { it == WeightTag.Morning || it == WeightTag.Evening }
    val inferred = if (hasPeriod) null else inferWeightTag(record.time)
    return (stored + listOfNotNull(inferred)).distinct()
}

fun formatWeightTags(record: WeightRecord): String =
    weightRecordTags(record).joinToString(", ") { it.label }

fun formatWeightHistoryLine(record: WeightRecord): String {
    val tags = formatWeightTags(record).takeIf { it.isNotEmpty() }
    val note = record.note.trim().takeIf { it.isNotEmpty() }
    return listOfNotNull("${formatDate(record.date)} · ${formatTime(record.time)}", tags, note)
        .joinToString(" · ")
}

fun filterWeightRecords(
    records: List<WeightRecord>,
    range: WeightChartRange = WeightChartRange.All,
    tags: Collection<String> = emptyList(),
    today: String = todayIso(),
): List<WeightRecord> {
    val required = WeightTag.parse(tags.toList())
    val inRange = weightRecordsInRange(records, range, today)
    if (required.isEmpty()) return inRange
    return inRange.filter { record ->
        val present = weightRecordTags(record)
        required.all { it in present }
    }
}

fun weightRecordsInRange(
    records: List<WeightRecord>,
    range: WeightChartRange,
    today: String = todayIso(),
): List<WeightRecord> {
    val model = weightChartModel(records, range, today) ?: return emptyList()
    val start = parseIsoDate(model.startDate) ?: return records
    val end = parseIsoDate(model.endDate) ?: return records
    return records.filter { record ->
        val date = parseIsoDate(record.date) ?: return@filter false
        !date.isBefore(start) && !date.isAfter(end)
    }
}

fun formatWaist(inches: Double?): String =
    if (inches == null) "Not set" else "${formatWeight(inches)} in"

fun formatWaistCaption(waistInches: Double?): String {
    val yours = waistInches?.let { "Your waist is ${formatWeight(it)} in. " }.orEmpty()
    return yours + "NHLBI high-risk cutoffs are above ${WAIST_HIGH_MEN_IN.toInt()} in for men and above ${WAIST_HIGH_WOMEN_IN.toInt()} in for women."
}

fun validateWaistInches(text: String): Pair<Double?, String?> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null to null
    val parsed = trimmed.toDoubleOrNull() ?: return null to "Enter a waist measurement."
    if (parsed < MIN_WAIST_IN || parsed > MAX_WAIST_IN) {
        return null to "Waist must be ${MIN_WAIST_IN.toInt()}–${MAX_WAIST_IN.toInt()} in."
    }
    return roundToTenth(parsed) to null
}

fun buildWeightCsv(records: List<WeightRecord>): String {
    val header = "date,time,weightLb,tags,note"
    val rows = chronologicalWeights(records).map { record ->
        listOf(
            record.date,
            record.time,
            formatWeight(record.weight),
            WeightTag.parse(record.tags).joinToString("|") { it.id },
            csvEscape(record.note.trim()),
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

fun buildWeightReport(
    records: List<WeightRecord>,
    generatedAt: String,
): String = buildString {
    appendLine("Life Metric weight log")
    appendLine(generatedAt)
    appendLine()
    if (records.isEmpty()) {
        appendLine("No readings in this range.")
        return@buildString
    }
    appendLine("${records.size} reading${if (records.size == 1) "" else "s"}")
    appendLine()
    chronologicalWeights(records).forEach { record ->
        append("${formatDate(record.date)} ${formatTime(record.time)}  ${formatWeight(record.weight)} lb")
        val tags = formatWeightTags(record)
        if (tags.isNotEmpty()) append("  $tags")
        val note = record.note.trim()
        if (note.isNotEmpty()) append("  $note")
        appendLine()
    }
}

private fun parseIsoDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

fun weightChangeDirection(weight: Double, previous: Double?): WeightChangeDirection? {
    if (previous == null) return null
    val delta = weight - previous
    return when {
        kotlin.math.abs(delta) < WEIGHT_STABLE_LB -> WeightChangeDirection.Unchanged
        delta > 0 -> WeightChangeDirection.Up
        else -> WeightChangeDirection.Down
    }
}

fun validateWeightLb(value: Double?): String? = when {
    value == null -> "Enter a weight value."
    value < MIN_WEIGHT_LB || value > MAX_WEIGHT_LB ->
        "Weight must be ${MIN_WEIGHT_LB.toInt()}–${MAX_WEIGHT_LB.toInt()} lb."
    else -> null
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
    val kind: WeightGoalKind,
)

fun weightGoalKind(startLb: Double, targetLb: Double): WeightGoalKind = when {
    kotlin.math.abs(startLb - targetLb) < 0.05 -> WeightGoalKind.Maintain
    startLb > targetLb -> WeightGoalKind.Lose
    else -> WeightGoalKind.Gain
}

fun weightGoalTargetLabel(kind: WeightGoalKind): String = when (kind) {
    WeightGoalKind.Lose -> "Target to lose"
    WeightGoalKind.Gain -> "Target to gain"
    WeightGoalKind.Maintain -> "Maintain"
}

fun weightGoalProgressLabel(kind: WeightGoalKind): String = when (kind) {
    WeightGoalKind.Lose -> "Weight lost"
    WeightGoalKind.Gain -> "Weight gained"
    WeightGoalKind.Maintain -> "Distance from target"
}

fun WeightGoalProgress.targetChangeLb(): Double = when (kind) {
    WeightGoalKind.Lose -> roundToTenth(startLb - targetLb)
    WeightGoalKind.Gain -> roundToTenth(targetLb - startLb)
    WeightGoalKind.Maintain -> roundToTenth(targetLb)
}

fun WeightGoalProgress.progressChangeLb(): Double = when (kind) {
    WeightGoalKind.Lose -> roundToTenth(startLb - currentLb)
    WeightGoalKind.Gain -> roundToTenth(currentLb - startLb)
    WeightGoalKind.Maintain -> roundToTenth(kotlin.math.abs(currentLb - targetLb))
}

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
        kind = weightGoalKind(start.weight, targetLb),
    )
}

private fun weekStart(iso: String): LocalDate {
    val date = LocalDate.parse(iso)
    val diff = if (date.dayOfWeek.value == 7) -6 else 1 - date.dayOfWeek.value
    return date.plusDays(diff.toLong())
}

data class WeekBucket(val label: String, val count: Int, val volumeLb: Double = 0.0)

data class WorkoutWeekSummary(
    val strengthDays: Int,
    val aerobicMinutes: Int,
    val strengthTarget: Int = HHS_STRENGTH_DAYS,
    val aerobicTarget: Int = HHS_AEROBIC_MINUTES,
)

fun workoutSessionsByWeek(
    workouts: List<Workout>,
    weeks: Int = 8,
    today: String = todayIso(),
): List<WeekBucket> {
    val todayDate = parseIsoDate(today) ?: LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val buckets = (weeks - 1 downTo 0).map { offset ->
        val start = todayDate.minusDays(offset.toLong() * 7)
        val key = weekStart(start.toString())
        key to WeekBucket(start.format(formatter), 0)
    }.toMutableList()
    val index = buckets.mapIndexed { i, pair -> pair.first to i }.toMap()
    workouts.forEach { workout ->
        runCatching { weekStart(workout.date) }.getOrNull()?.let { key ->
            val i = index[key] ?: return@forEach
            val current = buckets[i]
            buckets[i] = key to current.second.copy(
                count = current.second.count + 1,
                volumeLb = current.second.volumeLb + sessionVolumeLb(workout.entries),
            )
        }
    }
    return buckets.map { it.second }
}

fun isStrengthSession(workout: Workout, plan: FitnessPlan?): Boolean {
    if (workout.entries.any { it.schema == "load" && !isHollowWorkoutEntry(it) }) return true
    val typeId = workout.activityTypeId.ifBlank { plan?.activityTypeId }.orEmpty()
    return typeId.isNotBlank() && ActivityCatalog.familyOf(typeId) == "strength_resistance"
}

fun aerobicSeconds(entry: WorkoutEntry): Int = when (entry.schema) {
    "distance_time", "duration", "sport" -> (entry.durationSec ?: 0).coerceAtLeast(0)
    "interval" -> {
        val work = (entry.workSec ?: 0).coerceAtLeast(0)
        val rounds = (entry.rounds ?: 0).coerceAtLeast(0)
        if (work > 0 && rounds > 0) work * rounds else 0
    }
    else -> 0
}

fun workoutWeekSummary(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    today: String = todayIso(),
): WorkoutWeekSummary {
    val start = runCatching { weekStart(today) }.getOrElse { return WorkoutWeekSummary(0, 0) }
    val startIso = start.toString()
    val endIso = start.plusDays(6).toString()
    val inWeek = workouts.filter { it.date >= startIso && it.date <= endIso }
    val strengthDays = inWeek
        .filter { workout -> isStrengthSession(workout, plans.find { it.id == workout.planId }) }
        .map { it.date }
        .toSet()
        .size
    val aerobicMinutes = (inWeek.sumOf { workout -> workout.entries.sumOf(::aerobicSeconds) } / 60.0).roundToInt()
    return WorkoutWeekSummary(strengthDays = strengthDays, aerobicMinutes = aerobicMinutes)
}

fun formatWorkoutWeekStrength(summary: WorkoutWeekSummary): String =
    "${summary.strengthDays} of ${summary.strengthTarget} strength days"

fun formatWorkoutWeekAerobic(summary: WorkoutWeekSummary): String =
    "${summary.aerobicMinutes} of ${summary.aerobicTarget} aerobic minutes"

fun formatWorkoutWeekCaption(): String =
    "HHS adult target is 150 minutes and 2 strength days. Some is better than none."

fun formatWorkoutWeekLine(summary: WorkoutWeekSummary): String =
    "${summary.strengthDays}/${summary.strengthTarget} strength days · ${summary.aerobicMinutes}/${summary.aerobicTarget} min"

fun formatVolumeTrendCaption(): String =
    "Volume is weight × reps × sets. Easy junk sets inflate it."

fun formatSessionDurationInput(durationSec: Int?): String {
    val sec = durationSec?.takeIf { it > 0 } ?: return ""
    if (sec % 60 == 0) return (sec / 60).toString()
    return formatDurationClock(sec)
}

fun sessionDurationLabel(durationSec: Int?): String? {
    val sec = durationSec?.takeIf { it > 0 } ?: return null
    return formatDurationLabel(sec).ifEmpty { null }
}

fun summarizeWorkout(workout: Workout, useKg: Boolean = false): String {
    val body = summarizeWorkoutEntries(workout.entries, useKg)
    val duration = sessionDurationLabel(workout.durationSec)
    val note = workout.note.trim().takeIf { it.isNotEmpty() }
    return listOfNotNull(body, duration, note).joinToString(" · ")
}

fun currentGroupLoad(entries: List<WorkoutEntry>): PreviousLoad? {
    val last = entries.lastOrNull { it.schema == "load" && !isHollowWorkoutEntry(it) && !isWarmupSet(it) } ?: return null
    return PreviousLoad(last.weight ?: 0.0, last.repetitions ?: 0)
}

fun formatActivityVsLastTime(
    current: PreviousLoad?,
    previous: PreviousLoad?,
    useKg: Boolean = false,
): String? {
    if (previous == null) return null
    if (current == null) return formatPreviousLoadSuggestion(previous, useKg)
    if (current.weight == previous.weight && current.repetitions == previous.repetitions) return null
    return "${formatLoadPair(current.weight, current.repetitions, useKg)} · last time ${formatLoadPair(previous.weight, previous.repetitions, useKg)}"
}

fun activityImproved(current: PreviousLoad, previous: PreviousLoad): Boolean =
    current.weight > previous.weight ||
        (current.weight == previous.weight && current.repetitions > previous.repetitions)

fun formatWorkoutVsLastTime(
    workout: Workout,
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    useKg: Boolean = false,
): String? {
    val machineId = workout.entries.firstOrNull { it.schema == "load" && !isHollowWorkoutEntry(it) }?.machineId
        ?: return null
    val current = currentGroupLoad(workout.entries.filter { it.machineId == machineId })
    val previous = findPreviousLoad(
        workouts = workouts,
        plans = plans,
        activityId = machineId,
        currentWorkoutDate = workout.date,
        currentEntries = emptyList(),
        currentWorkoutId = workout.id,
    )
    return formatActivityVsLastTime(current, previous, useKg)
}

fun workoutVsLastImproved(
    workout: Workout,
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
): Boolean {
    val machineId = workout.entries.firstOrNull { it.schema == "load" && !isHollowWorkoutEntry(it) }?.machineId
        ?: return false
    val current = currentGroupLoad(workout.entries.filter { it.machineId == machineId }) ?: return false
    val previous = findPreviousLoad(
        workouts = workouts,
        plans = plans,
        activityId = machineId,
        currentWorkoutDate = workout.date,
        currentEntries = emptyList(),
        currentWorkoutId = workout.id,
    ) ?: return false
    return activityImproved(current, previous)
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

fun latestWorkout(workouts: List<Workout>): Workout? =
    workouts.maxWithOrNull(compareBy<Workout> { it.date }.thenBy { it.id })

fun lastUsedPlanId(workouts: List<Workout>, plans: List<FitnessPlan>): String? {
    if (plans.isEmpty()) return null
    val last = latestWorkout(workouts)
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

fun formatLiftWeight(lb: Double, useKg: Boolean = false): String =
    if (useKg) formatWeightValue(lb, true) else formatLoadWeight(lb)

fun parseLiftWeightInput(text: String, useKg: Boolean, previousLb: Double? = null): Double? {
    val trimmed = text.trim()
    if (previousLb != null && formatLiftWeight(previousLb, useKg) == trimmed) return previousLb
    return parseWeightInput(trimmed, useKg)
}

fun formatVolume(volumeLb: Double, useKg: Boolean = false): String {
    val shown = displayWeight(volumeLb, useKg)
    val amount = if (!useKg && shown == shown.toInt().toDouble()) {
        String.format(Locale.US, "%,d", shown.toInt())
    } else {
        String.format(Locale.US, "%,.1f", shown)
    }
    return "$amount ${weightUnit(useKg)}"
}

fun formatLoadPair(weightLb: Double, repetitions: Int, useKg: Boolean = false): String =
    "${formatLiftWeight(weightLb, useKg)} × $repetitions"

fun formatPreviousLoadSuggestion(load: PreviousLoad, useKg: Boolean = false): String =
    "Last time: ${formatLiftWeight(load.weight, useKg)} ${weightUnit(useKg)} × ${load.repetitions}."

fun isWarmupSet(entry: WorkoutEntry): Boolean =
    WorkoutSetKind.fromId(entry.setKind) == WorkoutSetKind.Warmup

fun loadVolumeLb(entry: WorkoutEntry): Double {
    if (entry.schema != "load" || isWarmupSet(entry)) return 0.0
    return (entry.weight ?: 0.0) * (entry.repetitions ?: 0)
}

fun sessionVolumeLb(entries: List<WorkoutEntry>): Double = entries.sumOf(::loadVolumeLb)

fun epleyOneRepMaxLb(weightLb: Double, repetitions: Int): Double {
    if (weightLb <= 0) return 0.0
    if (repetitions <= 1) return weightLb
    return roundToTenth(weightLb * (1 + repetitions / 30.0))
}

fun activityPersonalRecord(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    activityId: String,
): ActivityRecord? {
    val matchingIds = activityIdsWithSameName(plans, activityId)
    if (matchingIds.isEmpty()) return null
    val best = workouts.asSequence()
        .flatMap { it.entries.asSequence() }
        .filter { it.machineId in matchingIds && it.schema == "load" && !isHollowWorkoutEntry(it) && !isWarmupSet(it) }
        .maxWithOrNull(compareBy<WorkoutEntry> { it.weight ?: 0.0 }.thenBy { it.repetitions ?: 0 })
        ?: return null
    val weight = best.weight ?: return null
    val reps = best.repetitions ?: return null
    return ActivityRecord(weight, reps, epleyOneRepMaxLb(weight, reps))
}

fun formatActivityRecord(record: ActivityRecord, useKg: Boolean = false): String {
    val pr = "PR ${formatLoadPair(record.weightLb, record.repetitions, useKg)}"
    val epley = "Epley 1RM ${formatLiftWeight(record.estimated1RmLb, useKg)} ${weightUnit(useKg)}"
    return "$pr · $epley. Estimate only."
}

fun filterWorkouts(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    range: WorkoutHistoryRange = WorkoutHistoryRange.All,
    planId: String? = null,
    familyId: String? = null,
    today: String = todayIso(),
): List<Workout> {
    val cutoff = range.days?.let { days ->
        parseIsoDate(today)?.minusDays((days - 1).toLong())?.toString()
    }
    return workouts.filter { workout ->
        if (cutoff != null && workout.date < cutoff) return@filter false
        if (planId != null && workout.planId != planId) return@filter false
        if (familyId != null) {
            val typeId = workout.activityTypeId.ifBlank {
                plans.find { it.id == workout.planId }?.activityTypeId.orEmpty()
            }
            if (ActivityCatalog.familyOf(typeId) != familyId) return@filter false
        }
        true
    }.sortedWith(compareByDescending<Workout> { it.date }.thenByDescending { it.id })
}

fun usedWorkoutPlans(workouts: List<Workout>, plans: List<FitnessPlan>): List<FitnessPlan> {
    val ids = workouts.map { it.planId }.toSet()
    return plans.filter { it.id in ids }
}

fun usedWorkoutFamilies(workouts: List<Workout>, plans: List<FitnessPlan>): List<ActivityFamily> {
    val ids = workouts.map { workout ->
        workout.activityTypeId.ifBlank { plans.find { it.id == workout.planId }?.activityTypeId }.orEmpty()
    }.filter { it.isNotBlank() }.map { ActivityCatalog.familyOf(it) }.toSet()
    return ActivityCatalog.families.filter { it.id in ids }
}

fun buildWorkoutCsv(workouts: List<Workout>, plans: List<FitnessPlan>): String {
    val header = "date,plan,activity,lb,reps,miles,seconds,note"
    val rows = workouts.sortedWith(compareBy<Workout> { it.date }.thenBy { it.id }).flatMap { workout ->
        val plan = plans.find { it.id == workout.planId }
        val sessionNote = workout.note.trim()
        if (workout.entries.isEmpty()) {
            listOf(
                listOf(
                    workout.date,
                    csvEscape(plan?.name.orEmpty()),
                    "",
                    "",
                    "",
                    "",
                    workout.durationSec?.takeIf { it > 0 }?.toString().orEmpty(),
                    csvEscape(sessionNote),
                ).joinToString(","),
            )
        } else {
            workout.entries.map { entry ->
                val activity = plan?.machines?.find { it.id == entry.machineId }?.name.orEmpty()
                val note = listOfNotNull(
                    sessionNote.takeIf { it.isNotEmpty() },
                    entry.note?.trim()?.takeIf { it.isNotEmpty() },
                ).joinToString(" | ")
                listOf(
                    workout.date,
                    csvEscape(plan?.name.orEmpty()),
                    csvEscape(activity),
                    entry.weight?.takeIf { entry.schema == "load" }?.let(::formatLoadWeight).orEmpty(),
                    entry.repetitions?.takeIf { entry.schema == "load" }?.toString().orEmpty(),
                    entry.distanceMi?.takeIf { it > 0 }?.let { milesLabel(it) }.orEmpty(),
                    entry.durationSec?.takeIf { it > 0 }?.toString()
                        ?: workout.durationSec?.takeIf { it > 0 }?.toString().orEmpty(),
                    csvEscape(note),
                ).joinToString(",")
            }
        }
    }
    return (listOf(header) + rows).joinToString("\n")
}

fun buildWorkoutReport(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    generatedAt: String,
): String = buildString {
    appendLine("Life Metric workout log")
    appendLine(generatedAt)
    appendLine()
    if (workouts.isEmpty()) {
        appendLine("No sessions in this range.")
        return@buildString
    }
    appendLine("${workouts.size} session${if (workouts.size == 1) "" else "s"}")
    appendLine()
    workouts.sortedWith(compareBy<Workout> { it.date }.thenBy { it.id }).forEach { workout ->
        val plan = plans.find { it.id == workout.planId }
        appendLine("${formatDate(workout.date)}  ${plan?.name ?: ActivityCatalog.workoutLabel(workout, plan)}")
        val summary = summarizeWorkout(workout)
        if (summary.isNotBlank()) appendLine("  $summary")
        workout.entries.forEach { entry ->
            val name = plan?.machines?.find { it.id == entry.machineId }?.name ?: "Activity"
            appendLine("  $name  ${formatWorkoutEntrySummary(entry)}")
        }
        appendLine()
    }
}

fun isHollowWorkoutEntry(entry: WorkoutEntry): Boolean = when (entry.schema) {
    "load" -> (entry.weight ?: 0.0) <= 0.0 && (entry.repetitions ?: 0) <= 0
    "distance_time" -> (entry.distanceMi ?: 0.0) <= 0.0 && (entry.durationSec ?: 0) <= 0
    "duration", "sport" -> (entry.durationSec ?: 0) <= 0
    "interval" -> (entry.rounds ?: 0) <= 0
    else -> false
}

fun hollowWorkoutEntryError(entry: WorkoutEntry): Pair<String, String>? {
    if (!isHollowWorkoutEntry(entry)) return null
    return when (entry.schema) {
        "load" -> "weight" to "Enter a weight and repetitions."
        "distance_time" -> "distanceMi" to "Enter a distance or time."
        "duration", "sport" -> "durationSec" to "Enter minutes greater than 0."
        "interval" -> "rounds" to "Enter a whole number of rounds."
        else -> null
    }
}

fun bestLoadSet(entries: List<WorkoutEntry>): WorkoutEntry? =
    entries.filter { it.schema == "load" && !isHollowWorkoutEntry(it) && !isWarmupSet(it) }
        .maxWithOrNull(compareBy<WorkoutEntry> { it.weight ?: 0.0 }.thenBy { it.repetitions ?: 0 })

fun shortPlanName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""
    return trimmed.substringBefore(" ")
}

fun formatWorkoutChipLine(workout: Workout, plan: FitnessPlan?, useKg: Boolean = false): String {
    val title = shortPlanName(plan?.name).ifBlank { ActivityCatalog.workoutLabel(workout, plan) }
    return "$title · ${summarizeWorkoutEntries(workout.entries, useKg)}"
}

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

fun activityIdsWithSameName(plans: List<FitnessPlan>, activityId: String): Set<String> {
    if (activityId.isBlank()) return emptySet()
    val activities = plans.flatMap { it.machines }
    val name = activities.find { it.id == activityId }?.name?.trim().orEmpty()
    if (name.isEmpty()) return setOf(activityId)
    return activities.filter { it.name.trim().equals(name, ignoreCase = true) }.map { it.id }.toSet()
}

/**
 * Latest saved entry for the most recent set whose activity has the same name.
 * Prefers a later date, then a later set in the same workout.
 */
fun findPreviousEntry(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    activityId: String,
    currentWorkoutDate: String,
    currentEntries: List<WorkoutEntry>,
    currentWorkoutId: String? = null,
    excludeEntryId: String? = null,
    schema: String? = null,
): WorkoutEntry? {
    val matchingIds = activityIdsWithSameName(plans, activityId)
    if (matchingIds.isEmpty()) return null
    data class Candidate(val date: String, val sequence: Int, val entry: WorkoutEntry)
    val candidates = mutableListOf<Candidate>()
    fun consider(entry: WorkoutEntry, date: String, sequence: Int) {
        if (entry.machineId !in matchingIds) return
        if (schema != null && entry.schema != schema) return
        if (excludeEntryId != null && entry.id == excludeEntryId) return
        if (isHollowWorkoutEntry(entry)) return
        if (isWarmupSet(entry)) return
        candidates += Candidate(date, sequence, entry)
    }
    workouts.forEach { workout ->
        if (currentWorkoutId != null && workout.id == currentWorkoutId) return@forEach
        workout.entries.forEachIndexed { index, entry -> consider(entry, workout.date, index) }
    }
    currentEntries.forEachIndexed { index, entry -> consider(entry, currentWorkoutDate, index) }
    return candidates.maxWithOrNull(compareBy<Candidate> { it.date }.thenBy { it.sequence })?.entry
}

fun findPreviousLoad(
    workouts: List<Workout>,
    plans: List<FitnessPlan>,
    activityId: String,
    currentWorkoutDate: String,
    currentEntries: List<WorkoutEntry>,
    currentWorkoutId: String? = null,
    excludeEntryId: String? = null,
): PreviousLoad? {
    val entry = findPreviousEntry(
        workouts = workouts,
        plans = plans,
        activityId = activityId,
        currentWorkoutDate = currentWorkoutDate,
        currentEntries = currentEntries,
        currentWorkoutId = currentWorkoutId,
        excludeEntryId = excludeEntryId,
        schema = "load",
    ) ?: return null
    return PreviousLoad(entry.weight ?: return null, entry.repetitions ?: return null)
}

fun formatPreviousActivitySuggestion(entry: WorkoutEntry, useKg: Boolean = false): String {
    val extras = buildList {
        if (entry.resistance > 0) add("Resistance ${formatLoadWeight(entry.resistance)}")
        if (entry.avgSpeedMph > 0) add("${formatLoadWeight(entry.avgSpeedMph)} mph")
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ", prefix = " · ").orEmpty()
    val core = when (entry.schema) {
        "load" -> return formatPreviousLoadSuggestion(
            PreviousLoad(entry.weight ?: 0.0, entry.repetitions ?: 0),
            useKg,
        )
        "distance_time" -> {
            val miles = entry.distanceMi ?: 0.0
            val sec = entry.durationSec ?: 0
            when {
                miles > 0 && sec > 0 -> "${milesLabel(miles)} mi · ${formatDurationClock(sec)}"
                miles > 0 -> "${milesLabel(miles)} mi"
                sec > 0 -> formatDurationClock(sec)
                else -> return ""
            }
        }
        "duration", "sport" -> formatDurationLabel(entry.durationSec ?: 0).ifEmpty { return "" }
        "interval" -> {
            val rounds = entry.rounds ?: 0
            listOfNotNull(
                "$rounds ${if (rounds == 1) "round" else "rounds"}",
                entry.workSec?.let { "${it}s work" },
                entry.restSec?.let { "${it}s rest" },
            ).joinToString(" · ")
        }
        else -> return ""
    }
    return "Last time: $core$extras."
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

fun distanceTimeLabel(miles: Double, durationSec: Int): String {
    val milesText = milesLabel(miles)
    val clock = formatDurationClock(durationSec)
    val base = if (clock.isEmpty()) "$milesText mi" else "$milesText mi · $clock"
    val pace = formatPace(miles, durationSec)
    return if (pace.isEmpty()) base else "$base · $pace"
}

fun formatWorkoutEntrySummary(entry: WorkoutEntry, useKg: Boolean = false): String {
    val base = when (entry.schema) {
        "load" -> {
            val reps = entry.repetitions ?: 0
            val weight = entry.weight ?: 0.0
            "${formatLiftWeight(weight, useKg)} ${weightUnit(useKg)} · $reps ${if (reps == 1) "rep" else "reps"}"
        }
        "distance_time" -> {
            val distanceTime = distanceTimeLabel(entry.distanceMi ?: 0.0, entry.durationSec ?: 0)
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
    val extras = listOfNotNull(
        WorkoutSetKind.fromId(entry.setKind).takeIf { it != WorkoutSetKind.Working }?.label,
        entry.rpe?.takeIf { it in 1..10 }?.let { "RPE $it" },
    )
    return if (extras.isEmpty()) base else "$base · ${extras.joinToString(" · ")}"
}

fun summarizeWorkoutEntries(entries: List<WorkoutEntry>, useKg: Boolean = false): String {
    if (entries.isEmpty()) return "Nothing logged"
    val schemas = entries.map { it.schema }.toSet()
    if (schemas.size == 1) {
        when (entries.first().schema) {
            "load" -> {
                val setText = "${entries.size} ${ActivityCatalog.entryNoun("load", entries.size)}"
                val volume = sessionVolumeLb(entries)
                val volumeText = if (volume > 0) " · ${formatVolume(volume, useKg)}" else ""
                val best = bestLoadSet(entries)
                val bestText = if (entries.size > 1 && best != null) {
                    " · best ${formatLoadPair(best.weight ?: 0.0, best.repetitions ?: 0, useKg)}"
                } else {
                    ""
                }
                return "$setText$volumeText$bestText"
            }
            "distance_time" -> {
                val miles = entries.sumOf { it.distanceMi ?: 0.0 }
                val sec = entries.sumOf { it.durationSec ?: 0 }
                val distanceTime = distanceTimeLabel(miles, sec)
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
                val work = entries.mapNotNull { it.workSec }.takeIf { it.isNotEmpty() }?.sum()
                val rest = entries.mapNotNull { it.restSec }.takeIf { it.isNotEmpty() }?.sum()
                val parts = listOfNotNull(
                    "$rounds ${if (rounds == 1) "round" else "rounds"}",
                    work?.let { "${it}s work" },
                    rest?.let { "${it}s rest" },
                )
                return parts.joinToString(" · ")
            }
            "note" -> {
                return if (entries.size == 1) formatWorkoutEntrySummary(entries.first(), useKg) else "${entries.size} notes"
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
            parsed < MIN_WEIGHT_LB || parsed > MAX_WEIGHT_LB -> {
                goalError = "Weight must be ${MIN_WEIGHT_LB.toInt()}–${MAX_WEIGHT_LB.toInt()} lb."
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
