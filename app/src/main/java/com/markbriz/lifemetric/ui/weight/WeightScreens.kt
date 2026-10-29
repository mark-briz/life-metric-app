package com.markbriz.lifemetric.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.domain.WeightChangeDirection
import com.markbriz.lifemetric.domain.WeightChartRange
import com.markbriz.lifemetric.domain.WeightTag
import com.markbriz.lifemetric.domain.buildWeightCsv
import com.markbriz.lifemetric.domain.buildWeightReport
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.chronologicalWeights
import com.markbriz.lifemetric.domain.filterWeightRecords
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.formatWaistCaption
import com.markbriz.lifemetric.domain.formatWeight
import com.markbriz.lifemetric.domain.heightParts
import com.markbriz.lifemetric.domain.formatWeightGoalEta
import com.markbriz.lifemetric.domain.formatWeightHistoryLine
import com.markbriz.lifemetric.domain.formatWeightRateCaption
import com.markbriz.lifemetric.domain.formatWeightTrendDelta
import com.markbriz.lifemetric.domain.formatWeightValue
import com.markbriz.lifemetric.domain.formatWeightWeeklyRate
import com.markbriz.lifemetric.domain.formatWeightWithUnit
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.latestWeightTrend
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.parseWeightInput
import com.markbriz.lifemetric.domain.progressChangeLb
import com.markbriz.lifemetric.domain.recentWeights
import com.markbriz.lifemetric.domain.stepDisplayedWeight
import com.markbriz.lifemetric.domain.targetChangeLb
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.validateWeightLb
import com.markbriz.lifemetric.domain.weightChangeDirection
import com.markbriz.lifemetric.domain.weightChartModel
import com.markbriz.lifemetric.domain.weightGoalEtaDate
import com.markbriz.lifemetric.domain.weightGoalProgress
import com.markbriz.lifemetric.domain.weightGoalProgressLabel
import com.markbriz.lifemetric.domain.weightGoalTargetLabel
import com.markbriz.lifemetric.domain.weightRecordTags
import com.markbriz.lifemetric.domain.weightRecordsInRange
import com.markbriz.lifemetric.domain.weightTrendDelta
import com.markbriz.lifemetric.domain.weightUnit
import com.markbriz.lifemetric.domain.weightWeeklyRate
import com.markbriz.lifemetric.ui.profile.shareWeightReport
import kotlin.math.roundToInt
import com.markbriz.lifemetric.ui.components.ActiveFilter
import com.markbriz.lifemetric.ui.components.AddDetails
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BmiGauge
import com.markbriz.lifemetric.ui.components.ChoiceChip
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.FilterBar
import com.markbriz.lifemetric.ui.components.FilterSection
import com.markbriz.lifemetric.ui.components.FilterSheet
import com.markbriz.lifemetric.ui.components.FormDelete
import com.markbriz.lifemetric.ui.components.FormSheet
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HeroHeader
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.LastTimeSuggestion
import com.markbriz.lifemetric.ui.components.MenuAction
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.RecordFormScaffold
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.Segment
import com.markbriz.lifemetric.ui.components.SegmentedChoice
import com.markbriz.lifemetric.ui.components.SheetTitle
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.components.WeightChart
import com.markbriz.lifemetric.ui.theme.LocalAppColors

private const val HISTORY_PAGE = 30

