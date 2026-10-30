package com.markbriz.lifemetric.data

data class ActivityFamily(val id: String, val label: String, val chip: String)

data class ActivityType(
    val id: String,
    val familyId: String,
    val label: String,
    val schema: String,
)

enum class EntryMetricKind { Decimal, Whole }

enum class EntryMetricPlacement { Before, After }

/**
 * Optional telemetry a workout entry can record. Attach a subset to an activity type
 * so the same fields can be reused (for example speed + heart rate on running later).
 */
data class EntryMetric(
    val id: String,
    val label: String,
    val hint: String? = null,
    val kind: EntryMetricKind,
    val placement: EntryMetricPlacement = EntryMetricPlacement.After,
)

object EntryMetrics {
    const val RESISTANCE = "resistance"
    const val AVG_SPEED = "avgSpeedMph"
    const val AVG_RPM = "avgRpm"
    const val AVG_HEART_RATE = "avgHeartRateBpm"
    const val MAX_HEART_RATE = "maxHeartRateBpm"

    val resistance = EntryMetric(RESISTANCE, "Resistance", kind = EntryMetricKind.Decimal, placement = EntryMetricPlacement.Before)
    val avgSpeed = EntryMetric(AVG_SPEED, "Average speed (mph)", kind = EntryMetricKind.Decimal)
    val avgRpm = EntryMetric(AVG_RPM, "Average RPM", kind = EntryMetricKind.Whole)
    val avgHeartRate = EntryMetric(AVG_HEART_RATE, "Average heart rate (bpm)", kind = EntryMetricKind.Whole)
    val maxHeartRate = EntryMetric(MAX_HEART_RATE, "Max BPM", kind = EntryMetricKind.Whole)

    val all = listOf(resistance, avgSpeed, avgRpm, avgHeartRate, maxHeartRate)
}

object ActivityCatalog {
    val families = listOf(
        ActivityFamily("strength_resistance", "Strength & Resistance", "Strength"),
        ActivityFamily("cardio_endurance", "Cardio & Endurance", "Cardio"),
        ActivityFamily("flexibility_mobility", "Flexibility & Mobility", "Mobility"),
        ActivityFamily("high_intensity", "High-Intensity & Conditioning", "Conditioning"),
        ActivityFamily("sports_recreational", "Sports & Recreational", "Sports"),
        ActivityFamily("health_recovery", "Health & Recovery", "Recovery"),
    )

