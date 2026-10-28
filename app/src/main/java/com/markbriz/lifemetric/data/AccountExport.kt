package com.markbriz.lifemetric.data

import com.markbriz.lifemetric.domain.DEFAULT_BP_WINDOW
import com.markbriz.lifemetric.domain.MAX_WAIST_IN
import com.markbriz.lifemetric.domain.MIN_WAIST_IN
import com.markbriz.lifemetric.domain.clampBpWindow
import com.markbriz.lifemetric.domain.roundToTenth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Same file layout as the Expo app's `lib/accountExport.ts`. */
const val ACCOUNT_EXPORT_FORMAT = "life-metric-account"
const val ACCOUNT_EXPORT_VERSION = 1

private val UUID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", RegexOption.IGNORE_CASE)
private val LOGGING_SCHEMAS = setOf("load", "distance_time", "duration", "interval", "sport", "note")
private val SPORT_INTENSITIES = setOf("easy", "moderate", "hard")

class AccountExportException(message: String) : Exception(message)

data class ParsedAccountExport(
    val data: FitnessStore,
    /** Theme stored in the file, or null for a raw tracking blob. */
    val themeId: String?,
)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }
private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

fun accountExportFilename(now: LocalDateTime): String =
    "life-metric-%04d-%02d-%02d-%02d%02d%02d.json".format(
        now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute, now.second,
    )

fun buildAccountExport(store: FitnessStore, themeId: String, exportedAt: String): String {
    val root = buildJsonObject {
        put("format", ACCOUNT_EXPORT_FORMAT)
        put("version", ACCOUNT_EXPORT_VERSION)
        put("exportedAt", exportedAt)
        put("themeId", themeId)
        put("data", dataObject(store))
    }
    return prettyJson.encodeToString(JsonObject.serializer(), root)
}

private fun dataObject(store: FitnessStore): JsonObject = buildJsonObject {
    put("plans", buildJsonArray { store.plans.forEach { add(planObject(it)) } })
    put("workouts", buildJsonArray { store.workouts.forEach { add(workoutObject(it)) } })
    put(
        "weightRecords",
        buildJsonArray {
            store.weightRecords.forEach { record ->
                add(
                    buildJsonObject {
                        put("id", exportWeightId(record.id))
                        put("date", record.date)
                        put("time", record.time)
                        put("weight", roundToTenth(record.weight))
                        if (record.tags.isNotEmpty()) {
                            put("tags", buildJsonArray { record.tags.forEach { add(JsonPrimitive(it)) } })
                        }
                        if (record.note.isNotBlank()) put("note", record.note)
                    },
                )
            }
        },
    )
    put(
        "bloodPressureReadings",
        buildJsonArray {
            store.bloodPressureReadings.forEach { reading ->
                add(
                    buildJsonObject {
                        put("id", reading.id)
                        put("date", reading.date)
                        put("time", reading.time)
                        put("systolic", reading.systolic)
                        put("diastolic", reading.diastolic)
                        put("heartRate", reading.heartRate?.let { JsonPrimitive(it) } ?: JsonNull)
                        if (reading.period != null) put("period", reading.period)
                        if (reading.pairId != null) put("pairId", reading.pairId)
                        if (reading.averagedFrom.isNotEmpty()) {
                            put(
                                "averagedFrom",
                                buildJsonArray {
                                    reading.averagedFrom.forEach { sample ->
                                        add(
                                            buildJsonObject {
                                                put("systolic", sample.systolic)
                                                put("diastolic", sample.diastolic)
                                                put("heartRate", sample.heartRate?.let { JsonPrimitive(it) } ?: JsonNull)
                                            },
                                        )
                                    }
                                },
                            )
                        }
                        if (reading.tags.isNotEmpty()) {
                            put("tags", buildJsonArray { reading.tags.forEach { add(JsonPrimitive(it)) } })
                        }
                        if (reading.note.isNotBlank()) put("note", reading.note)
                    },
                )
            }
        },
    )
    put(
        "profile",
        buildJsonObject {
            put("firstName", store.profile.firstName)
            put("lastName", store.profile.lastName)
            put("heightInches", store.profile.heightInches?.let { JsonPrimitive(it) } ?: JsonNull)
            put("weightGoalLb", store.profile.weightGoalLb?.let { JsonPrimitive(it) } ?: JsonNull)
            put("bpMovingAverageReadings", store.profile.bpMovingAverageReadings)
            put(
                "weightGoalStartId",
                store.profile.weightGoalStartId?.let { JsonPrimitive(exportWeightId(it)) } ?: JsonNull,
            )
            put("bpTargetSystolic", store.profile.bpTargetSystolic?.let { JsonPrimitive(it) } ?: JsonNull)
            put("bpTargetDiastolic", store.profile.bpTargetDiastolic?.let { JsonPrimitive(it) } ?: JsonNull)
            put("bpMorningReminder", store.profile.bpMorningReminder)
            put("bpEveningReminder", store.profile.bpEveningReminder)
            put("bpMorningReminderTime", store.profile.bpMorningReminderTime)
            put("bpEveningReminderTime", store.profile.bpEveningReminderTime)
            put("weightMorningReminder", store.profile.weightMorningReminder)
            put("weightMorningReminderTime", store.profile.weightMorningReminderTime)
            put("weightDisplayKg", store.profile.weightDisplayKg)
            put("waistInches", store.profile.waistInches?.let { JsonPrimitive(it) } ?: JsonNull)
        },
    )
}

