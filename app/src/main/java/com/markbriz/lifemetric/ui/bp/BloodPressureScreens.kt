package com.markbriz.lifemetric.ui.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.BloodPressureSample
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.newId
import com.markbriz.lifemetric.domain.BloodPressureChartRange
import com.markbriz.lifemetric.domain.BloodPressurePeriod
import com.markbriz.lifemetric.domain.BloodPressureSave
import com.markbriz.lifemetric.domain.BloodPressureTag
import com.markbriz.lifemetric.domain.BloodPressureVisitSummary
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.averageBloodPressureSamples
import com.markbriz.lifemetric.domain.bloodPressureDelta
import com.markbriz.lifemetric.domain.bloodPressureLowHint
import com.markbriz.lifemetric.domain.bloodPressurePeriod
import com.markbriz.lifemetric.domain.bloodPressureSaveHint
import com.markbriz.lifemetric.domain.bloodPressureSaveWarning
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.bloodPressureVisitSummary
import com.markbriz.lifemetric.domain.buildBloodPressureCsv
import com.markbriz.lifemetric.domain.buildBloodPressureReport
import com.markbriz.lifemetric.domain.chartBloodPressure
import com.markbriz.lifemetric.domain.filterBloodPressureReadings
import com.markbriz.lifemetric.domain.formatBloodPressure
import com.markbriz.lifemetric.domain.formatBloodPressureCompact
import com.markbriz.lifemetric.domain.formatBloodPressureDelta
import com.markbriz.lifemetric.domain.formatBloodPressureHistoryLine
import com.markbriz.lifemetric.domain.formatBloodPressureTarget
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatDerivedBloodPressure
import com.markbriz.lifemetric.domain.formatPreviousBloodPressure
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.inferBloodPressurePeriod
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.validateBloodPressureReading
import com.markbriz.lifemetric.ui.components.ActiveFilter
import com.markbriz.lifemetric.ui.components.AddDetails
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BloodPressureZoneChart
import com.markbriz.lifemetric.ui.components.BpBadge
import com.markbriz.lifemetric.ui.components.BpGauge
import com.markbriz.lifemetric.ui.components.ChoiceChip
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.ConfirmDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.FilterBar
import com.markbriz.lifemetric.ui.components.FilterSection
import com.markbriz.lifemetric.ui.components.FilterSheet
import com.markbriz.lifemetric.ui.components.FormDelete
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
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.profile.shareBloodPressureReport
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

private const val HISTORY_PAGE = 30

private enum class BpTrendView { Chart, Category }

