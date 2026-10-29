package com.markbriz.lifemetric.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.markbriz.lifemetric.domain.SessionActivity
import com.markbriz.lifemetric.domain.WorkoutHistoryRange
import com.markbriz.lifemetric.domain.WorkoutSetKind
import com.markbriz.lifemetric.domain.WorkoutWeekSummary
import com.markbriz.lifemetric.domain.activityImproved
import com.markbriz.lifemetric.domain.activityPersonalRecord
import com.markbriz.lifemetric.domain.buildWorkoutCsv
import com.markbriz.lifemetric.domain.buildWorkoutReport
import com.markbriz.lifemetric.domain.currentGroupLoad
import com.markbriz.lifemetric.domain.draftHasDetails
import com.markbriz.lifemetric.domain.filterWorkouts
import com.markbriz.lifemetric.domain.findPreviousEntry
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatActivityRecord
import com.markbriz.lifemetric.domain.formatActivityVsLastTime
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatDurationClock
import com.markbriz.lifemetric.domain.formatLiftWeight
import com.markbriz.lifemetric.domain.formatPlanLastDone
import com.markbriz.lifemetric.domain.formatPreviousActivitySuggestion
import com.markbriz.lifemetric.domain.formatPreviousLoadSuggestion
import com.markbriz.lifemetric.domain.formatRelativeDays
import com.markbriz.lifemetric.domain.formatRestClock
import com.markbriz.lifemetric.domain.formatSessionDetailsLine
import com.markbriz.lifemetric.domain.formatSessionDurationInput
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.formatVolumeTrendCaption
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.formatWorkoutVsLastTime
import com.markbriz.lifemetric.domain.formatWorkoutWeekAerobic
import com.markbriz.lifemetric.domain.formatWorkoutWeekCaption
import com.markbriz.lifemetric.domain.formatWorkoutWeekStrength
import com.markbriz.lifemetric.domain.hollowWorkoutEntryError
import com.markbriz.lifemetric.domain.isWarmupSet
import com.markbriz.lifemetric.domain.lastUsedPlanId
import com.markbriz.lifemetric.domain.latestWorkout
import com.markbriz.lifemetric.domain.metricDisplay
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.parseLiftWeightInput
import com.markbriz.lifemetric.domain.sessionActivities
import com.markbriz.lifemetric.domain.sessionPrefillEntry
import com.markbriz.lifemetric.domain.summarizeWorkout
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.usedWorkoutFamilies
import com.markbriz.lifemetric.domain.usedWorkoutPlans
import com.markbriz.lifemetric.domain.validateSessionDetails
import com.markbriz.lifemetric.domain.weightUnit
import com.markbriz.lifemetric.domain.withParsedMetrics
import com.markbriz.lifemetric.domain.workoutSessionsByWeek
import com.markbriz.lifemetric.domain.workoutVsLastImproved
import com.markbriz.lifemetric.domain.workoutWeekSummary
import com.markbriz.lifemetric.domain.workoutsInLastDays
import com.markbriz.lifemetric.ui.components.ActiveFilter
import com.markbriz.lifemetric.ui.components.AddDetails
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ChoiceChip
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.FilterBar
import com.markbriz.lifemetric.ui.components.FilterSection
import com.markbriz.lifemetric.ui.components.FilterSheet
import com.markbriz.lifemetric.ui.components.FormSheet
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HeroHeader
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.LastTimeSuggestion
import com.markbriz.lifemetric.ui.components.MenuAction
import com.markbriz.lifemetric.ui.components.OverflowMenu
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SecondaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.Segment
import com.markbriz.lifemetric.ui.components.SegmentedChoice
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.SheetTitle
import com.markbriz.lifemetric.ui.components.StatChip
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.components.WorkoutBarChart
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.fieldShape
import com.markbriz.lifemetric.ui.profile.shareWorkoutReport
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val HISTORY_PAGE = 20

private enum class WorkoutTrendView { Sessions, Volume }