/** The Expo app stores weight rows under uuid ids; rewrite legacy prefixed ids so the file loads there too. */
private fun exportWeightId(id: String): String =
    if (UUID_PATTERN.matches(id)) id else UUID.nameUUIDFromBytes(id.toByteArray()).toString()

private fun planObject(plan: FitnessPlan): JsonObject = buildJsonObject {
    put("id", plan.id)
    put("name", plan.name)
    if (plan.description.isNotBlank()) put("description", plan.description)
    put("activityTypeId", plan.activityTypeId)
    put("category", plan.category)
    put(
        "machines",
        buildJsonArray {
            plan.machines.forEach { machine ->
                add(
                    buildJsonObject {
                        put("id", machine.id)
                        put("name", machine.name)
                        if (machine.description.isNotBlank()) put("description", machine.description)
                    },
                )
            }
        },
    )
}

private fun workoutObject(workout: Workout): JsonObject = buildJsonObject {
    put("id", workout.id)
    put("date", workout.date)
    put("planId", workout.planId)
    put("activityTypeId", workout.activityTypeId)
    put("entries", buildJsonArray { workout.entries.forEach { add(entryObject(it)) } })
}

private fun entryObject(entry: WorkoutEntry): JsonObject = buildJsonObject {
    put("id", entry.id)
    put("planId", entry.planId)
    put("machineId", entry.machineId)
    put("schema", entry.schema)
    when (entry.schema) {
        "load" -> {
            put("weight", entry.weight ?: 0.0)
            put("repetitions", entry.repetitions ?: 1)
        }
        "distance_time" -> {
            put("distanceMi", entry.distanceMi ?: 0.0)
            put("durationSec", entry.durationSec ?: 0)
        }
        "duration" -> put("durationSec", entry.durationSec ?: 0)
        "sport" -> {
            put("durationSec", entry.durationSec ?: 0)
            entry.intensity?.takeIf { it.isNotBlank() }?.let { put("intensity", it) }
        }
        "interval" -> {
            put("rounds", entry.rounds ?: 1)
            entry.workSec?.let { put("workSec", it) }
            entry.restSec?.let { put("restSec", it) }
        }
        else -> entry.note?.trim()?.takeIf { it.isNotEmpty() }?.let { put("note", it) }
    }
    put("resistance", roundToTenth(entry.resistance))
    put("avgSpeedMph", roundToTenth(entry.avgSpeedMph))
    put("avgRpm", entry.avgRpm)
    put("avgHeartRateBpm", entry.avgHeartRateBpm)
    put("maxHeartRateBpm", entry.maxHeartRateBpm)
}