@Composable
fun BloodPressureScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditBpWindow: () -> Unit,
    onEditBpTarget: () -> Unit,
    onHowToMeasure: () -> Unit,
    onSettings: () -> Unit,
    highlightedId: String? = null,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val latest = latestBloodPressure(store.bloodPressureReadings)
    val summary = bloodPressureVisitSummary(
        store.bloodPressureReadings,
        store.profile.bpTargetSystolic,
        store.profile.bpTargetDiastolic,
    )
    var chartRange by remember { mutableStateOf(BloodPressureChartRange.Last10) }
    var trendView by remember { mutableStateOf(BpTrendView.Chart) }
    var periodFilter by remember { mutableStateOf<BloodPressurePeriod?>(null) }
    var tagFilters by remember { mutableStateOf(setOf<String>()) }
    var visibleCount by remember { mutableIntStateOf(HISTORY_PAGE) }
    var filterOpen by remember { mutableStateOf(false) }
    val filterFocus = remember { FocusRequester() }
    val trend = chartBloodPressure(store.bloodPressureReadings, chartRange)
    val sorted = remember(store.bloodPressureReadings) {
        store.bloodPressureReadings.sortedWith(
            compareByDescending<BloodPressureReading> { "${it.date}T${it.time}" }.thenByDescending { it.id },
        )
    }
    val filtered = filterBloodPressureReadings(sorted, periodFilter, tagFilters)
    val recent = filtered.take(visibleCount)
    val usedTags = BloodPressureTag.entries.filter { tag ->
        tag.id in tagFilters || store.bloodPressureReadings.any { tag.id in it.tags }
    }
    val setPeriod: (BloodPressurePeriod?) -> Unit = {
        periodFilter = it
        visibleCount = HISTORY_PAGE
    }
    val toggleTag: (String) -> Unit = { tagId ->
        tagFilters = if (tagId in tagFilters) tagFilters - tagId else tagFilters + tagId
        visibleCount = HISTORY_PAGE
    }
    val clearFilters: () -> Unit = {
        periodFilter = null
        tagFilters = emptySet()
        visibleCount = HISTORY_PAGE
    }
    val activeFilters = buildList {
        periodFilter?.let { period -> add(ActiveFilter(period.label) { setPeriod(null) }) }
        BloodPressureTag.entries.filter { it.id in tagFilters }.forEach { tag ->
            add(ActiveFilter(tag.label) { toggleTag(tag.id) })
        }
    }
    var pending by remember { mutableStateOf<BloodPressureReading?>(null) }
    val shareReport: () -> Unit = {
        shareBloodPressureReport(
            context,
            buildBloodPressureReport(
                store.bloodPressureReadings,
                store.profile.bpTargetSystolic,
                store.profile.bpTargetDiastolic,
                generatedAt = "${formatDate(todayIso())} ${formatTime(nowTime())}",
            ),
            buildBloodPressureCsv(store.bloodPressureReadings),
        )
    }

    AppScreen(
        title = "Blood pressure",
        showFab = true,
        fabLabel = "Add reading",
        onFab = onAdd,
        actions = listOfNotNull(
            MenuAction("How to measure") { onHowToMeasure() },
            if (store.bloodPressureReadings.isNotEmpty()) MenuAction("Share report") { shareReport() } else null,
            MenuAction("Blood pressure settings") { onSettings() },
        ),
    ) {
        if (latest == null) {
            EmptyState(
                "No blood pressure readings yet",
                "Add a reading with date, time, systolic, and diastolic. Heart rate is optional.",
                "Add your first reading",
                onAdd,
            )
            return@AppScreen
        }
        SurfaceCard { BloodPressureHero(latest, store.bloodPressureReadings) }

        SectionTitle("Trend")
        SurfaceCard {
            SegmentedChoice(
                segments = listOf(
                    Segment(BpTrendView.Chart, "Chart", "Trend chart"),
                    Segment(BpTrendView.Category, "Category", "Category view of the latest reading"),
                ),
                selected = trendView,
                onSelect = { trendView = it },
            )
            when (trendView) {
                BpTrendView.Chart -> {
                    SegmentedChoice(
                        segments = BloodPressureChartRange.entries.map { range ->
                            Segment(range, range.label, range.limit?.let { "Last $it readings" } ?: "All readings")
                        },
                        selected = chartRange,
                        onSelect = { chartRange = it },
                        unitLabel = "Readings",
                    )
                    if (trend.size > 1) {
                        BloodPressureChart(trend, store.bloodPressureReadings, store.profile.bpMovingAverageReadings)
                    } else {
                        Text("Add more readings to see a trend line.", color = colors.inkMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Average of last ${store.profile.bpMovingAverageReadings} readings",
                            color = colors.inkMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextLink("Change", onClick = onEditBpWindow, contentDescription = "Change blood pressure average window")
                    }
                }
                BpTrendView.Category -> {
                    Text("Where your latest reading falls on the AHA categories.", color = colors.inkMuted, fontSize = 13.sp)
                    BpGauge(latest.systolic, latest.diastolic)
                    BloodPressureZoneChart(latest.systolic, latest.diastolic)
                }
            }
        }

        SectionTitle("Goal")
        if (summary != null) {
            BloodPressureGoalCard(summary, onEditBpTarget)
        } else {
            EmptyState(
                "No readings in the last 7 days",
                "Add a reading to see your 7-day average and how often you are at or under target.",
                "Add reading",
                onAdd,
            )
        }

        SectionTitle("History")
        FilterBar(activeFilters, onOpen = { filterOpen = true }, onClearAll = clearFilters, openerFocus = filterFocus)
        if (filtered.isEmpty()) {
            Text("No readings match these filters.", color = colors.inkMuted)
        }
        recent.forEach { reading ->
            HistoryRow(
                primary = formatBloodPressure(reading.systolic, reading.diastolic, reading.heartRate),
                secondary = formatBloodPressureHistoryLine(reading),
                onClick = { onEdit(reading.id) },
                trailing = {
                    BpBadge(
                        bloodPressureStatus(reading.systolic, reading.diastolic),
                        compact = true,
                        systolic = reading.systolic,
                        diastolic = reading.diastolic,
                    )
                },
                overflowLabel = "Delete",
                onOverflow = { pending = reading },
                highlighted = reading.id == highlightedId,
            )
        }
        if (recent.size < filtered.size) {
            TextLink("Show older", onClick = { visibleCount += HISTORY_PAGE })
        }
    }
    if (filterOpen) {
        FilterSheet(
            resultSummary = "Showing ${filtered.size} of ${sorted.size} readings.",
            onDismiss = { filterOpen = false },
            onClearAll = clearFilters,
            returnFocus = filterFocus,
        ) {
            FilterSection("Time of day") {
                ChoiceChip("All", selected = periodFilter == null, onClick = { setPeriod(null) })
                listOf(BloodPressurePeriod.Morning, BloodPressurePeriod.Evening).forEach { period ->
                    ChoiceChip(period.label, selected = periodFilter == period, onClick = { setPeriod(period) })
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
    pending?.let { reading ->
        ConfirmDeleteDialog(
            title = "Delete this reading?",
            body = "Delete the ${formatDate(reading.date)} ${formatTime(reading.time)} reading of ${formatBloodPressureCompact(reading.systolic, reading.diastolic, reading.heartRate)}?",
            onConfirm = { onDelete(reading.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun BloodPressureGoalCard(summary: BloodPressureVisitSummary, onEditBpTarget: () -> Unit) {
    val colors = LocalAppColors.current
    SurfaceCard {
        Text("7-DAY AVERAGE", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
        Text(
            formatBloodPressure(summary.meanSystolic, summary.meanDiastolic, summary.meanHeartRate),
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        BpBadge(summary.status, systolic = summary.meanSystolic, diastolic = summary.meanDiastolic)
        Text(
            "From ${summary.count} ${if (summary.count == 1) "reading" else "readings"} in the last 7 days. AHA categories here use the average, not one reading.",
            color = colors.inkMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        val morning = summary.morning
        val evening = summary.evening
        if (morning != null && evening != null) {
            HorizontalDivider(color = colors.border)
            Text(
                "Morning ${morning.systolic} / ${morning.diastolic} · Evening ${evening.systolic} / ${evening.diastolic}",
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        HorizontalDivider(color = colors.border)
        val target = summary.target
        if (target != null) {
            val goal = formatBloodPressureTarget(target.targetSystolic, target.targetDiastolic)
            Text(
                "${target.last7Met} of ${target.last7Total} in the last 7 days at or under $goal.",
                color = colors.ink,
                fontSize = 14.sp,
            )
            Text(
                "${target.last30Met} of ${target.last30Total} in the last 30 days at or under $goal.",
                color = colors.inkMuted,
                fontSize = 13.sp,
            )
            TextLink("Change target", onClick = onEditBpTarget, contentDescription = "Change blood pressure target")
        } else {
            Text("Set the target your clinician gave you to track how often you meet it.", color = colors.inkMuted, fontSize = 13.sp)
            TextLink("Set a target", onClick = onEditBpTarget, contentDescription = "Set blood pressure target")
        }
    }
}

@Composable
fun BloodPressureFormScreen(
    existing: BloodPressureReading?,
    previous: BloodPressureReading? = null,
    missing: Boolean,
    onBack: () -> Unit,
    onHowToMeasure: () -> Unit,
    onSave: (BloodPressureSave) -> Unit,
    onFinished: (BpStatus) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    if (missing) {
        AppScreen(title = "Reading not found", lead = "That reading may have been deleted.", onBack = onBack, backLabel = "Blood pressure") {}
        return
    }
    val suggestion = previous.takeIf { existing == null }
    val raw = existing?.averagedFrom.orEmpty()
    val firstSample = raw.getOrNull(0)
    val secondSample = raw.getOrNull(1)
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: todayIso()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time ?: nowTime()) }
    var period by remember(existing?.id) {
        mutableStateOf(BloodPressurePeriod.fromId(existing?.period) ?: inferBloodPressurePeriod(existing?.time ?: nowTime()))
    }
    var periodLocked by remember(existing?.id) { mutableStateOf(existing?.period != null) }
    var systolic by remember(existing?.id) {
        mutableStateOf((firstSample?.systolic ?: existing?.systolic)?.toString().orEmpty())
    }
    var diastolic by remember(existing?.id) {
        mutableStateOf((firstSample?.diastolic ?: existing?.diastolic)?.toString().orEmpty())
    }
    var heartRate by remember(existing?.id) {
        mutableStateOf((firstSample?.heartRate ?: existing?.heartRate)?.toString().orEmpty())
    }
    var pairEnabled by remember(existing?.id) { mutableStateOf(secondSample != null) }
    var systolic2 by remember(existing?.id) { mutableStateOf(secondSample?.systolic?.toString().orEmpty()) }
    var diastolic2 by remember(existing?.id) { mutableStateOf(secondSample?.diastolic?.toString().orEmpty()) }
    var heartRate2 by remember(existing?.id) { mutableStateOf(secondSample?.heartRate?.toString().orEmpty()) }
    var waitSeconds by remember(existing?.id) { mutableIntStateOf(if (secondSample != null) 0 else 60) }
    var selectedTags by remember(existing?.id) {
        mutableStateOf(BloodPressureTag.parse(existing?.tags.orEmpty()).map { it.id }.toSet())
    }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var pending by remember { mutableStateOf<BloodPressureSave?>(null) }
    var confirmWarning by remember { mutableStateOf<String?>(null) }
    var detailsOpen by remember(existing?.id) {
        mutableStateOf(existing?.tags?.isNotEmpty() == true || existing?.note?.isNotBlank() == true)
    }
    val isEdit = existing != null
    val fields = listOf(
        date, time, period.id, systolic, diastolic, heartRate, pairEnabled.toString(),
        systolic2, diastolic2, heartRate2, selectedTags.sorted().joinToString(","), note,
    )
    val initialFields = remember(existing?.id) { fields }
    LaunchedEffect(pairEnabled, existing?.id) {
        if (!pairEnabled || secondSample != null) return@LaunchedEffect
        waitSeconds = 60
        while (waitSeconds > 0 && pairEnabled) {
            delay(1_000)
            waitSeconds -= 1
        }
    }
    val submit: () -> Unit = {
        val draft = buildBloodPressureSave(
            date = date,
            time = time,
            period = period,
            systolic = systolic,
            diastolic = diastolic,
            heartRate = heartRate,
            pairEnabled = pairEnabled,
            systolic2 = systolic2,
            diastolic2 = diastolic2,
            heartRate2 = heartRate2,
            existingPairId = existing?.pairId,
            tags = selectedTags,
            note = note,
        )
        errors = draft.second
        val next = draft.first
        if (next != null) {
            pending = next
            val warning = bloodPressureSaveWarning(next.systolic, next.diastolic)
            if (warning != null) confirmWarning = warning
            else commitReading(next, onSave, onFinished)
        }
    }
    RecordFormScaffold(
        title = if (isEdit) "Edit reading" else "Add reading",
        lead = if (isEdit) {
            "Update the date, time, and pressure. Heart rate is optional."
        } else {
            "Date and time default to now. Enter systolic over diastolic. Heart rate is optional."
        },
        isDirty = fields != initialFields,
        onClose = onBack,
        onSave = submit,
        actions = listOf(MenuAction("How to measure") { onHowToMeasure() }),
        delete = if (existing != null && onDelete != null) {
            FormDelete(
                title = "Delete this reading?",
                body = "Delete the ${formatDate(existing.date)} ${formatTime(existing.time)} reading of ${formatBloodPressureCompact(existing.systolic, existing.diastolic, existing.heartRate)}?",
                onConfirm = onDelete,
            )
        } else {
            null
        },
    ) {
        SurfaceCard {
            DateTimeFields(
                date,
                time,
                { date = it },
                {
                    time = it
                    if (!periodLocked) period = inferBloodPressurePeriod(it)
                },
                errors["date"],
                errors["time"],
            )
            Text("Time of day", color = LocalAppColors.current.inkMuted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BloodPressurePeriod.entries.forEach { option ->
                    ChoiceChip(option.label, selected = period == option, onClick = {
                        period = option
                        periodLocked = true
                    })
                }
            }
            Text(if (pairEnabled) "First reading" else "Reading", color = LocalAppColors.current.ink, fontWeight = FontWeight.SemiBold)
            if (suggestion != null) {
                LastTimeSuggestion(
                    formatPreviousBloodPressure(suggestion.systolic, suggestion.diastolic, suggestion.heartRate),
                    onUse = {
                        systolic = suggestion.systolic.toString()
                        diastolic = suggestion.diastolic.toString()
                        heartRate = suggestion.heartRate?.toString().orEmpty()
                    },
                    useDescription = "Use last reading's values",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Systolic", systolic, { systolic = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["systolic"], keyboardType = KeyboardType.Number)
                LabeledField("Diastolic", diastolic, { diastolic = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["diastolic"], keyboardType = KeyboardType.Number)
            }
            LabeledField("Heart rate (bpm)", heartRate, { heartRate = it.filter(Char::isDigit) }, error = errors["heartRate"], keyboardType = KeyboardType.Number)
            if (!pairEnabled) {
                TextLink(
                    "Add a second reading (wait 1 minute)",
                    onClick = { pairEnabled = true },
                )
            } else {
                Text(
                    if (waitSeconds > 0) "Wait 0:${waitSeconds.toString().padStart(2, '0')} before the second reading."
                    else "Ready for the second reading.",
                    color = LocalAppColors.current.inkMuted,
                    fontSize = 13.sp,
                )
                Text("Second reading", color = LocalAppColors.current.ink, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField("Systolic", systolic2, { systolic2 = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["systolic2"], keyboardType = KeyboardType.Number)
                    LabeledField("Diastolic", diastolic2, { diastolic2 = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["diastolic2"], keyboardType = KeyboardType.Number)
                }
                LabeledField("Heart rate (bpm)", heartRate2, { heartRate2 = it.filter(Char::isDigit) }, error = errors["heartRate2"], keyboardType = KeyboardType.Number)
                TextLink("Remove second reading", onClick = {
                    pairEnabled = false
                    systolic2 = ""
                    diastolic2 = ""
                    heartRate2 = ""
                })
            }
            val liveSys = systolic.toIntOrNull()
            val liveDia = diastolic.toIntOrNull()
            val liveWarning = if (liveSys != null && liveDia != null && liveDia < liveSys) {
                bloodPressureSaveHint(liveSys, liveDia)
            } else {
                null
            }
            if (liveWarning != null) {
                Text(liveWarning, color = LocalAppColors.current.danger, fontSize = 13.sp, lineHeight = 18.sp)
            }
            if (liveSys != null && liveDia != null) {
                LowBpHint(liveSys, liveDia)
            }
            AddDetails(
                expanded = detailsOpen,
                onToggle = { detailsOpen = !detailsOpen },
                label = if (selectedTags.isEmpty() && note.isBlank()) "Add details" else "Details",
            ) {
                Text("Tags", color = LocalAppColors.current.inkMuted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                TagChipRow(
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
    confirmWarning?.let { warning ->
        ConfirmDialog(
            title = "Check this reading",
            body = warning,
            confirmLabel = "Save anyway",
            dismissLabel = "Edit",
            onConfirm = {
                confirmWarning = null
                pending?.let { saved ->
                    commitReading(saved, onSave, onFinished)
                }
            },
            onDismiss = {
                confirmWarning = null
                pending = null
            },
        )
    }
}

private fun buildBloodPressureSave(
    date: String,
    time: String,
    period: BloodPressurePeriod,
    systolic: String,
    diastolic: String,
    heartRate: String,
    pairEnabled: Boolean,
    systolic2: String,
    diastolic2: String,
    heartRate2: String,
    existingPairId: String?,
    tags: Set<String>,
    note: String,
): Pair<BloodPressureSave?, Map<String, String>> {
    val savedTags = BloodPressureTag.parse(tags.toList()).map { it.id }
    val savedNote = note.trim()
    val first = validateBloodPressureReading(date, time, systolic, diastolic, heartRate)
    val errors = first.errors.toMutableMap()
    val secondBlank = systolic2.isBlank() && diastolic2.isBlank() && heartRate2.isBlank()
    if (!first.ok || first.systolic == null || first.diastolic == null) {
        return null to errors
    }
    if (!pairEnabled || secondBlank) {
        return BloodPressureSave(
            date = date,
            time = time,
            systolic = first.systolic,
            diastolic = first.diastolic,
            heartRate = first.heartRate,
            period = period.id,
            tags = savedTags,
            note = savedNote,
        ) to errors
    }
    val second = validateBloodPressureReading(date, time, systolic2, diastolic2, heartRate2)
    second.errors.forEach { (key, message) ->
        errors[if (key == "date" || key == "time") key else "${key}2"] = message
    }
    if (!second.ok || second.systolic == null || second.diastolic == null) {
        return null to errors
    }
    val samples = listOf(
        BloodPressureSample(first.systolic, first.diastolic, first.heartRate),
        BloodPressureSample(second.systolic, second.diastolic, second.heartRate),
    )
    val average = averageBloodPressureSamples(samples) ?: return null to errors
    return BloodPressureSave(
        date = date,
        time = time,
        systolic = average.systolic,
        diastolic = average.diastolic,
        heartRate = average.heartRate,
        period = period.id,
        pairId = existingPairId ?: newId("pair"),
        averagedFrom = samples,
        tags = savedTags,
        note = savedNote,
    ) to errors
}

@Composable
private fun LowBpHint(systolic: Int, diastolic: Int) {
    val hint = bloodPressureLowHint(systolic, diastolic) ?: return
    Text(hint, color = LocalAppColors.current.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipRow(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    tags: List<BloodPressureTag> = BloodPressureTag.entries,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            ChoiceChip(tag.label, selected = tag.id in selected, onClick = { onToggle(tag.id) })
        }
    }
}

private fun commitReading(
    saved: BloodPressureSave,
    onSave: (BloodPressureSave) -> Unit,
    onStatus: (BpStatus) -> Unit,
) {
    onSave(saved)
    onStatus(bloodPressureStatus(saved.systolic, saved.diastolic))
}

/** Hero content for the hub card; the Home card reuses it so both read the same. */
@Composable
fun BloodPressureHero(latest: BloodPressureReading, readings: List<BloodPressureReading>) {
    val colors = LocalAppColors.current
    val status = bloodPressureStatus(latest.systolic, latest.diastolic)
    val delta = bloodPressureDelta(readings)
    HeroHeader("Latest reading", formatBloodPressure(latest.systolic, latest.diastolic, latest.heartRate), valueSize = 28.sp)
    BpBadge(status, systolic = latest.systolic, diastolic = latest.diastolic)
    if (delta != null) {
        Text(
            formatBloodPressureDelta(delta),
            color = when {
                delta.systolic == 0 && delta.diastolic == 0 -> colors.inkMuted
                delta.systolic <= 0 && delta.diastolic <= 0 -> colors.successText
                else -> colors.accentText
            },
            fontWeight = FontWeight.Medium,
        )
    }
    Text(formatDerivedBloodPressure(latest.systolic, latest.diastolic), color = colors.inkMuted, fontSize = 13.sp)
    Text("${formatDate(latest.date)} · ${formatTime(latest.time)} · ${bloodPressurePeriod(latest).label}", color = colors.inkMuted)
    LowBpHint(latest.systolic, latest.diastolic)
    if (status == BpStatus.Severe) {
        Text(
            "Wait 1 minute and take it again. Call your health care professional if it stays this high. Call 911 if you have chest pain, shortness of breath, back pain, numbness, weakness, vision changes, or trouble speaking.",
            color = colors.danger,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}