    val types = listOf(
        ActivityType("strength_training", "strength_resistance", "Strength Training", "load"),
        ActivityType("powerlifting", "strength_resistance", "Powerlifting", "load"),
        ActivityType("bodyweight", "strength_resistance", "Bodyweight / Calisthenics", "load"),
        ActivityType("functional", "strength_resistance", "Functional Training", "load"),
        ActivityType("kettlebell", "strength_resistance", "Kettlebell Training", "load"),
        ActivityType("olympic_weightlifting", "strength_resistance", "Olympic Weightlifting", "load"),
        ActivityType("running", "cardio_endurance", "Running", "distance_time"),
        ActivityType("walking", "cardio_endurance", "Walking", "distance_time"),
        ActivityType("cycling", "cardio_endurance", "Cycling", "distance_time"),
        ActivityType("swimming", "cardio_endurance", "Swimming", "distance_time"),
        ActivityType("rowing", "cardio_endurance", "Rowing", "distance_time"),
        ActivityType("elliptical", "cardio_endurance", "Elliptical", "distance_time"),
        ActivityType("stair_climbing", "cardio_endurance", "Stair Climbing", "distance_time"),
        ActivityType("hiking", "cardio_endurance", "Hiking", "distance_time"),
        ActivityType("jump_rope", "cardio_endurance", "Jump Rope", "duration"),
        ActivityType("stretching", "flexibility_mobility", "Stretching", "duration"),
        ActivityType("yoga", "flexibility_mobility", "Yoga", "duration"),
        ActivityType("pilates", "flexibility_mobility", "Pilates", "duration"),
        ActivityType("mobility_training", "flexibility_mobility", "Mobility Training", "duration"),
        ActivityType("foam_rolling", "flexibility_mobility", "Foam Rolling / Recovery", "duration"),
        ActivityType("hiit", "high_intensity", "HIIT", "interval"),
        ActivityType("circuit_training", "high_intensity", "Circuit Training", "interval"),
        ActivityType("crossfit", "high_intensity", "CrossFit", "interval"),
        ActivityType("boot_camp", "high_intensity", "Boot Camp", "interval"),
        ActivityType("metcon", "high_intensity", "Metabolic Conditioning", "interval"),
        ActivityType("tabata", "high_intensity", "Tabata", "interval"),
        ActivityType("basketball", "sports_recreational", "Basketball", "sport"),
        ActivityType("soccer", "sports_recreational", "Soccer", "sport"),
        ActivityType("tennis_pickleball", "sports_recreational", "Tennis / Pickleball", "sport"),
        ActivityType("golf", "sports_recreational", "Golf", "sport"),
        ActivityType("volleyball", "sports_recreational", "Volleyball", "sport"),
        ActivityType("martial_arts", "sports_recreational", "Martial Arts", "sport"),
        ActivityType("dance", "sports_recreational", "Dance", "sport"),
        ActivityType("other_sports", "sports_recreational", "Other Sports", "sport"),
        ActivityType("recovery_workout", "health_recovery", "Recovery Workout", "duration"),
        ActivityType("low_intensity", "health_recovery", "Low-Intensity Activity", "duration"),
        ActivityType("breathing_meditation", "health_recovery", "Breathing / Meditation", "duration"),
        ActivityType("physical_therapy", "health_recovery", "Physical Therapy / Rehabilitation", "duration"),
        ActivityType("rest_day", "health_recovery", "Rest Day", "note"),
    )

    private val byId = types.associateBy { it.id }

    private val typeToCategory = mapOf(
        "strength_training" to "strength",
        "powerlifting" to "strength",
        "bodyweight" to "strength",
        "functional" to "strength",
        "kettlebell" to "strength",
        "olympic_weightlifting" to "strength",
        "running" to "cardio",
        "walking" to "cardio",
        "cycling" to "cardio",
        "swimming" to "cardio",
        "rowing" to "cardio",
        "elliptical" to "cardio",
        "stair_climbing" to "cardio",
        "hiking" to "outdoor",
        "jump_rope" to "cardio",
        "stretching" to "mobility_flexibility",
        "yoga" to "mobility_flexibility",
        "pilates" to "mobility_flexibility",
        "mobility_training" to "mobility_flexibility",
        "foam_rolling" to "mobility_flexibility",
        "hiit" to "conditioning",
        "circuit_training" to "conditioning",
        "crossfit" to "conditioning",
        "boot_camp" to "conditioning",
        "metcon" to "conditioning",
        "tabata" to "conditioning",
        "basketball" to "sports",
        "soccer" to "sports",
        "tennis_pickleball" to "sports",
        "golf" to "sports",
        "volleyball" to "sports",
        "martial_arts" to "sports",
        "dance" to "sports",
        "other_sports" to "sports",
        "recovery_workout" to "recovery",
        "low_intensity" to "recovery",
        "breathing_meditation" to "recovery",
        "physical_therapy" to "recovery",
        "rest_day" to "recovery",
    )

    const val DEFAULT_TYPE = "strength_training"

    fun get(id: String): ActivityType = byId[id] ?: byId.getValue(DEFAULT_TYPE)

    fun label(id: String): String = get(id).label

    fun schema(id: String): String = get(id).schema

    /**
     * Schema to show and save for one set. An existing set keeps the schema it was logged
     * with, so changing the plan's activity type later cannot rewrite that set into empty
     * fields for the new type.
     */
    fun entrySchema(existing: WorkoutEntry?, activityTypeId: String): String {
        val stored = existing?.schema?.takeIf { candidate -> types.any { it.schema == candidate } }
        return stored ?: schema(activityTypeId)
    }