fun parseAccountExport(raw: String): ParsedAccountExport {
    val parsed = try {
        lenientJson.parseToJsonElement(raw)
    } catch (_: Exception) {
        throw AccountExportException("This file is not valid JSON.")
    }
    val record = parsed as? JsonObject ?: throw AccountExportException("This file is not a Life Metric data export.")

    var payload: JsonElement = record
    var themeId: String? = null
    if (record.containsKey("format")) {
        if (record["format"].asString() != ACCOUNT_EXPORT_FORMAT) {
            throw AccountExportException("This file is not a Life Metric data export.")
        }
        if (record["version"].asDouble() != ACCOUNT_EXPORT_VERSION.toDouble()) {
            throw AccountExportException("This export is from a different version of Life Metric.")
        }
        payload = record["data"] ?: JsonNull
        themeId = record["themeId"].asString()?.takeIf { id -> THEME_IDS.contains(id) }
    }

    val data = normalizeAppData(payload) ?: throw AccountExportException("This file is not a Life Metric data export.")
    assertReadable(payload, data)

    return ParsedAccountExport(
        data = data.copy(
            workouts = data.workouts.sortedWith(compareByDescending<Workout> { it.date }.thenByDescending { it.id }),
            weightRecords = data.weightRecords.sortedWith(
                compareByDescending<WeightRecord> { "${it.date}T${it.time}" }.thenByDescending { it.id },
            ),
            bloodPressureReadings = data.bloodPressureReadings.sortedWith(
                compareByDescending<BloodPressureReading> { "${it.date}T${it.time}" }.thenByDescending { it.id },
            ),
        ),
        themeId = themeId,
    )
}

private val THEME_IDS = setOf("mint", "ocean", "sunset", "dark")

// ---- normalize (mirrors lib/fitnessLoadPlan.ts + utils/workout.ts) ----

private fun normalizeAppData(value: JsonElement): FitnessStore? {
    val data = value as? JsonObject ?: return null
    val plansRaw = data["plans"] as? JsonArray ?: return null
    val workoutsRaw = data["workouts"] as? JsonArray ?: return null
    val plans = plansRaw.mapNotNull(::parsePlan)
    val workouts = workoutsRaw.mapNotNull { parseWorkout(it, plans) }
    return FitnessStore(
        plans = plans,
        workouts = workouts,
        weightRecords = normalizeWeights(data["weightRecords"]),
        bloodPressureReadings = normalizeBloodPressure(data["bloodPressureReadings"]),
        profile = normalizeProfile(data["profile"]),
    )
}

private fun parseMachine(value: JsonElement): Machine? {
    val record = value as? JsonObject ?: return null
    val id = record["id"].asTrimmed() ?: return null
    val name = record["name"].asTrimmed() ?: return null
    return Machine(id, name, record["description"].asTrimmed().orEmpty())
}

private fun parsePlan(value: JsonElement): FitnessPlan? {
    val record = value as? JsonObject ?: return null
    val id = record["id"].asTrimmed() ?: return null
    val name = record["name"].asTrimmed() ?: return null
    val machinesRaw = record["machines"] as? JsonArray ?: return null
    val typeId = parseActivityTypeId(record["activityTypeId"].asString(), record["category"].asString())
    return FitnessPlan(
        id = id,
        name = name,
        description = record["description"].asTrimmed().orEmpty(),
        activityTypeId = typeId,
        category = ActivityCatalog.category(typeId),
        machines = machinesRaw.mapNotNull(::parseMachine),
    )
}

private val CATEGORY_TO_TYPE = mapOf(
    "strength" to "strength_training",
    "cardio" to "running",
    "conditioning" to "hiit",
    "mobility_flexibility" to "mobility_training",
    "sports" to "other_sports",
    "outdoor" to "hiking",
    "recovery" to "recovery_workout",
    "other" to "strength_training",
)

private fun parseActivityTypeId(value: String?, fallback: String?): String {
    if (value != null && ActivityCatalog.types.any { it.id == value }) return value
    if (fallback != null && ActivityCatalog.types.any { it.id == fallback }) return fallback
    if (fallback != null && CATEGORY_TO_TYPE.containsKey(fallback)) return CATEGORY_TO_TYPE.getValue(fallback)
    return ActivityCatalog.DEFAULT_TYPE
}