@Composable
fun WeightScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditGoal: () -> Unit,
    onHowToWeigh: () -> Unit,
    onSettings: () -> Unit,
    highlightedId: String? = null,
    /** Saves height and waist together; returns an error message or null. */
    onSaveBodyMeasures: (feet: String, inches: String, waist: String) -> String?,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val useKg = store.profile.weightDisplayKg
    val latest = latestWeight(store.weightRecords)
    val trend = latestWeightTrend(store.weightRecords)
    val trendDelta = weightTrendDelta(store.weightRecords)
    val sorted = remember(store.weightRecords) { recentWeights(store.weightRecords, store.weightRecords.size) }
    var chartRange by remember { mutableStateOf(WeightChartRange.Last30) }
    var listRange by remember { mutableStateOf(WeightChartRange.All) }
    var tagFilters by remember { mutableStateOf(setOf<String>()) }
    var visibleCount by remember { mutableIntStateOf(HISTORY_PAGE) }
    var filterOpen by remember { mutableStateOf(false) }
    var bodyOpen by remember { mutableStateOf(false) }
    val filterFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }
    val filtered = remember(store.weightRecords, listRange, tagFilters) {
        filterWeightRecords(sorted, listRange, tagFilters)
    }
    val recent = filtered.take(visibleCount)
    val usedTags = WeightTag.entries.filter { tag ->
        tag.id in tagFilters || store.weightRecords.any { tag in weightRecordTags(it) }
    }
    val chart = remember(store.weightRecords, chartRange) {
        weightChartModel(store.weightRecords, chartRange)
    }
    var pending by remember { mutableStateOf<WeightRecord?>(null) }
    val height = store.profile.heightInches
    val bmi = if (latest != null && height != null) calculateBmi(latest.weight, height) else Double.NaN
    val goalBmi = if (height != null && store.profile.weightGoalLb != null) calculateBmi(store.profile.weightGoalLb, height) else Double.NaN
    val showTrend = latest != null && store.weightRecords.size >= 2 && trend != null
    val setRange: (WeightChartRange) -> Unit = {
        listRange = it
        visibleCount = HISTORY_PAGE
    }
    val toggleTag: (String) -> Unit = { tagId ->
        tagFilters = if (tagId in tagFilters) tagFilters - tagId else tagFilters + tagId
        visibleCount = HISTORY_PAGE
    }
    val clearFilters: () -> Unit = {
        listRange = WeightChartRange.All
        tagFilters = emptySet()
        visibleCount = HISTORY_PAGE
    }
    val activeFilters = buildList {
        listRange.days?.let { days -> add(ActiveFilter("Last $days days") { setRange(WeightChartRange.All) }) }
        WeightTag.entries.filter { it.id in tagFilters }.forEach { tag ->
            add(ActiveFilter(tag.label) { toggleTag(tag.id) })
        }
    }

    val shareLog: () -> Unit = {
        val inRange = weightRecordsInRange(store.weightRecords, chartRange)
        shareWeightReport(
            context,
            buildWeightReport(inRange, generatedAt = "${formatDate(todayIso())} ${formatTime(nowTime())}"),
            buildWeightCsv(inRange),
        )
    }

    AppScreen(
        title = "Weight",
        showFab = true,
        fabLabel = "Record weight",
        onFab = onAdd,
        actions = listOfNotNull(
            MenuAction("How to weigh") { onHowToWeigh() },
            if (store.weightRecords.isNotEmpty()) MenuAction("Share log") { shareLog() } else null,
            MenuAction("Weight settings") { onSettings() },
        ),
    ) {
        if (latest == null) {
            EmptyState(
                "No weights recorded",
                "Record your first scale reading to start the history chart.",
                "Record weight",
                onAdd,
            )
        } else {
            SurfaceCard { WeightHero(store.weightRecords, useKg) }
            SectionTitle("Trend")
            SurfaceCard {
                SegmentedChoice(
                    segments = WeightChartRange.hubChoices.map { range ->
                        Segment(range, range.label, range.days?.let { "Last $it days" } ?: "All days")
                    },
                    selected = chartRange,
                    onSelect = { chartRange = it },
                    unitLabel = "Days",
                )
                if (chart != null && chart.points.isNotEmpty()) {
                    WeightChart(chart, store.profile.weightGoalLb, useKg)
                } else {
                    Text("No daily lows in this range. Try a longer window.", color = colors.inkMuted)
                }
            }
        }
        SectionTitle("Goal")
        WeightTargetSection(store, onEditGoal, useKg)
        if (latest != null) {
            SectionTitle("Body measures")
            if (height != null) {
                SurfaceCard {
                    BmiGauge(bmi, goalBmi)
                    Text(formatWaistCaption(store.profile.waistInches), color = colors.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
                    TextLink(
                        "Edit",
                        onClick = { bodyOpen = true },
                        contentDescription = "Edit height and waist",
                        modifier = Modifier.focusRequester(bodyFocus),
                    )
                }
            } else {
                EmptyState(
                    "Height needed for BMI",
                    "Add your height so BMI can be calculated from the latest weight.",
                    "Set height",
                ) { bodyOpen = true }
            }
        }
        if (sorted.isNotEmpty()) {
            val previousWeightById = remember(store.weightRecords) {
                val chronological = chronologicalWeights(store.weightRecords)
                chronological.mapIndexedNotNull { index, record ->
                    if (index == 0) null else record.id to chronological[index - 1].weight
                }.toMap()
            }
            SectionTitle("History")
            FilterBar(activeFilters, onOpen = { filterOpen = true }, onClearAll = clearFilters, openerFocus = filterFocus)
            if (filtered.isEmpty()) {
                Text("No readings match these filters.", color = colors.inkMuted)
            }
            recent.forEach { record ->
                HistoryRow(
                    primary = formatWeightWithUnit(record.weight, useKg),
                    secondary = formatWeightHistoryLine(record),
                    onClick = { onEdit(record.id) },
                    trailing = { WeightChangeArrow(record.weight, previousWeightById[record.id]) },
                    overflowLabel = "Delete",
                    onOverflow = { pending = record },
                    highlighted = record.id == highlightedId,
                )
            }
            if (recent.size < filtered.size) {
                TextLink("Show older", onClick = { visibleCount += HISTORY_PAGE })
            }
        }
    }
    if (filterOpen) {
        FilterSheet(
            resultSummary = "Showing ${filtered.size} of ${sorted.size} readings.",
            onDismiss = { filterOpen = false },
            onClearAll = clearFilters,
            returnFocus = filterFocus,
        ) {
            FilterSection("Range") {
                WeightChartRange.hubChoices.forEach { range ->
                    ChoiceChip(
                        range.days?.let { "$it days" } ?: "All",
                        selected = listRange == range,
                        onClick = { setRange(range) },
                    )
                }
            }
            if (usedTags.isNotEmpty()) {
                FilterSection("Tags") {
                    usedTags.forEach { tag ->
                        ChoiceChip(tag.label, selected = tag.id in tagFilters, onClick = { toggleTag(tag.id) })
                    }
                }
            }
        }
    }
    if (bodyOpen) {
        BodyMeasuresSheet(
            heightInches = height,
            waistInches = store.profile.waistInches,
            onSave = onSaveBodyMeasures,
            onDismiss = { bodyOpen = false },
            returnFocus = bodyFocus,
        )
    }
    pending?.let { record ->
        ConfirmDeleteDialog(
            title = "Delete this reading?",
            body = "Delete the ${formatDate(record.date)} reading of ${formatWeightWithUnit(record.weight, store.profile.weightDisplayKg)}?",
            onConfirm = { onDelete(record.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun BodyMeasuresSheet(
    heightInches: Int?,
    waistInches: Double?,
    onSave: (feet: String, inches: String, waist: String) -> String?,
    onDismiss: () -> Unit,
    returnFocus: FocusRequester? = null,
) {
    val colors = LocalAppColors.current
    val parts = heightParts(heightInches)
    var feet by remember { mutableStateOf(parts.first) }
    var inches by remember { mutableStateOf(parts.second) }
    var waist by remember { mutableStateOf(waistInches?.let { formatWeight(it) }.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val initial = remember { listOf(feet, inches, waist) }
    FormSheet(onDismiss = onDismiss, isDirty = { listOf(feet, inches, waist) != initial }, returnFocus = returnFocus) { close, requestClose ->
        SheetTitle("Body measures", "Height is used for BMI. Waist is optional.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField("Feet", feet, { feet = it.filter(Char::isDigit) }, Modifier.weight(1f), hint = "e.g. 5", keyboardType = KeyboardType.Number)
            LabeledField("Inches", inches, { inches = it.filter(Char::isDigit) }, Modifier.weight(1f), hint = "0–11", keyboardType = KeyboardType.Number)
        }
        LabeledField("Waist (in)", waist, { waist = it }, hint = "Leave blank to clear.", keyboardType = KeyboardType.Decimal)
        if (error != null) Text(error!!, color = colors.danger)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Cancel", requestClose, Modifier.weight(1f))
            PrimaryButton(
                "Save",
                onClick = {
                    error = onSave(feet, inches, waist)
                    if (error == null) close()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeightChangeArrow(weight: Double, previous: Double?) {
    val direction = weightChangeDirection(weight, previous) ?: return
    val colors = LocalAppColors.current
    val (icon, description, tint) = when (direction) {
        WeightChangeDirection.Up -> Triple(Icons.Default.ArrowUpward, "Weight increased", colors.danger)
        WeightChangeDirection.Down -> Triple(Icons.Default.ArrowDownward, "Weight decreased", colors.success)
        WeightChangeDirection.Unchanged -> Triple(Icons.Default.Remove, "No meaningful change", colors.ink)
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun WeightTargetSection(store: FitnessStore, onEditGoal: () -> Unit, useKg: Boolean) {
    val colors = LocalAppColors.current
    val startExists = store.weightRecords.any { it.id == store.profile.weightGoalStartId }
    if (!startExists) {
        EmptyState(
            "Add a starting weight goal",
            "Choose a recorded scale reading as the start of this goal.",
            "Choose starting weight",
            onEditGoal,
        )
        return
    }
    val progress = weightGoalProgress(store.weightRecords, store.profile.weightGoalStartId, store.profile.weightGoalLb)
    if (progress == null) {
        EmptyState(
            "Add a target weight",
            "Set a target weight to see progress from your starting reading.",
            "Set target weight",
            onEditGoal,
        )
        return
    }
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("START", color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(formatWeightWithUnit(progress.startLb, useKg), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                "${progress.percentComplete.roundToInt()}%",
                color = colors.brandDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("TARGET", color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(formatWeightWithUnit(progress.targetLb, useKg), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        LinearProgressIndicator(
            progress = { (progress.percentComplete / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
            color = colors.brand,
            trackColor = colors.surfaceMuted,
        )
        Text("Percent complete", color = colors.inkFaint, fontSize = 12.sp)
        HorizontalDivider(color = colors.border)
        GoalStatRow(weightGoalTargetLabel(progress.kind), formatWeightWithUnit(progress.targetChangeLb(), useKg))
        HorizontalDivider(color = colors.border)
        GoalStatRow(weightGoalProgressLabel(progress.kind), formatWeightWithUnit(progress.progressChangeLb(), useKg))
        weightWeeklyRate(store.weightRecords)?.let { rate ->
            HorizontalDivider(color = colors.border)
            GoalStatRow("Weekly rate", formatWeightWeeklyRate(rate, useKg))
            Text(formatWeightRateCaption(rate.days), color = colors.inkFaint, fontSize = 12.sp)
        }
        weightGoalEtaDate(store.weightRecords, store.profile.weightGoalStartId, store.profile.weightGoalLb)?.let { eta ->
            Text(formatWeightGoalEta(eta), color = colors.ink, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Estimate only. Daily scale weight jumps around.", color = colors.inkFaint, fontSize = 12.sp)
        }
        TextLink("Change goal", onClick = onEditGoal)
    }
}

@Composable
private fun GoalStatRow(label: String, value: String) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.inkMuted, fontSize = 14.sp)
        Text(value, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun WeightFormScreen(
    existing: WeightRecord?,
    lastWeight: Double?,
    useKg: Boolean,
    missing: Boolean,
    onBack: () -> Unit,
    onHowToWeigh: () -> Unit,
    onSave: (date: String, time: String, weight: Double, tags: List<String>, note: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    if (missing) {
        AppScreen(title = "Reading not found", lead = "That weight reading may have been deleted.", onBack = onBack, backLabel = "Weight") {}
        return
    }
    val unit = weightUnit(useKg)
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: todayIso()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time ?: nowTime()) }
    var weight by remember(existing?.id) { mutableStateOf(existing?.weight?.let { formatWeightValue(it, useKg) }.orEmpty()) }
    var selectedTags by remember(existing?.id) { mutableStateOf(existing?.tags?.toSet().orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailsOpen by remember(existing?.id) {
        mutableStateOf(existing?.tags?.isNotEmpty() == true || existing?.note?.isNotBlank() == true)
    }
    val isEdit = existing != null
    val fields = listOf(date, time, weight, selectedTags.sorted().joinToString(","), note)
    val initialFields = remember(existing?.id) { fields }
    fun stepWeight(delta: Double) {
        val currentLb = parseWeightInput(weight, useKg) ?: lastWeight ?: return
        weight = formatWeightValue(stepDisplayedWeight(currentLb, delta, useKg), useKg)
    }
    val submit: () -> Unit = {
        val parsed = parseWeightInput(weight, useKg)
        error = when {
            date.isBlank() -> "Date is required."
            time.isBlank() -> "Time is required."
            else -> validateWeightLb(parsed)
        }
        if (error == null && parsed != null) onSave(date, time, parsed, selectedTags.toList(), note.trim())
    }
    RecordFormScaffold(
        title = if (isEdit) "Edit weight" else "Record weight",
        lead = "Date and time default to now. Adjust weight by 0.1 $unit.",
        isDirty = fields != initialFields,
        onClose = onBack,
        onSave = submit,
        actions = listOf(MenuAction("How to weigh") { onHowToWeigh() }),
        delete = if (existing != null && onDelete != null) {
            FormDelete(
                title = "Delete this reading?",
                body = "Delete the ${formatDate(existing.date)} reading of ${formatWeightWithUnit(existing.weight, useKg)}?",
                onConfirm = onDelete,
            )
        } else {
            null
        },
    ) {
        SurfaceCard {
            DateTimeFields(date, time, { date = it }, { time = it })
            Text("Weight ($unit)", color = LocalAppColors.current.inkMuted, fontWeight = FontWeight.Medium)
            if (!isEdit && lastWeight != null) {
                LastTimeSuggestion(
                    "Last time: ${formatWeightWithUnit(lastWeight, useKg)}.",
                    onUse = { weight = formatWeightValue(lastWeight, useKg) },
                    useDescription = "Use last weight",
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(
                    "−",
                    onClick = { stepWeight(-0.1) },
                    contentDescription = "Decrease weight",
                )
                LabeledField(
                    label = unit,
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.weight(1f),
                    error = error,
                    hint = if (isEdit) "Use − / + to adjust by 0.1 $unit." else "Type the number from the scale.",
                    keyboardType = KeyboardType.Decimal,
                )
                GhostButton(
                    "+",
                    onClick = { stepWeight(0.1) },
                    contentDescription = "Increase weight",
                )
            }
            AddDetails(
                expanded = detailsOpen,
                onToggle = { detailsOpen = !detailsOpen },
                label = if (selectedTags.isEmpty() && note.isBlank()) "Add details" else "Details",
            ) {
                Text("Tags", color = LocalAppColors.current.inkMuted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                WeightTagChipRow(
                    selected = selectedTags,
                    onToggle = { tagId ->
                        selectedTags = if (tagId in selectedTags) selectedTags - tagId else selectedTags + tagId
                    },
                )
                LabeledField(
                    "Note",
                    note,
                    { note = it.take(160) },
                    hint = "Optional one-line note",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeightTagChipRow(
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WeightTag.entries.forEach { tag ->
            ChoiceChip(tag.label, selected = tag.id in selected, onClick = { onToggle(tag.id) })
        }
    }
}

/** Hero content for the hub card; the Home card reuses it so both read the same. */
@Composable
fun WeightHero(records: List<WeightRecord>, useKg: Boolean) {
    val colors = LocalAppColors.current
    val latest = latestWeight(records) ?: return
    val trend = latestWeightTrend(records)
    val trendDelta = weightTrendDelta(records)
    val showTrend = records.size >= 2 && trend != null
    HeroHeader(
        if (showTrend) "7-day trend" else "Latest",
        formatWeightWithUnit(if (showTrend) trend ?: latest.weight else latest.weight, useKg),
        valueSize = 40.sp,
    )
    Text(
        "Last: ${formatWeightWithUnit(latest.weight, useKg)} · ${formatDate(latest.date)} · ${formatTime(latest.time)}",
        color = colors.inkMuted,
    )
    if (showTrend && trendDelta != null) {
        Text(
            formatWeightTrendDelta(trendDelta, useKg),
            color = if (trendDelta <= 0) colors.successText else colors.accentText,
            fontWeight = FontWeight.Medium,
        )
    }
}