    fun category(id: String): String = typeToCategory[id] ?: "strength"

    fun typesIn(familyId: String): List<ActivityType> = types.filter { it.familyId == familyId }

    fun familyOf(typeId: String): String = get(typeId).familyId

    fun planLabel(plan: FitnessPlan?): String = if (plan == null) "Unknown type" else label(plan.activityTypeId)

    fun workoutLabel(workout: Workout, plan: FitnessPlan?): String {
        val id = if (byId.containsKey(workout.activityTypeId)) workout.activityTypeId else plan?.activityTypeId ?: DEFAULT_TYPE
        return label(id)
    }

    fun segmentFieldLabel(schema: String): String = if (schema == "distance_time") "Segment" else "Activity"

    /**
     * Extra entry fields for this activity. Cycling uses speed, cadence, and heart rate;
     * add the same [EntryMetric]s to other type ids when those workouts need them.
     */
    fun entryMetrics(activityTypeId: String): List<EntryMetric> = when (activityTypeId) {
        "cycling" -> listOf(
            EntryMetrics.resistance,
            EntryMetrics.avgSpeed,
            EntryMetrics.avgRpm,
            EntryMetrics.avgHeartRate,
            EntryMetrics.maxHeartRate,
        )
        else -> emptyList()
    }

    fun addEntryLabel(schema: String): String = when (schema) {
        "load" -> "Add set"
        "distance_time" -> "Add segment"
        "interval" -> "Add block"
        "note" -> "Add note"
        else -> "Add block"
    }

    fun editEntryLabel(schema: String): String = when (schema) {
        "load" -> "Edit set"
        "distance_time" -> "Edit segment"
        "interval" -> "Edit block"
        "note" -> "Edit note"
        else -> "Edit block"
    }

    fun entryNoun(schema: String, count: Int): String = when (schema) {
        "load" -> if (count == 1) "set" else "sets"
        "distance_time" -> if (count == 1) "segment" else "segments"
        "interval" -> if (count == 1) "block" else "blocks"
        "note" -> if (count == 1) "note" else "notes"
        else -> if (count == 1) "block" else "blocks"
    }

    fun intervalDefaults(typeId: String): Triple<Int, Int?, Int?> =
        if (typeId == "tabata") Triple(8, 20, 10) else Triple(4, null, null)

    fun starterPlans(): List<FitnessPlan> {
        fun machine(name: String, description: String) = Machine(newId("machine"), name, description)
        return listOf(
            FitnessPlan(
                id = newId("plan"),
                name = "Upper Body Strength",
                description = "Chest, back, and shoulders — push and pull focused.",
                activityTypeId = "strength_training",
                category = "strength",
                machines = listOf(
                    machine("Tricepts Extension", "Extension of the tricepts muscle."),
                    machine("Bicepts Curl", "Curl of the bicepts muscle."),
                    machine("Chest Press", "Seated machine press; full range without locking elbows."),
                    machine("Lat Pulldown", "Wide grip to the upper chest."),
                    machine("Rear Deltoid", "Rear deltoid muscle."),
                    machine("Fixed Pulldown", "Fixed pulldown machine."),
                    machine("Shoulder Press", "Seated overhead press with a controlled tempo."),
                    machine("Seated Row", "Neutral grip; squeeze scapulae at the end of each rep."),
                    machine("Pectoral Fly Deltoid", "Pectoral fly deltoid muscle."),
                ),
            ),
            FitnessPlan(
                id = newId("plan"),
                name = "Lower Body Focus",
                description = "Legs and posterior chain for strength and stability.",
                activityTypeId = "strength_training",
                category = "strength",
                machines = listOf(
                    machine("Leg Press", "Feet mid-platform; stop short of locking knees."),
                    machine("Leg Curl", "Hamstring curl — keep hips pinned to the pad."),
                    machine("Calf Raise", "Full stretch at the bottom, pause at the top."),
                ),
            ),
        )
    }
}