private fun parseEntry(value: JsonElement, plans: List<FitnessPlan>): WorkoutEntry? {
    val record = value as? JsonObject ?: return null
    val id = record["id"].asTrimmed() ?: return null
    val planId = record["planId"].asTrimmed() ?: return null
    val machineId = record["machineId"].asTrimmed() ?: return null
    val plan = plans.find { it.id == planId }
    val schema = record["schema"].asString()?.takeIf { it in LOGGING_SCHEMAS }
        ?: plan?.let { ActivityCatalog.schema(it.activityTypeId) }
        ?: "load"
    val base = WorkoutEntry(
        id = id,
        planId = planId,
        machineId = machineId,
        schema = schema,
        resistance = record["resistance"].asNonNegativeDouble(),
        avgSpeedMph = record["avgSpeedMph"].asNonNegativeDouble(),
        avgRpm = record["avgRpm"].asNonNegativeInt(),
        avgHeartRateBpm = record["avgHeartRateBpm"].asNonNegativeInt(),
        maxHeartRateBpm = record["maxHeartRateBpm"].asNonNegativeInt(),
    )
    return when (schema) {
        "load" -> {
            val weight = record["weight"].asDouble() ?: 0.0
            val reps = record["repetitions"].asDouble() ?: 0.0
            if (weight < 0 || reps < 0) return null
            base.copy(weight = weight, repetitions = reps.toInt())
        }
        "distance_time" -> {
            val miles = record["distanceMi"].asDouble()?.takeIf { it >= 0 } ?: 0.0
            val sec = durationFrom(record) ?: return null
            base.copy(distanceMi = miles, durationSec = sec)
        }
        "duration" -> base.copy(durationSec = durationFrom(record) ?: return null)
        "sport" -> {
            val sec = durationFrom(record) ?: return null
            val intensity = record["intensity"].asString()
            if (intensity != null && intensity !in SPORT_INTENSITIES) return null
            base.copy(durationSec = sec, intensity = intensity)
        }
        "interval" -> {
            val rounds = record["rounds"].asWhole()?.takeIf { it >= 0 } ?: 0
            val work = record["workSec"]
            val rest = record["restSec"]
            val workSec = if (work.isPresent()) work.asWhole()?.takeIf { it >= 1 } ?: return null else null
            val restSec = if (rest.isPresent()) rest.asWhole()?.takeIf { it >= 1 } ?: return null else null
            base.copy(rounds = rounds, workSec = workSec, restSec = restSec)
        }
        else -> base.copy(note = record["note"].asTrimmed())
    }
}

private fun durationFrom(record: JsonObject): Int? {
    val value = record["durationSec"].asDouble() ?: return 0
    if (value < 0) return null
    return value.toInt()
}

private fun parseWorkout(value: JsonElement, plans: List<FitnessPlan>): Workout? {
    val record = value as? JsonObject ?: return null
    val id = record["id"].asTrimmed() ?: return null
    val date = record["date"].asString()?.trim()?.take(10)?.takeIf(::isIsoDate) ?: return null
    val entriesRaw = record["entries"] as? JsonArray ?: return null
    val entries = entriesRaw.mapNotNull { parseEntry(it, plans) }
    val planId = record["planId"].asTrimmed() ?: entries.firstOrNull()?.planId ?: return null
    val plan = plans.find { it.id == planId }
    return Workout(
        id = id,
        date = date,
        planId = planId,
        activityTypeId = parseActivityTypeId(record["activityTypeId"].asString(), plan?.activityTypeId ?: plan?.category),
        entries = entries.map { if (it.planId == planId) it else it.copy(planId = planId) },
    )
}

private fun isIsoDate(value: String): Boolean =
    Regex("""^\d{4}-\d{2}-\d{2}$""").matches(value) && runCatching { LocalDate.parse(value) }.isSuccess

private fun normalizeWeights(value: JsonElement?): List<WeightRecord> {
    val array = value as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        val record = element as? JsonObject ?: return@mapNotNull null
        val id = record["id"].asString() ?: return@mapNotNull null
        val date = record["date"].asString() ?: return@mapNotNull null
        val weight = record["weight"].asNumber() ?: return@mapNotNull null
        val time = record["time"].asString()?.takeIf { it.isNotEmpty() } ?: "00:00"
        WeightRecord(
            id = id,
            date = date,
            time = time,
            weight = roundToTenth(weight),
            tags = normalizeStringList(record["tags"]),
            note = record["note"].asString().orEmpty(),
        )
    }
}

