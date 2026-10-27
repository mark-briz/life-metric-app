package com.markbriz.lifemetric.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.EntryMetric
import com.markbriz.lifemetric.data.EntryMetricKind
import com.markbriz.lifemetric.data.EntryMetricPlacement
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.newId
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatDurationClock
import com.markbriz.lifemetric.domain.formatLoadWeight
import com.markbriz.lifemetric.domain.formatPreviousLoadSuggestion
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.lastUsedMachineId
import com.markbriz.lifemetric.domain.metricDisplay
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.summarizeWorkoutEntries
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.withParsedMetrics
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.fieldShape
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun WorkoutsScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPlans: () -> Unit,
    onCreatePlan: () -> Unit,
) {
    val groups = store.workouts.sortedWith(compareByDescending<Workout> { it.date }.thenBy { it.id })
        .groupBy { it.date }
    var pending by remember { mutableStateOf<Workout?>(null) }
    val hasPlans = store.plans.isNotEmpty()
    AppScreen(
        eyebrow = "Workout log",
        title = "Workouts",
        lead = "Log strength, cardio, and other sessions.",
        showFab = hasPlans,
        fabLabel = "Log workout",
        onFab = onAdd,
    ) {
        SettingsRow(
            "Manage plans",
            "Create and edit fitness plans and their activities.",
            onPlans,
        )
        if (!hasPlans) {
            EmptyState(
                "Create a plan first",
                "Workout sessions are logged against a plan. Add one, then log sets against it.",
                "Create a plan",
                onCreatePlan,
            )
        } else if (groups.isEmpty()) {
            EmptyState("No workouts yet", "Pick a plan, then add sets, segments, or blocks.", "Log workout", onAdd)
        } else {
            groups.forEach { (date, workouts) ->
                SectionTitle(formatDate(date))
                workouts.forEach { workout ->
                    val plan = store.plans.find { it.id == workout.planId }
                    val label = "${ActivityCatalog.workoutLabel(workout, plan)} · ${plan?.name ?: "Unknown plan"}"
                    HistoryRow(
                        primary = label,
                        secondary = summarizeWorkoutEntries(workout.entries),
                        onClick = { onOpen(workout.id) },
                        overflowLabel = "Delete",
                        onOverflow = { pending = workout },
                    )
                }
            }
        }
    }
    pending?.let { workout ->
        val planName = store.plans.find { it.id == workout.planId }?.name ?: "this plan"
        ConfirmDeleteDialog(
            title = "Delete this workout?",
            body = "Delete $planName on ${formatDate(workout.date)} and all ${workout.entries.size} ${ActivityCatalog.entryNoun(ActivityCatalog.schema(workout.activityTypeId), workout.entries.size)}?",
            onConfirm = { onDelete(workout.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
fun WorkoutFormScreen(
    store: FitnessStore,
    workout: Workout?,
    missing: Boolean,
    isNew: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit = onBack,
    onDate: (String) -> Unit,
    onPlan: (String) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onPlans: () -> Unit,
    onCreatePlan: () -> Unit,
    onAddActivity: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    if (store.plans.isEmpty()) {
        AppScreen(
            title = "Create a plan first",
            lead = "Workout sessions are logged against a fitness plan.",
            onBack = onBack,
            backLabel = "Workouts",
        ) {
            PrimaryButton("Create a plan", onClick = onCreatePlan)
        }
        return
    }
    if (missing || workout == null) {
        AppScreen(title = "Workout not found", lead = "That workout may have been deleted.", onBack = onBack, backLabel = "Workouts") {}
        return
    }
    var pendingEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var pendingPlanId by remember { mutableStateOf<String?>(null) }
    val plan = store.plans.find { it.id == workout.planId }
    val schema = plan?.let { ActivityCatalog.schema(it.activityTypeId) } ?: "load"
    val addLabel = ActivityCatalog.addEntryLabel(schema)
    BackHandler(onBack = onBack)
    AppScreen(
        title = if (isNew) "Log workout" else "Edit workout",
        lead = "Pick a date and plan, then add sets.",
        onBack = onBack,
        backLabel = "Workouts",
    ) {
        SurfaceCard {
            DateTimeFields(workout.date.ifBlank { todayIso() }, "", onDate, {}, showTime = false)
            PlanMenu(store.plans, workout.planId) { planId ->
                if (planId != workout.planId && workout.entries.isNotEmpty()) {
                    pendingPlanId = planId
                } else {
                    onPlan(planId)
                }
            }
            TextLink("Manage plans", onClick = onPlans)
            if (plan != null && plan.machines.isEmpty()) {
                EmptyState(
                    "This plan has no activities",
                    "Add an activity such as Chest Press, Easy run, or a 20-minute stretch.",
                    "Add an activity",
                    { onAddActivity(plan.id) },
                )
            } else {
                PrimaryButton(addLabel, onClick = {
                    if (workout.planId.isBlank()) return@PrimaryButton
                    onAddEntry()
                })
            }
        }
        if (plan == null || plan.machines.isNotEmpty()) {
            if (workout.entries.isEmpty()) {
                EmptyState(
                    "No ${ActivityCatalog.entryNoun(schema, 2)} yet",
                    "Add a ${ActivityCatalog.entryNoun(schema, 1)} to start logging this workout.",
                    addLabel,
                    onAddEntry,
                )
            } else {
                workout.entries.groupBy { it.machineId }.forEach { (machineId, sets) ->
                    val name = plan?.machines?.find { it.id == machineId }?.name ?: "Unknown activity"
                    SurfaceCard {
                        Text(name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        sets.forEachIndexed { index, entry ->
                            if (index > 0) HorizontalDivider(color = colors.border)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (sets.size > 1) {
                                        Text("Set ${index + 1}", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(formatWorkoutEntrySummary(entry), color = colors.inkMuted, fontSize = 13.sp)
                                }
                                IconButton(onClick = { onEditEntry(entry) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit $name ${ActivityCatalog.entryNoun(schema, 1)} ${index + 1}", tint = colors.brandDark)
                                }
                                IconButton(onClick = { pendingEntry = entry }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove $name ${ActivityCatalog.entryNoun(schema, 1)} ${index + 1}", tint = colors.danger)
                                }
                            }
                        }
                    }
                }
            }
        }
        PrimaryButton("Done", onClick = onDone)
    }
    pendingPlanId?.let { planId ->
        val next = store.plans.find { it.id == planId }
        val count = workout.entries.size
        ConfirmDeleteDialog(
            title = "Switch plans?",
            body = "Switch to ${next?.name ?: "this plan"}? This removes $count ${ActivityCatalog.entryNoun(schema, count)} from this session.",
            confirmLabel = "Switch plan",
            onConfirm = { onPlan(planId); pendingPlanId = null },
            onDismiss = { pendingPlanId = null },
        )
    }
    pendingEntry?.let { entry ->
        val noun = ActivityCatalog.entryNoun(schema, 1)
        val name = plan?.machines?.find { it.id == entry.machineId }?.name ?: "this $noun"
        ConfirmDeleteDialog(
            title = "Remove this $noun?",
            body = "Remove $name from this workout?",
            confirmLabel = "Remove",
            onConfirm = { onRemoveEntry(entry.id); pendingEntry = null },
            onDismiss = { pendingEntry = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEntryFormScreen(
    plan: FitnessPlan?,
    existing: WorkoutEntry?,
    missing: Boolean = false,
    plans: List<FitnessPlan> = emptyList(),
    workouts: List<Workout> = emptyList(),
    currentWorkoutId: String? = null,
    currentWorkoutDate: String = todayIso(),
    currentEntries: List<WorkoutEntry> = emptyList(),
    onBack: () -> Unit,
    onSave: (WorkoutEntry) -> Unit,
) {
    val colors = LocalAppColors.current
    if (plan == null) {
        AppScreen(title = "Workout plan required", lead = "Select a plan before adding a set.", onBack = onBack, backLabel = "Workout") {}
        return
    }
    if (missing) {
        val noun = ActivityCatalog.entryNoun(ActivityCatalog.schema(plan.activityTypeId), 1)
        AppScreen(
            title = "${noun.replaceFirstChar { it.uppercase() }} not found",
            lead = "That $noun may have been deleted.",
            onBack = onBack,
            backLabel = "Workout",
        ) {}
        return
    }
    val schema = ActivityCatalog.schema(plan.activityTypeId)
    val isNew = existing == null
    val tabataDefaults = isNew && plan.activityTypeId == "tabata"
    val entryId = remember(existing?.id) { existing?.id ?: newId("entry") }
    val allPlans = plans.ifEmpty { listOf(plan) }
    val defaultMachineId = lastUsedMachineId(workouts, plan.id, plan.machines).orEmpty()
    var machineId by remember(existing?.id, plan.id) {
        mutableStateOf(existing?.machineId?.takeIf { it.isNotBlank() } ?: defaultMachineId)
    }
    fun previousLoad(forMachineId: String) = findPreviousLoad(
        workouts = workouts,
        plans = allPlans,
        activityId = forMachineId,
        currentWorkoutDate = currentWorkoutDate,
        currentEntries = currentEntries,
        currentWorkoutId = currentWorkoutId,
        excludeEntryId = existing?.id,
    )
    val initialLoad = if (isNew && schema == "load") previousLoad(machineId) else null
    var weight by remember(existing?.id) {
        mutableStateOf(existing?.weight?.let(::formatLoadWeight) ?: initialLoad?.weight?.let(::formatLoadWeight).orEmpty())
    }
    var reps by remember(existing?.id) {
        mutableStateOf(existing?.repetitions?.toString() ?: initialLoad?.repetitions?.toString().orEmpty())
    }
    var distance by remember(existing?.id) {
        mutableStateOf(existing?.distanceMi?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var duration by remember(existing?.id) {
        mutableStateOf(
            when (schema) {
                "distance_time" -> existing?.durationSec?.takeIf { it > 0 }?.let { formatDurationClock(it) }.orEmpty()
                else -> existing?.durationSec?.takeIf { it > 0 }?.let { (it / 60.0).let { m -> if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } }.orEmpty()
            },
        )
    }
    var rounds by remember(existing?.id, plan.activityTypeId) {
        mutableStateOf(existing?.rounds?.toString() ?: if (tabataDefaults) "8" else "")
    }
    var work by remember(existing?.id, plan.activityTypeId) {
        mutableStateOf(existing?.workSec?.toString() ?: if (tabataDefaults) "20" else "")
    }
    var rest by remember(existing?.id, plan.activityTypeId) {
        mutableStateOf(existing?.restSec?.toString() ?: if (tabataDefaults) "10" else "")
    }
    var intensity by remember(existing?.id) { mutableStateOf(existing?.intensity.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    val extraMetrics = ActivityCatalog.entryMetrics(plan.activityTypeId)
    var metricDrafts by remember(existing?.id, plan.activityTypeId) {
        mutableStateOf(
            extraMetrics.associate { metric ->
                metric.id to existing?.metricDisplay(metric).orEmpty().let { if (it == "0" || it == "0.0") "" else it }
            },
        )
    }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var dirty by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun markDirty() {
        dirty = true
    }

    fun builtEntry(
        nextMachineId: String = machineId,
        nextWeight: String = weight,
        nextReps: String = reps,
        nextDistance: String = distance,
        nextDuration: String = duration,
        nextRounds: String = rounds,
        nextWork: String = work,
        nextRest: String = rest,
        nextIntensity: String = intensity,
        nextNote: String = note,
        nextMetrics: Map<String, String> = metricDrafts,
    ): Pair<WorkoutEntry?, Map<String, String>> {
        val next = mutableMapOf<String, String>()
        if (nextMachineId.isBlank() || plan.machines.none { it.id == nextMachineId }) {
            next["machineId"] = "Select an activity."
        }
        val built = (existing ?: WorkoutEntry(id = entryId, planId = plan.id, machineId = nextMachineId, schema = schema))
            .copy(planId = plan.id, machineId = nextMachineId, schema = schema)
        val entry = when (schema) {
            "load" -> {
                val w = nextWeight.toDoubleOrNull()
                val r = nextReps.toIntOrNull()
                if (nextWeight.isNotBlank() && (w == null || w < 0)) next["weight"] = "Weight must be 0 or greater."
                if (nextReps.isNotBlank() && (r == null || r < 0)) next["repetitions"] = "Repetitions must be 0 or greater."
                built.copy(weight = w?.takeIf { it >= 0 } ?: 0.0, repetitions = r?.takeIf { it >= 0 } ?: 0)
            }
            "distance_time" -> {
                val miles = nextDistance.toDoubleOrNull()
                val sec = if (nextDuration.isBlank()) 0 else parseDurationInput(nextDuration)
                if (nextDistance.isNotBlank() && (miles == null || miles < 0)) next["distanceMi"] = "Distance must be 0 or greater."
                if (nextDuration.isNotBlank() && sec == null) next["durationSec"] = "Enter time as mm:ss or minutes."
                built.copy(distanceMi = miles?.takeIf { it >= 0 } ?: 0.0, durationSec = sec ?: 0)
            }
            "duration", "sport" -> {
                val sec = if (nextDuration.isBlank()) 0 else parseDurationInput(nextDuration)
                if (nextDuration.isNotBlank() && sec == null) next["durationSec"] = "Enter minutes greater than 0."
                built.copy(durationSec = sec ?: 0, intensity = nextIntensity.ifBlank { null })
            }
            "interval" -> {
                val roundCount = nextRounds.toIntOrNull()
                if (nextRounds.isNotBlank() && (roundCount == null || roundCount < 0)) next["rounds"] = "Enter a whole number of rounds."
                val workSec = nextWork.toIntOrNull()
                val restSec = nextRest.toIntOrNull()
                if (nextWork.isNotBlank() && (workSec == null || workSec < 1)) next["workSec"] = "Enter whole-number seconds."
                if (nextRest.isNotBlank() && (restSec == null || restSec < 1)) next["restSec"] = "Enter whole-number seconds."
                built.copy(rounds = roundCount?.takeIf { it >= 0 } ?: 0, workSec = workSec, restSec = restSec)
            }
            else -> built.copy(note = nextNote.trim())
        }
        val (entryWithMetrics, metricErrors) = entry.withParsedMetrics(extraMetrics, nextMetrics)
        next.putAll(metricErrors)
        return if (next.isEmpty()) entryWithMetrics to emptyMap() else null to next
    }

    fun requestBack() {
        if (dirty) confirmDiscard = true else onBack()
    }

    @Composable
    fun metricField(metric: EntryMetric) {
        LabeledField(
            metric.label,
            metricDrafts[metric.id].orEmpty(),
            { text ->
                val nextValue = if (metric.kind == EntryMetricKind.Whole) text.filter(Char::isDigit) else text
                metricDrafts = metricDrafts + (metric.id to nextValue)
                markDirty()
            },
            error = errors[metric.id],
            hint = metric.hint,
            keyboardType = if (metric.kind == EntryMetricKind.Whole) KeyboardType.Number else KeyboardType.Decimal,
        )
    }

    val previous = if (schema == "load" && machineId.isNotBlank()) previousLoad(machineId) else null
    BackHandler(onBack = { requestBack() })
    AppScreen(
        title = if (isNew) ActivityCatalog.addEntryLabel(schema) else ActivityCatalog.editEntryLabel(schema),
        lead = ActivityCatalog.label(plan.activityTypeId),
        onBack = { requestBack() },
        backLabel = "Workout",
    ) {
        SurfaceCard {
            MachineMenu(plan, machineId) { nextId ->
                machineId = nextId
                if (isNew && schema == "load") {
                    val load = previousLoad(nextId)
                    weight = load?.weight?.let(::formatLoadWeight).orEmpty()
                    reps = load?.repetitions?.toString().orEmpty()
                }
                markDirty()
            }
            if (errors["machineId"] != null) Text(errors["machineId"]!!, color = colors.danger, fontSize = 12.sp)
            if (previous != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatPreviousLoadSuggestion(previous),
                        color = colors.inkMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            weight = formatLoadWeight(previous.weight)
                            reps = previous.repetitions.toString()
                            markDirty()
                        },
                    ) {
                        Text("Use")
                    }
                }
            }
            extraMetrics.filter { it.placement == EntryMetricPlacement.Before }.forEach { metricField(it) }
            when (schema) {
                "load" -> {
                    LabeledField("Weight", weight, { weight = it; markDirty() }, error = errors["weight"], keyboardType = KeyboardType.Decimal)
                    LabeledField("Repetitions", reps, { reps = it.filter(Char::isDigit); markDirty() }, error = errors["repetitions"], keyboardType = KeyboardType.Number)
                }
                "distance_time" -> {
                    LabeledField("Distance (mi)", distance, { distance = it; markDirty() }, error = errors["distanceMi"], keyboardType = KeyboardType.Decimal)
                    LabeledField("Time", duration, { duration = it; markDirty() }, error = errors["durationSec"], hint = "mm:ss", keyboardType = KeyboardType.Text)
                }
                "duration", "sport" -> {
                    LabeledField("Minutes", duration, { duration = it; markDirty() }, error = errors["durationSec"], keyboardType = KeyboardType.Decimal)
                    if (schema == "sport") {
                        IntensityMenu(intensity) { intensity = it; markDirty() }
                    }
                }
                "interval" -> {
                    if (tabataDefaults) {
                        Text("Tabata defaults: 8 rounds, 20s work, 10s rest.", color = colors.inkMuted, fontSize = 13.sp)
                    }
                    LabeledField("Rounds", rounds, { rounds = it.filter(Char::isDigit); markDirty() }, error = errors["rounds"], keyboardType = KeyboardType.Number)
                    LabeledField("Work (sec)", work, { work = it.filter(Char::isDigit); markDirty() }, error = errors["workSec"], keyboardType = KeyboardType.Number)
                    LabeledField("Rest (sec)", rest, { rest = it.filter(Char::isDigit); markDirty() }, error = errors["restSec"], keyboardType = KeyboardType.Number)
                }
                else -> LabeledField("Note (optional)", note, { note = it; markDirty() }, singleLine = false, minLines = 3)
            }
            extraMetrics.filter { it.placement == EntryMetricPlacement.After }.forEach { metricField(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Cancel", { requestBack() }, Modifier.weight(1f))
                PrimaryButton("Save", modifier = Modifier.weight(1f), onClick = {
                    val (entry, nextErrors) = builtEntry()
                    errors = nextErrors
                    if (entry != null) onSave(entry)
                })
            }
        }
        if (extraMetrics.isNotEmpty()) Spacer(Modifier.height(140.dp))
    }
    if (confirmDiscard) {
        ConfirmDeleteDialog(
            title = "Discard changes?",
            body = if (isNew) "This set will not be added." else "Your edits will not be saved.",
            confirmLabel = "Discard",
            onConfirm = { confirmDiscard = false; onBack() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanMenu(plans: List<FitnessPlan>, selectedId: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = plans.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Workout plan",
            onValueChange = {},
            readOnly = true,
            label = { Text("Workout plan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text("${plan.name} · ${ActivityCatalog.planLabel(plan)}") },
                    onClick = { onSelect(plan.id); open = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MachineMenu(plan: FitnessPlan, selectedId: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = plan.machines.find { it.id == selectedId }
    val label = ActivityCatalog.segmentFieldLabel(ActivityCatalog.schema(plan.activityTypeId))
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            plan.machines.forEach { machine ->
                DropdownMenuItem(text = { Text(machine.name) }, onClick = { onSelect(machine.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntensityMenu(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf("easy", "moderate", "hard")
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected.ifBlank { "Optional" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Intensity") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}