@Composable
fun WorkoutsScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onStartPlan: (String) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPlans: () -> Unit,
    onCreatePlan: () -> Unit,
    onHowToLog: () -> Unit = {},
    onSettings: () -> Unit = {},
    highlightedId: String? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val useKg = store.profile.weightDisplayKg
    var pending by remember { mutableStateOf<Workout?>(null) }
    var listRange by remember { mutableStateOf(WorkoutHistoryRange.All) }
    var planFilter by remember { mutableStateOf<String?>(null) }
    var familyFilter by remember { mutableStateOf<String?>(null) }
    var visibleCount by remember { mutableIntStateOf(HISTORY_PAGE) }
    var trendView by remember { mutableStateOf(WorkoutTrendView.Sessions) }
    var filterOpen by remember { mutableStateOf(false) }
    val filterFocus = remember { FocusRequester() }
    val hasPlans = store.plans.isNotEmpty()
    val lastWorkout = latestWorkout(store.workouts)
    val lastPlan = store.plans.find { it.id == lastWorkout?.planId }
    val sessions = workoutsInLastDays(store.workouts, 30)
    val sets = sessions.sumOf { it.entries.size }
    val weeks = workoutSessionsByWeek(store.workouts, 8)
    val week = workoutWeekSummary(store.workouts, store.plans)
    val lastVs = lastWorkout?.let { formatWorkoutVsLastTime(it, store.workouts, store.plans, useKg) }
    val filtered = filterWorkouts(store.workouts, store.plans, listRange, planFilter, familyFilter)
    val recent = filtered.take(visibleCount)
    val groups = recent.groupBy { it.date }
    val filterPlans = usedWorkoutPlans(store.workouts, store.plans)
    val filterFamilies = usedWorkoutFamilies(store.workouts, store.plans)
    val setRange: (WorkoutHistoryRange) -> Unit = {
        listRange = it
        visibleCount = HISTORY_PAGE
    }
    val setPlan: (String?) -> Unit = {
        planFilter = it
        visibleCount = HISTORY_PAGE
    }
    val setFamily: (String?) -> Unit = {
        familyFilter = it
        visibleCount = HISTORY_PAGE
    }
    val clearFilters: () -> Unit = {
        listRange = WorkoutHistoryRange.All
        planFilter = null
        familyFilter = null
        visibleCount = HISTORY_PAGE
    }
    val activeFilters = buildList {
        listRange.days?.let { days -> add(ActiveFilter("Last $days days") { setRange(WorkoutHistoryRange.All) }) }
        filterPlans.find { it.id == planFilter }?.let { plan -> add(ActiveFilter(plan.name) { setPlan(null) }) }
        filterFamilies.find { it.id == familyFilter }?.let { family -> add(ActiveFilter(family.chip) { setFamily(null) }) }
    }
    val shareLog: () -> Unit = {
        shareWorkoutReport(
            context,
            buildWorkoutReport(filtered, store.plans, "${formatDate(todayIso())} ${formatTime(nowTime())}"),
            buildWorkoutCsv(filtered, store.plans),
        )
    }
    AppScreen(
        title = "Workouts",
        showFab = true,
        fabLabel = "Log workout",
        onFab = onAdd,
        actions = listOfNotNull(
            MenuAction("Plans") { onPlans() },
            MenuAction("How to log") { onHowToLog() },
            if (store.workouts.isNotEmpty()) MenuAction("Share log") { shareLog() } else null,
            MenuAction("Workout settings") { onSettings() },
        ),
    ) {
        if (!hasPlans) {
            EmptyState(
                "Create a plan first",
                "Workout sessions are logged against a plan. Add one, then log sets against it.",
                "Create a plan",
                onCreatePlan,
            )
            return@AppScreen
        }
        if (lastWorkout == null) {
            EmptyState("No workouts yet", "Pick a plan, then add sets, segments, or time.", "Log workout", onAdd)
            return@AppScreen
        }
        SurfaceCard { WorkoutHero(store, onStartAgain = onStartPlan) }

        SectionTitle("Trend")
        SurfaceCard {
            SegmentedChoice(
                segments = listOf(
                    Segment(WorkoutTrendView.Sessions, "Sessions", "Weekly sessions"),
                    Segment(WorkoutTrendView.Volume, "Volume", "Weekly volume"),
                ),
                selected = trendView,
                onSelect = { trendView = it },
            )
            Text("Last 8 weeks", color = colors.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            when (trendView) {
                WorkoutTrendView.Sessions -> if (weeks.any { it.count > 0 }) {
                    WorkoutBarChart(weeks)
                } else {
                    Text("No sessions in the last 8 weeks.", color = colors.inkMuted)
                }
                WorkoutTrendView.Volume -> if (weeks.any { it.volumeLb > 0 }) {
                    WorkoutBarChart(weeks, title = "Volume (lb)") { it.volumeLb.roundToInt() }
                    Text(formatVolumeTrendCaption(), color = colors.inkFaint, fontSize = 12.sp, lineHeight = 16.sp)
                } else {
                    Text("Volume appears once you log weighted sets.", color = colors.inkMuted)
                }
            }
            Text(
                "Sessions (30d) ${sessions.size} · Sets (30d) $sets",
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        SectionTitle("Goal")
        WorkoutWeekCard(week)

        SectionTitle("History")
        FilterBar(activeFilters, onOpen = { filterOpen = true }, onClearAll = clearFilters, openerFocus = filterFocus)
        if (filtered.isEmpty()) {
            Text("No workouts match these filters.", color = colors.inkMuted)
        }
        groups.forEach { (date, workouts) ->
            Text(formatDate(date), color = colors.inkMuted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            workouts.forEach { workout ->
                val plan = store.plans.find { it.id == workout.planId }
                HistoryRow(
                    primary = plan?.name ?: "Unknown plan",
                    secondary = "${ActivityCatalog.workoutLabel(workout, plan)} · ${summarizeWorkout(workout, useKg)}",
                    onClick = { onOpen(workout.id) },
                    overflowLabel = "Delete",
                    onOverflow = { pending = workout },
                    highlighted = workout.id == highlightedId,
                )
            }
        }
        if (recent.size < filtered.size) {
            TextLink("Show older", onClick = { visibleCount += HISTORY_PAGE })
        }
    }
    if (filterOpen) {
        FilterSheet(
            resultSummary = "Showing ${filtered.size} of ${store.workouts.size} workouts.",
            onDismiss = { filterOpen = false },
            onClearAll = clearFilters,
            returnFocus = filterFocus,
        ) {
            FilterSection("Range") {
                WorkoutHistoryRange.entries.forEach { range ->
                    ChoiceChip(
                        range.days?.let { "Last $it days" } ?: "All",
                        selected = listRange == range,
                        onClick = { setRange(range) },
                    )
                }
            }
            if (filterPlans.isNotEmpty()) {
                FilterSection("Plan") {
                    ChoiceChip("Any", selected = planFilter == null, onClick = { setPlan(null) })
                    filterPlans.forEach { plan ->
                        ChoiceChip(plan.name, selected = planFilter == plan.id, onClick = { setPlan(plan.id) })
                    }
                }
            }
            if (filterFamilies.isNotEmpty()) {
                FilterSection("Activity family") {
                    ChoiceChip("Any", selected = familyFilter == null, onClick = { setFamily(null) })
                    filterFamilies.forEach { family ->
                        ChoiceChip(family.chip, selected = familyFilter == family.id, onClick = { setFamily(family.id) })
                    }
                }
            }
        }
    }
    pending?.let { workout ->
        WorkoutDeleteDialog(store, workout, onConfirm = { onDelete(workout.id); pending = null }, onDismiss = { pending = null })
    }
}

@Composable
private fun WorkoutDeleteDialog(store: FitnessStore, workout: Workout, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val planName = store.plans.find { it.id == workout.planId }?.name ?: "this plan"
    val noun = ActivityCatalog.entryNoun(ActivityCatalog.schema(workout.activityTypeId), workout.entries.size)
    ConfirmDeleteDialog(
        title = "Delete this workout?",
        body = "Delete $planName on ${formatDate(workout.date)} and all ${workout.entries.size} $noun?",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** Picks the plan for a new session, or a replacement plan for an existing one. */
@Composable
fun PlanPickerSheet(
    plans: List<FitnessPlan>,
    workouts: List<Workout>,
    selectedId: String?,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreatePlan: () -> Unit,
    onManagePlans: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    var selected by remember(selectedId) {
        mutableStateOf(selectedId?.takeIf { id -> plans.any { it.id == id } } ?: lastUsedPlanId(workouts, plans))
    }
    val current = selected?.takeIf { id -> plans.any { it.id == id } } ?: lastUsedPlanId(workouts, plans)
    FormSheet(onDismiss = onDismiss) { close, _ ->
        SheetTitle(
            "Choose a plan",
            "A plan is a list of activities you repeat. Each workout logs sets against one plan.",
        )
        if (plans.isEmpty()) {
            EmptyState(
                "Create a plan to start logging",
                "Name it, choose what you log, then add activities such as Chest Press or Easy run.",
                "Create a plan",
            ) { close(); onCreatePlan() }
            return@FormSheet
        }
        plans.forEach { plan ->
            val isSelected = plan.id == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { selected = plan.id })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = colors.brand, unselectedColor = colors.inkMuted),
                )
                Column(Modifier.weight(1f)) {
                    Text(plan.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    val count = plan.machines.size
                    Text(
                        "${ActivityCatalog.planLabel(plan)} · $count ${if (count == 1) "activity" else "activities"} · ${formatPlanLastDone(workouts, plan.id)}",
                        color = colors.inkMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        TextLink("+ New plan", onClick = { close(); onCreatePlan() }, contentDescription = "Create a new plan")
        if (onManagePlans != null) TextLink("Manage plans", onClick = { close(); onManagePlans() })
        PrimaryButton(confirmLabel, enabled = current != null, onClick = {
            val id = current ?: return@PrimaryButton
            close()
            onConfirm(id)
        })
    }
}

@Composable
private fun SessionDetailsSheet(workout: Workout, onSave: (date: String, durationSec: Int?, note: String) -> Unit, onDismiss: () -> Unit) {
    val initialDuration = formatSessionDurationInput(workout.durationSec)
    var date by remember { mutableStateOf(workout.date.ifBlank { todayIso() }) }
    var duration by remember { mutableStateOf(initialDuration) }
    var note by remember { mutableStateOf(workout.note) }
    var error by remember { mutableStateOf<String?>(null) }
    val dirty = date != workout.date || duration != initialDuration || note != workout.note
    FormSheet(onDismiss = onDismiss, isDirty = { dirty }) { close, requestClose ->
        SheetTitle("Session details", "Date is required. Duration and note are optional.")
        DateTimeFields(date, "", { date = it }, {}, showTime = false)
        LabeledField(
            "Duration (min)",
            duration,
            { duration = it; error = null },
            error = error,
            hint = "Minutes, mm:ss, or h:mm:ss.",
            keyboardType = KeyboardType.Text,
        )
        LabeledField("Note", note, { note = it.take(160) }, hint = "Optional. Travel gym, short on sleep.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Cancel", requestClose, Modifier.weight(1f))
            PrimaryButton("Save", modifier = Modifier.weight(1f), onClick = {
                val result = validateSessionDetails(duration, note)
                error = result.error
                if (result.error == null) {
                    onSave(date, result.durationSec, result.note)
                    close()
                }
            })
        }
    }
}

private data class SetTarget(val machineId: String, val entryId: String?, val key: Long = System.nanoTime())

@Composable
fun WorkoutFormScreen(
    store: FitnessStore,
    workout: Workout?,
    missing: Boolean,
    isDraft: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSaveDetails: (date: String, durationSec: Int?, note: String) -> Unit,
    onPlan: (String) -> Unit,
    onSaveEntry: (WorkoutEntry) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onDeleteWorkout: () -> Unit,
    onPlans: () -> Unit,
    onCreatePlan: () -> Unit,
    onAddActivity: (String) -> Unit,
    onRestTaken: (String, Int?) -> Unit = { _, _ -> },
    onRestTimerSettings: () -> Unit = {},
    onHowToLog: () -> Unit = {},
    backLabel: String = "Workouts",
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
    val context = LocalContext.current
    val useKg = store.profile.weightDisplayKg
    val plan = store.plans.find { it.id == workout.planId }
    val schema = plan?.let { ActivityCatalog.schema(it.activityTypeId) } ?: "load"
    val addLabel = ActivityCatalog.addEntryLabel(schema)
    val startedNew = remember(workout.id) { isDraft }
    var setTarget by remember { mutableStateOf<SetTarget?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var pendingEntry by remember { mutableStateOf<WorkoutEntry?>(null) }
    var pendingPlanId by remember { mutableStateOf<String?>(null) }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val restPref = store.profile.workoutRestTimerSec
    var restRemaining by remember { mutableIntStateOf(0) }
    var restEntryId by remember { mutableStateOf<String?>(null) }
    var restToken by remember { mutableLongStateOf(0L) }
    var seenCount by remember(workout.id) { mutableIntStateOf(workout.entries.size) }
    LaunchedEffect(workout.entries.size) {
        if (workout.entries.size > seenCount) {
            val last = workout.entries.lastOrNull()
            if (last != null && (last.schema == "load" || last.schema == "interval") && !isWarmupSet(last) && restPref > 0) {
                restRemaining = restPref
                restEntryId = last.id
                restToken = System.nanoTime()
            }
        }
        seenCount = workout.entries.size
    }
    LaunchedEffect(restToken) {
        while (restRemaining > 0) {
            delay(1000)
            restRemaining -= 1
            if (restRemaining == 0) {
                restEntryId?.let { onRestTaken(it, restPref) }
                restEntryId = null
            }
        }
    }

    fun leave(action: () -> Unit) {
        if (isDraft && draftHasDetails(workout)) pendingLeave = action else action()
    }
    BackHandler { leave(onBack) }

    val shareSession = {
        shareWorkoutReport(
            context,
            buildWorkoutReport(listOf(workout), store.plans, "${formatDate(todayIso())} ${formatTime(nowTime())}"),
            buildWorkoutCsv(listOf(workout), store.plans),
        )
    }
    AppScreen(
        lead = listOfNotNull(if (startedNew) "Log workout" else "Edit workout", plan?.let { ActivityCatalog.planLabel(it) }).joinToString(" · "),
        title = plan?.name ?: "Workout",
        onBack = { leave(onBack) },
        backLabel = backLabel,
        actions = listOf(
            MenuAction("Session details") { showDetails = true },
            MenuAction("Change plan") { showPlans = true },
            MenuAction("Share session") { shareSession() },
            MenuAction("How to log") { onHowToLog() },
            MenuAction("Rest timer") { onRestTimerSettings() },
            MenuAction("Delete workout", destructive = true) {
                if (isDraft) onBack() else confirmDelete = true
            },
        ),
        bottomBar = {
            if (restRemaining > 0) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Rest ${formatRestClock(restRemaining)}",
                        color = colors.ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        restRemaining = 0
                        restEntryId = null
                    }) { Text("Skip rest", color = colors.brandDark, fontWeight = FontWeight.SemiBold) }
                }
            }
            PrimaryButton("Finish", onClick = { leave(onFinish) })
        },
    ) {
        SettingsRow(formatDate(workout.date.ifBlank { todayIso() }), formatSessionDetailsLine(workout), { showDetails = true })
        if (plan == null) {
            EmptyState(
                "Choose a plan",
                "This workout is not linked to one of your plans. Choose one to add sets.",
                "Choose a plan",
            ) { showPlans = true }
        } else if (plan.machines.isEmpty()) {
            EmptyState(
                "This plan has no activities",
                "Add an activity such as Chest Press, Easy run, or a 20-minute stretch.",
                "Add an activity",
            ) { onAddActivity(plan.id) }
        }
        sessionActivities(plan, workout.entries).forEach { activity ->
            ActivityCard(
                activity = activity,
                schema = schema,
                addLabel = addLabel,
                store = store,
                workout = workout,
                useKg = useKg,
                canAdd = plan != null && activity.onPlan,
                onAdd = { setTarget = SetTarget(activity.machineId, null) },
                onEdit = { entry -> setTarget = SetTarget(entry.machineId, entry.id) },
                onDelete = { entry -> pendingEntry = entry },
            )
        }
    }

    val target = setTarget
    if (target != null && plan != null) {
        var dirty by remember { mutableStateOf(false) }
        FormSheet(
            onDismiss = { setTarget = null },
            isDirty = { dirty },
            discardBody = if (target.entryId == null) "This ${ActivityCatalog.entryNoun(schema, 1)} will not be added." else "Your edits will not be saved.",
        ) { close, requestClose ->
            key(target.key) {
                val existing = target.entryId?.let { id -> workout.entries.find { it.id == id } }
                WorkoutEntryForm(
                    plan = plan,
                    existing = existing,
                    missing = target.entryId != null && existing == null,
                    initialMachineId = target.machineId,
                    plans = store.plans,
                    workouts = store.workouts,
                    currentWorkoutId = workout.id,
                    currentWorkoutDate = workout.date,
                    currentEntries = workout.entries,
                    useKg = useKg,
                    onDirtyChange = { dirty = it },
                    onCancel = requestClose,
                    onClose = close,
                    onSave = { entry, addAnother ->
                        onSaveEntry(entry)
                        if (addAnother) setTarget = SetTarget(entry.machineId, null) else close()
                    },
                )
            }
        }
    }
    if (showDetails) {
        SessionDetailsSheet(workout, onSave = onSaveDetails, onDismiss = { showDetails = false })
    }
    if (showPlans) {
        PlanPickerSheet(
            plans = store.plans,
            workouts = store.workouts,
            selectedId = workout.planId,
            confirmLabel = "Use this plan",
            onConfirm = { planId ->
                if (planId != workout.planId && workout.entries.isNotEmpty()) pendingPlanId = planId else onPlan(planId)
            },
            onDismiss = { showPlans = false },
            onCreatePlan = onCreatePlan,
            onManagePlans = onPlans,
        )
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
            body = "Remove ${formatWorkoutEntrySummary(entry, useKg)} for $name from this workout?",
            confirmLabel = "Remove",
            onConfirm = { onRemoveEntry(entry.id); pendingEntry = null },
            onDismiss = { pendingEntry = null },
        )
    }
    pendingLeave?.let { action ->
        ConfirmDeleteDialog(
            title = "Nothing logged yet",
            body = "Discard this workout? The duration and note you entered will not be saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep logging",
            onConfirm = { pendingLeave = null; action() },
            onDismiss = { pendingLeave = null },
        )
    }
    if (confirmDelete) {
        WorkoutDeleteDialog(store, workout, onConfirm = { confirmDelete = false; onDeleteWorkout() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
private fun ActivityCard(
    activity: SessionActivity,
    schema: String,
    addLabel: String,
    store: FitnessStore,
    workout: Workout,
    useKg: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onEdit: (WorkoutEntry) -> Unit,
    onDelete: (WorkoutEntry) -> Unit,
) {
    val colors = LocalAppColors.current
    val sets = activity.entries
    val noun = ActivityCatalog.entryNoun(schema, 1).replaceFirstChar { it.uppercase() }
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(activity.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            if (sets.isEmpty()) Text("Not logged", color = colors.inkFaint, fontSize = 12.sp)
        }
        if (schema == "load") {
            val previous = findPreviousLoad(
                workouts = store.workouts,
                plans = store.plans,
                activityId = activity.machineId,
                currentWorkoutDate = workout.date,
                currentEntries = emptyList(),
                currentWorkoutId = workout.id,
            )
            val current = currentGroupLoad(sets)
            val vs = formatActivityVsLastTime(current, previous, useKg)
            val line = vs ?: previous?.takeIf { current == null }?.let { formatPreviousLoadSuggestion(it, useKg) }
            if (line != null) {
                val improved = current != null && previous != null && activityImproved(current, previous)
                Text(line, color = if (improved) colors.successText else colors.inkMuted, fontSize = 13.sp)
            }
            val history = if (store.workouts.any { it.id == workout.id }) store.workouts else store.workouts + workout
            activityPersonalRecord(history, store.plans, activity.machineId)?.let { record ->
                Text(formatActivityRecord(record, useKg), color = colors.inkMuted, fontSize = 12.sp)
            }
        } else if (sets.isEmpty()) {
            findPreviousEntry(
                workouts = store.workouts,
                plans = store.plans,
                activityId = activity.machineId,
                currentWorkoutDate = workout.date,
                currentEntries = emptyList(),
                currentWorkoutId = workout.id,
                schema = schema,
            )?.let { previous ->
                formatPreviousActivitySuggestion(previous, useKg).takeIf { it.isNotEmpty() }?.let {
                    Text(it, color = colors.inkMuted, fontSize = 13.sp)
                }
            }
        }
        sets.forEachIndexed { index, entry ->
            HorizontalDivider(color = colors.border)
            val label = "$noun ${index + 1}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClickLabel = "Edit ${activity.name} $label", onClick = { onEdit(entry) })
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(label, color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatWorkoutEntrySummary(entry, useKg), color = colors.ink, fontSize = 15.sp)
                }
                OverflowMenu(
                    listOf(MenuAction("Delete", destructive = true) { onDelete(entry) }),
                    "More actions for ${activity.name} $label",
                )
            }
        }
        if (canAdd) {
            SecondaryButton("+ $addLabel", onClick = onAdd, contentDescription = "$addLabel for ${activity.name}")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutEntryForm(
    plan: FitnessPlan,
    existing: WorkoutEntry?,
    missing: Boolean,
    initialMachineId: String,
    plans: List<FitnessPlan>,
    workouts: List<Workout>,
    currentWorkoutId: String,
    currentWorkoutDate: String,
    currentEntries: List<WorkoutEntry>,
    useKg: Boolean,
    onDirtyChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onSave: (entry: WorkoutEntry, addAnother: Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val schema = ActivityCatalog.schema(plan.activityTypeId)
    if (missing) {
        val noun = ActivityCatalog.entryNoun(schema, 1)
        SheetTitle("${noun.replaceFirstChar { it.uppercase() }} not found", "That $noun may have been deleted.")
        PrimaryButton("Close", onClick = onClose)
        return
    }
    val isNew = existing == null
    val tabataDefaults = isNew && plan.activityTypeId == "tabata"
    val entryId = remember { existing?.id ?: newId("entry") }
    val allPlans = plans.ifEmpty { listOf(plan) }
    var machineId by remember {
        mutableStateOf(
            existing?.machineId?.takeIf { it.isNotBlank() }
                ?: initialMachineId.takeIf { id -> plan.machines.any { it.id == id } }
                ?: plan.machines.firstOrNull()?.id.orEmpty(),
        )
    }
    val otherEntries = currentEntries.filterNot { it.id == existing?.id }
    fun lastTime(forMachineId: String) = findPreviousEntry(
        workouts = workouts,
        plans = allPlans,
        activityId = forMachineId,
        currentWorkoutDate = currentWorkoutDate,
        currentEntries = emptyList(),
        currentWorkoutId = currentWorkoutId,
        excludeEntryId = existing?.id,
        schema = schema,
    )
    fun prefillFor(forMachineId: String) = sessionPrefillEntry(otherEntries, forMachineId, schema)
    val unit = weightUnit(useKg)
    val extraMetrics = ActivityCatalog.entryMetrics(plan.activityTypeId)
    val initialSession = if (isNew && machineId.isNotBlank()) sessionPrefillEntry(otherEntries, machineId, schema) else null
    val initialPrevious = if (isNew && machineId.isNotBlank()) prefillFor(machineId) else null
    fun durationField(entry: WorkoutEntry?): String {
        val sec = entry?.durationSec?.takeIf { it > 0 } ?: return ""
        return if (schema == "distance_time") formatDurationClock(sec) else formatSessionDurationInput(sec)
    }
    fun metricMap(entry: WorkoutEntry?) = extraMetrics.associate { metric ->
        metric.id to entry?.metricDisplay(metric).orEmpty().let { if (it == "0" || it == "0.0") "" else it }
    }
    val source = existing ?: initialPrevious
    var weight by remember { mutableStateOf(source?.weight?.takeIf { existing != null || it > 0 }?.let { formatLiftWeight(it, useKg) }.orEmpty()) }
    var reps by remember { mutableStateOf(source?.repetitions?.takeIf { existing != null || it > 0 }?.toString().orEmpty()) }
    var distance by remember { mutableStateOf(source?.distanceMi?.takeIf { it > 0 }?.toString().orEmpty()) }
    var duration by remember { mutableStateOf(durationField(source)) }
    var rounds by remember {
        mutableStateOf(
            existing?.rounds?.toString()
                ?: initialPrevious?.rounds?.takeIf { it > 0 }?.toString()
                ?: if (tabataDefaults) "8" else "",
        )
    }
    var work by remember {
        mutableStateOf(
            existing?.workSec?.toString()
                ?: initialPrevious?.workSec?.takeIf { it > 0 }?.toString()
                ?: if (tabataDefaults) "20" else "",
        )
    }
    var rest by remember {
        mutableStateOf(
            existing?.restSec?.toString()
                ?: initialPrevious?.restSec?.takeIf { it > 0 }?.toString()
                ?: if (tabataDefaults) "10" else "",
        )
    }
    var intensity by remember { mutableStateOf(existing?.intensity.orEmpty()) }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var setKind by remember { mutableStateOf(WorkoutSetKind.fromId(existing?.setKind).id) }
    var rpe by remember { mutableStateOf(existing?.rpe?.toString().orEmpty()) }
    var metricDrafts by remember {
        mutableStateOf(metricMap(existing).mapValues { (id, value) -> value.ifEmpty { metricMap(initialPrevious)[id].orEmpty() } })
    }
    val detailMetrics = extraMetrics.filter { it.placement == EntryMetricPlacement.After }
    val hasDetailFields = schema == "load" || schema == "interval" || detailMetrics.isNotEmpty()
    var detailsOpen by remember {
        mutableStateOf(
            existing != null && (
                WorkoutSetKind.fromId(existing.setKind) != WorkoutSetKind.Working ||
                    existing.rpe != null ||
                    detailMetrics.any { metricMap(existing)[it.id].orEmpty().isNotEmpty() }
                ),
        )
    }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var dirty by remember { mutableStateOf(false) }
    var prefillNote by remember { mutableStateOf(if (initialSession != null) "Copied from your last ${ActivityCatalog.entryNoun(schema, 1)}." else null) }
    LaunchedEffect(dirty) { onDirtyChange(dirty) }

    fun markDirty() {
        dirty = true
    }

    fun applyPrevious(entry: WorkoutEntry) {
        weight = formatLiftWeight(entry.weight ?: 0.0, useKg)
        reps = (entry.repetitions ?: 0).takeIf { it > 0 }?.toString().orEmpty()
        distance = entry.distanceMi?.takeIf { it > 0 }?.toString().orEmpty()
        duration = durationField(entry)
        rounds = entry.rounds?.takeIf { it > 0 }?.toString().orEmpty().ifEmpty { if (tabataDefaults) "8" else "" }
        work = entry.workSec?.takeIf { it > 0 }?.toString().orEmpty().ifEmpty { if (tabataDefaults) "20" else "" }
        rest = entry.restSec?.takeIf { it > 0 }?.toString().orEmpty().ifEmpty { if (tabataDefaults) "10" else "" }
        metricDrafts = metricMap(entry)
        markDirty()
    }

    fun builtEntry(): Pair<WorkoutEntry?, Map<String, String>> {
        val next = mutableMapOf<String, String>()
        if (machineId.isBlank() || plan.machines.none { it.id == machineId }) {
            next["machineId"] = "Select an activity."
        }
        val parsedRpe = rpe.trim().toIntOrNull()
        if (rpe.isNotBlank() && (parsedRpe == null || parsedRpe !in 1..10)) {
            next["rpe"] = "Enter 1–10."
        }
        val built = (existing ?: WorkoutEntry(id = entryId, planId = plan.id, machineId = machineId, schema = schema))
            .copy(
                planId = plan.id,
                machineId = machineId,
                schema = schema,
                setKind = WorkoutSetKind.fromId(setKind).id,
                rpe = parsedRpe?.takeIf { it in 1..10 },
            )
        val entry = when (schema) {
            "load" -> {
                val w = parseLiftWeightInput(weight, useKg, existing?.weight)
                val r = reps.toIntOrNull()
                if (weight.isNotBlank() && w == null) next["weight"] = "Weight must be 0 or greater."
                if (reps.isNotBlank() && (r == null || r < 0)) next["repetitions"] = "Repetitions must be 0 or greater."
                built.copy(weight = w?.takeIf { it >= 0 } ?: 0.0, repetitions = r?.takeIf { it >= 0 } ?: 0)
            }
            "distance_time" -> {
                val miles = distance.toDoubleOrNull()
                val sec = if (duration.isBlank()) 0 else parseDurationInput(duration)
                if (distance.isNotBlank() && (miles == null || miles < 0)) next["distanceMi"] = "Distance must be 0 or greater."
                if (duration.isNotBlank() && sec == null) next["durationSec"] = "Enter time as mm:ss or minutes."
                built.copy(distanceMi = miles?.takeIf { it >= 0 } ?: 0.0, durationSec = sec ?: 0)
            }
            "duration", "sport" -> {
                val sec = if (duration.isBlank()) 0 else parseDurationInput(duration)
                if (duration.isNotBlank() && sec == null) next["durationSec"] = "Enter minutes greater than 0."
                built.copy(durationSec = sec ?: 0, intensity = intensity.ifBlank { null })
            }
            "interval" -> {
                val roundCount = rounds.toIntOrNull()
                if (rounds.isNotBlank() && (roundCount == null || roundCount < 0)) next["rounds"] = "Enter a whole number of rounds."
                val workSec = work.toIntOrNull()
                val restSec = rest.toIntOrNull()
                if (work.isNotBlank() && (workSec == null || workSec < 1)) next["workSec"] = "Enter whole-number seconds."
                if (rest.isNotBlank() && (restSec == null || restSec < 1)) next["restSec"] = "Enter whole-number seconds."
                built.copy(rounds = roundCount?.takeIf { it >= 0 } ?: 0, workSec = workSec, restSec = restSec)
            }
            else -> built.copy(note = note.trim())
        }
        hollowWorkoutEntryError(entry)?.let { (field, message) -> next.putIfAbsent(field, message) }
        val (entryWithMetrics, metricErrors) = entry.withParsedMetrics(extraMetrics, metricDrafts)
        next.putAll(metricErrors)
        return if (next.isEmpty()) entryWithMetrics to emptyMap() else null to next
    }

    fun submit(addAnother: Boolean) {
        val (entry, nextErrors) = builtEntry()
        errors = nextErrors
        if (nextErrors.keys.any { it == "rpe" || detailMetrics.any { metric -> metric.id == it } }) detailsOpen = true
        if (entry != null) {
            dirty = false
            onDirtyChange(false)
            onSave(entry, addAnother)
        }
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

    val lastTimeEntry = if (machineId.isNotBlank()) lastTime(machineId) else null
    val lastTimeCopy = lastTimeEntry?.let { formatPreviousActivitySuggestion(it, useKg) }.orEmpty()
    SheetTitle(
        if (isNew) ActivityCatalog.addEntryLabel(schema) else ActivityCatalog.editEntryLabel(schema),
        "${plan.name} · ${ActivityCatalog.label(plan.activityTypeId)}",
    )
    MachineMenu(plan, machineId) { nextId ->
        machineId = nextId
        if (isNew) {
            val fromSession = sessionPrefillEntry(otherEntries, nextId, schema)
            prefillNote = if (fromSession != null) "Copied from your last ${ActivityCatalog.entryNoun(schema, 1)}." else null
            prefillFor(nextId)?.let { applyPrevious(it) }
        }
        markDirty()
    }
    errors["machineId"]?.let { Text(it, color = colors.danger, fontSize = 12.sp) }
    prefillNote?.let { Text(it, color = colors.inkMuted, fontSize = 13.sp) }
    if (lastTimeCopy.isNotEmpty() && lastTimeEntry != null) {
        LastTimeSuggestion(lastTimeCopy, onUse = {
            applyPrevious(lastTimeEntry)
            prefillNote = null
        })
    }
    extraMetrics.filter { it.placement == EntryMetricPlacement.Before }.forEach { metricField(it) }
    when (schema) {
        "load" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Weight ($unit)", weight, { weight = it; markDirty() }, Modifier.weight(1f), errors["weight"], keyboardType = KeyboardType.Decimal)
                LabeledField("Reps", reps, { reps = it.filter(Char::isDigit); markDirty() }, Modifier.weight(1f), errors["repetitions"], keyboardType = KeyboardType.Number)
            }
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
    if (hasDetailFields) {
        AddDetails(expanded = detailsOpen, onToggle = { detailsOpen = !detailsOpen }) {
            if (schema == "load" || schema == "interval") {
                Text("Set type", color = colors.inkMuted, fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutSetKind.entries.forEach { kind ->
                        ChoiceChip(kind.label, selected = setKind == kind.id, onClick = {
                            setKind = kind.id
                            markDirty()
                        })
                    }
                }
                LabeledField(
                    "How hard (RPE)",
                    rpe,
                    { rpe = it.filter(Char::isDigit).take(2); markDirty() },
                    error = errors["rpe"],
                    hint = "Borg-style 1–10. Optional.",
                    keyboardType = KeyboardType.Number,
                )
            }
            detailMetrics.forEach { metricField(it) }
        }
    }
    if (isNew) {
        SecondaryButton("Save & add another", onClick = { submit(addAnother = true) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Cancel", onCancel, Modifier.weight(1f))
        PrimaryButton("Save", modifier = Modifier.weight(1f), onClick = { submit(addAnother = false) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MachineMenu(plan: FitnessPlan, selectedId: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = plan.machines.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Select activity",
            onValueChange = {},
            readOnly = true,
            label = { Text("Activity") },
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

@Composable
private fun WorkoutWeekCard(summary: WorkoutWeekSummary) {
    val colors = LocalAppColors.current
    val strength = (summary.strengthDays.toFloat() / summary.strengthTarget).coerceIn(0f, 1f)
    val aerobic = (summary.aerobicMinutes.toFloat() / summary.aerobicTarget).coerceIn(0f, 1f)
    SurfaceCard {
        Text("THIS WEEK", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
        Text(formatWorkoutWeekStrength(summary), color = colors.ink, fontWeight = FontWeight.Medium)
        LinearProgressIndicator(
            progress = { strength },
            modifier = Modifier.fillMaxWidth(),
            color = colors.brand,
            trackColor = colors.border,
        )
        Text(formatWorkoutWeekAerobic(summary), color = colors.ink, fontWeight = FontWeight.Medium)
        LinearProgressIndicator(
            progress = { aerobic },
            modifier = Modifier.fillMaxWidth(),
            color = colors.brand,
            trackColor = colors.border,
        )
        Text(formatWorkoutWeekCaption(), color = colors.inkMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/** Hero content for the hub card; the Home card reuses it so both read the same. */
@Composable
fun WorkoutHero(store: FitnessStore, onStartAgain: ((String) -> Unit)? = null) {
    val colors = LocalAppColors.current
    val useKg = store.profile.weightDisplayKg
    val lastWorkout = latestWorkout(store.workouts) ?: return
    val lastPlan = store.plans.find { it.id == lastWorkout.planId }
    val lastVs = formatWorkoutVsLastTime(lastWorkout, store.workouts, store.plans, useKg)
    HeroHeader("Last workout", lastPlan?.name ?: ActivityCatalog.workoutLabel(lastWorkout, lastPlan), valueSize = 28.sp)
    Text(summarizeWorkout(lastWorkout, useKg), color = colors.ink, fontWeight = FontWeight.Medium)
    lastVs?.let {
        val improved = workoutVsLastImproved(lastWorkout, store.workouts, store.plans)
        Text(
            it,
            color = if (improved) colors.successText else colors.inkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
    Text(
        "${formatRelativeDays(lastWorkout.date)} · ${ActivityCatalog.workoutLabel(lastWorkout, lastPlan)}",
        color = colors.inkMuted,
    )
    if (lastPlan != null && onStartAgain != null) {
        SecondaryButton(
            "Start again",
            onClick = { onStartAgain(lastPlan.id) },
            contentDescription = "Start again with ${lastPlan.name}",
        )
    }
}