private fun normalizeBloodPressure(value: JsonElement?): List<BloodPressureReading> {
    val array = value as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        val record = element as? JsonObject ?: return@mapNotNull null
        val id = record["id"].asString() ?: return@mapNotNull null
        val date = record["date"].asString() ?: return@mapNotNull null
        val time = record["time"].asString() ?: return@mapNotNull null
        val systolic = record["systolic"].asNumber() ?: return@mapNotNull null
        val diastolic = record["diastolic"].asNumber() ?: return@mapNotNull null
        val heartRateElement = record["heartRate"]
        val heartRate = when {
            heartRateElement == null || heartRateElement is JsonNull -> null
            else -> {
                val value = heartRateElement.asNumber() ?: return@mapNotNull null
                if (value != Math.floor(value) || value < 1) return@mapNotNull null
                value.toInt()
            }
        }
        BloodPressureReading(
            id = id,
            date = date,
            time = time,
            systolic = systolic.toInt(),
            diastolic = diastolic.toInt(),
            heartRate = heartRate,
            period = record["period"].asString(),
            pairId = record["pairId"].asString(),
            averagedFrom = normalizeBloodPressureSamples(record["averagedFrom"]),
            tags = normalizeStringList(record["tags"]),
            note = record["note"].asString().orEmpty(),
        )
    }
}

private fun isProfile(value: JsonElement?): Boolean {
    val record = value as? JsonObject ?: return false
    if (record["firstName"].asString() == null || record["lastName"].asString() == null) return false
    val height = record["heightInches"]
    return height == null || height is JsonNull || (height.asNumber()?.let { it > 0 } == true)
}

private fun normalizeProfile(value: JsonElement?): UserProfile {
    if (!isProfile(value)) return UserProfile()
    val record = value as JsonObject
    val goal = record["weightGoalLb"].asNumber()?.takeIf { it >= 0 }?.let(::roundToTenth)
    val window = record["bpMovingAverageReadings"].asNumber()
        ?.takeIf { it == Math.floor(it) }?.toInt()?.let(::clampBpWindow) ?: DEFAULT_BP_WINDOW
    return UserProfile(
        firstName = record["firstName"].asString().orEmpty(),
        lastName = record["lastName"].asString().orEmpty(),
        heightInches = record["heightInches"].asNumber()?.toInt(),
        weightGoalLb = goal,
        bpMovingAverageReadings = window,
        weightGoalStartId = record["weightGoalStartId"].asTrimmed(),
        bpTargetSystolic = record["bpTargetSystolic"].asWhole()?.takeIf { it > 0 },
        bpTargetDiastolic = record["bpTargetDiastolic"].asWhole()?.takeIf { it > 0 },
        bpMorningReminder = record["bpMorningReminder"].asBoolean(),
        bpEveningReminder = record["bpEveningReminder"].asBoolean(),
        bpMorningReminderTime = record["bpMorningReminderTime"].asString()?.takeIf { it.isNotBlank() } ?: "07:00",
        bpEveningReminderTime = record["bpEveningReminderTime"].asString()?.takeIf { it.isNotBlank() } ?: "19:00",
        weightMorningReminder = record["weightMorningReminder"].asBoolean(),
        weightMorningReminderTime = record["weightMorningReminderTime"].asString()?.takeIf { it.isNotBlank() } ?: "07:00",
        weightDisplayKg = record["weightDisplayKg"].asBoolean(),
        waistInches = record["waistInches"].asNumber()?.takeIf { it >= MIN_WAIST_IN && it <= MAX_WAIST_IN }?.let(::roundToTenth),
    )
}

private fun normalizeStringList(value: JsonElement?): List<String> {
    val array = value as? JsonArray ?: return emptyList()
    return array.mapNotNull { it.asString()?.trim()?.takeIf(String::isNotEmpty) }
}

private fun normalizeBloodPressureSamples(value: JsonElement?): List<BloodPressureSample> {
    val array = value as? JsonArray ?: return emptyList()
    return array.mapNotNull { element ->
        val record = element as? JsonObject ?: return@mapNotNull null
        val systolic = record["systolic"].asNumber() ?: return@mapNotNull null
        val diastolic = record["diastolic"].asNumber() ?: return@mapNotNull null
        val heartRateElement = record["heartRate"]
        val heartRate = when {
            heartRateElement == null || heartRateElement is JsonNull -> null
            else -> {
                val parsed = heartRateElement.asNumber() ?: return@mapNotNull null
                if (parsed != Math.floor(parsed) || parsed < 1) return@mapNotNull null
                parsed.toInt()
            }
        }
        BloodPressureSample(systolic.toInt(), diastolic.toInt(), heartRate)
    }
}

// ---- strict checks (mirrors assertReadable) ----

private fun assertReadable(payload: JsonElement, data: FitnessStore) {
    val raw = payload as? JsonObject ?: throw AccountExportException("This file is not a Life Metric data export.")
    assertArrayLength(raw["plans"], data.plans.size, "workout plan")
    assertArrayLength(raw["workouts"], data.workouts.size, "workout")
    assertArrayLength(raw["weightRecords"], data.weightRecords.size, "weight reading")
    assertArrayLength(raw["bloodPressureReadings"], data.bloodPressureReadings.size, "blood pressure reading")
    if (!isProfile(raw["profile"])) throw AccountExportException("The profile in this file could not be read.")

    assertUnique(data.plans.map { it.id }, "workout plans")
    assertUnique(data.plans.flatMap { plan -> plan.machines.map { it.id } }, "activities")
    assertUnique(data.workouts.map { it.id }, "workouts")
    assertUnique(data.workouts.flatMap { workout -> workout.entries.map { it.id } }, "workout entries")
    assertUnique(data.weightRecords.map { it.id }, "weight readings")
    assertUnique(data.bloodPressureReadings.map { it.id }, "blood pressure readings")

    val plansById = data.plans.associateBy { it.id }
    data.workouts.forEach { workout ->
        val plan = plansById[workout.planId]
            ?: throw AccountExportException("A workout in this file points at a plan that is not in the file.")
        val machines = plan.machines.map { it.id }.toSet()
        if (workout.entries.any { it.machineId !in machines }) {
            throw AccountExportException("A workout in this file points at an activity that is not on its plan.")
        }
    }
    if (data.weightRecords.any { it.weight < 0 || !UUID_PATTERN.matches(it.id) }) {
        throw AccountExportException("A weight reading in this file could not be read.")
    }
    if (data.bloodPressureReadings.any { it.systolic < 1 || it.diastolic < 1 }) {
        throw AccountExportException("A blood pressure reading in this file could not be read.")
    }
}

private fun assertArrayLength(value: JsonElement?, kept: Int, label: String) {
    val array = value as? JsonArray ?: throw AccountExportException("This file is not a Life Metric data export.")
    if (array.size != kept) throw AccountExportException("A $label in this file could not be read.")
}

private fun assertUnique(ids: List<String>, label: String) {
    if (ids.toSet().size != ids.size) throw AccountExportException("This file has duplicate $label.")
}

// ---- JsonElement helpers ----

private fun JsonElement?.isPresent(): Boolean = this != null && this !is JsonNull

private fun JsonElement?.asString(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    return if (primitive.isString) primitive.content else null
}

private fun JsonElement?.asTrimmed(): String? = asString()?.trim()?.takeIf { it.isNotEmpty() }

/** Numeric JSON value only (matches `typeof === 'number'`). */
private fun JsonElement?.asNumber(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive.isString) return null
    return primitive.doubleOrNull?.takeIf { it.isFinite() }
}

/** Number, or a numeric string (matches `Number(value)` on form drafts). */
private fun JsonElement?.asDouble(): Double? {
    val primitive = this as? JsonPrimitive ?: return null
    val text = primitive.contentOrNull?.trim() ?: return null
    if (text.isEmpty()) return null
    return text.toDoubleOrNull()?.takeIf { it.isFinite() }
}

private fun JsonElement?.asWhole(): Int? = asDouble()?.takeIf { it == Math.floor(it) }?.toInt()

private fun JsonElement?.asBoolean(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    return primitive.booleanOrNull
        ?: primitive.contentOrNull?.equals("true", ignoreCase = true)
        ?: false
}

/** Missing or invalid optional telemetry defaults to 0 so older export files still load. */
private fun JsonElement?.asNonNegativeDouble(): Double = asDouble()?.takeIf { it >= 0 } ?: 0.0

private fun JsonElement?.asNonNegativeInt(): Int = asDouble()?.takeIf { it >= 0 }?.toInt() ?: 0
