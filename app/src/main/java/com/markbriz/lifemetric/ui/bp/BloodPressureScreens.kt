package com.markbriz.lifemetric.ui.bp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BpBadge
import com.markbriz.lifemetric.ui.components.ChoiceChip
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.profile.shareBloodPressureReport
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

private const val HISTORY_PAGE = 30

@Composable
fun BloodPressureScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditBpWindow: () -> Unit,
    onEditBpTarget: () -> Unit,
    onHowToMeasure: () -> Unit,
    onEditReminders: () -> Unit,
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
    var periodFilter by remember { mutableStateOf<BloodPressurePeriod?>(null) }
    var tagFilters by remember { mutableStateOf(setOf<String>()) }
    var visibleCount by remember { mutableIntStateOf(HISTORY_PAGE) }
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
    var pending by remember { mutableStateOf<BloodPressureReading?>(null) }

    AppScreen(
        eyebrow = "Vitals",
        title = "Blood pressure",
        lead = "Track systolic and diastolic readings over time.",
        showFab = true,
        fabLabel = "Add reading",
        onFab = onAdd,
    ) {
        if (latest == null) {
            EmptyState(
                "No blood pressure readings yet",
                "Add a reading with date, time, systolic, and diastolic. Heart rate is optional.",
            ) {
                PrimaryButton("Add your first reading", onClick = onAdd)
                TextLink("How to measure", onHowToMeasure)
                TextLink("Reminders", onEditReminders)
            }
        } else {
            val status = bloodPressureStatus(latest.systolic, latest.diastolic)
            val delta = bloodPressureDelta(store.bloodPressureReadings)
            SurfaceCard {
                Text("MOST RECENT READING", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
                Text(formatBloodPressure(latest.systolic, latest.diastolic, latest.heartRate), color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                if (delta != null) {
                    Text(
                        formatBloodPressureDelta(delta),
                        color = when {
                            delta.systolic == 0 && delta.diastolic == 0 -> colors.inkMuted
                            delta.systolic <= 0 && delta.diastolic <= 0 -> colors.success
                            else -> colors.accent
                        },
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(formatDerivedBloodPressure(latest.systolic, latest.diastolic), color = colors.inkMuted, fontSize = 13.sp)
                BpBadge(status, systolic = latest.systolic, diastolic = latest.diastolic)
                LowBpHint(latest.systolic, latest.diastolic)
                if (status == BpStatus.Severe) {
                    Text(
                        "Wait 1 minute and take it again. Call your health care professional if it stays this high. Call 911 if you have chest pain, shortness of breath, back pain, numbness, weakness, vision changes, or trouble speaking.",
                        color = colors.danger,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
                Text("${formatDate(latest.date)} · ${formatTime(latest.time)} · ${bloodPressurePeriod(latest).label}", color = colors.inkMuted)
            }
        }
        if (summary != null) {
            SectionTitle("Visit summary")
            VisitSummaryCard(summary, onEditBpTarget)
        }
        if (store.bloodPressureReadings.isNotEmpty()) {
            SectionTitle("History")
            SurfaceCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BloodPressureChartRange.entries.forEach { range ->
                        ChoiceChip(range.label, selected = chartRange == range, onClick = { chartRange = range })
                    }
                }
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
                TextLink("How to measure", onHowToMeasure)
                TextLink("Share report", onClick = {
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
                }, contentDescription = "Share blood pressure report")
                TextLink("Reminders", onEditReminders)
            }
            SectionTitle("Recent readings")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("All", selected = periodFilter == null, onClick = {
                    periodFilter = null
                    visibleCount = HISTORY_PAGE
                })
                ChoiceChip(BloodPressurePeriod.Morning.label, selected = periodFilter == BloodPressurePeriod.Morning, onClick = {
                    periodFilter = BloodPressurePeriod.Morning
                    visibleCount = HISTORY_PAGE
                })
                ChoiceChip(BloodPressurePeriod.Evening.label, selected = periodFilter == BloodPressurePeriod.Evening, onClick = {
                    periodFilter = BloodPressurePeriod.Evening
                    visibleCount = HISTORY_PAGE
                })
            }
            if (usedTags.isNotEmpty()) {
                TagFilterRow(
                    tags = usedTags,
                    selected = tagFilters,
                    onToggle = { tagId ->
                        tagFilters = if (tagId in tagFilters) tagFilters - tagId else tagFilters + tagId
                        visibleCount = HISTORY_PAGE
                    },
                )
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
                )
            }
            if (recent.size < filtered.size) {
                TextLink("Show older", onClick = { visibleCount += HISTORY_PAGE })
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
private fun VisitSummaryCard(summary: BloodPressureVisitSummary, onEditBpTarget: () -> Unit) {
    val colors = LocalAppColors.current
    SurfaceCard {
        Text(
            formatBloodPressure(summary.meanSystolic, summary.meanDiastolic, summary.meanHeartRate),
            color = colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text("7-day average is ${summary.status.label}", color = colors.ink, fontWeight = FontWeight.SemiBold)
        BpBadge(summary.status, systolic = summary.meanSystolic, diastolic = summary.meanDiastolic)
        Text(
            "From ${summary.count} ${if (summary.count == 1) "reading" else "readings"} in the last 7 days.",
            color = colors.inkMuted,
            fontSize = 13.sp,
        )
        val morning = summary.morning
        val evening = summary.evening
        if (morning != null && evening != null) {
            Text(
                "Morning ${morning.systolic} / ${morning.diastolic} · Evening ${evening.systolic} / ${evening.diastolic}",
                color = colors.inkMuted,
                fontSize = 13.sp,
            )
        }
        val target = summary.target
        if (target != null) {
            val goal = formatBloodPressureTarget(target.targetSystolic, target.targetDiastolic)
            Text(
                "${target.last7Met} of ${target.last7Total} in the last 7 days at or under $goal.",
                color = colors.ink,
                fontSize = 13.sp,
            )
            Text(
                "${target.last30Met} of ${target.last30Total} in the last 30 days at or under $goal.",
                color = colors.inkMuted,
                fontSize = 13.sp,
            )
            TextLink("Change target", onClick = onEditBpTarget)
        } else {
            TextLink("Set a target", onClick = onEditBpTarget, contentDescription = "Set blood pressure target")
        }
        Text(
            "AHA categories on this card use the average, not one reading.",
            color = colors.inkMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
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
    onFinished: () -> Unit,
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
        mutableStateOf((firstSample?.systolic ?: existing?.systolic ?: suggestion?.systolic)?.toString().orEmpty())
    }
    var diastolic by remember(existing?.id) {
        mutableStateOf((firstSample?.diastolic ?: existing?.diastolic ?: suggestion?.diastolic)?.toString().orEmpty())
    }
    var heartRate by remember(existing?.id) {
        mutableStateOf((firstSample?.heartRate ?: existing?.heartRate ?: suggestion?.heartRate)?.toString().orEmpty())
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
    var savedStatus by remember { mutableStateOf<BpStatus?>(null) }
    val isEdit = existing != null
    val preview = savedStatus
    LaunchedEffect(pairEnabled, existing?.id) {
        if (!pairEnabled || secondSample != null) return@LaunchedEffect
        waitSeconds = 60
        while (waitSeconds > 0 && pairEnabled) {
            delay(1_000)
            waitSeconds -= 1
        }
    }
    if (preview != null && pending != null) {
        val saved = pending!!
        BackHandler { onFinished() }
        LaunchedEffect(preview) {
            delay(1_400)
            onFinished()
        }
        AppScreen(
            eyebrow = "Blood pressure",
            title = "Saved",
            lead = "This reading is ${preview.label}.",
        ) {
            SurfaceCard {
                Text(
                    formatBloodPressure(saved.systolic, saved.diastolic, saved.heartRate),
                    color = LocalAppColors.current.ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(formatDerivedBloodPressure(saved.systolic, saved.diastolic), color = LocalAppColors.current.inkMuted)
                if (saved.averagedFrom.size >= 2) {
                    Text("Average of two readings.", color = LocalAppColors.current.inkMuted, fontSize = 13.sp)
                }
                BpBadge(preview, systolic = saved.systolic, diastolic = saved.diastolic)
                LowBpHint(saved.systolic, saved.diastolic)
            }
        }
        return
    }
    AppScreen(
        eyebrow = "Blood pressure / ${if (isEdit) "Edit" else "New"}",
        title = if (isEdit) "Edit reading" else "Add reading",
        lead = if (isEdit) {
            "Update the date, time, and pressure. Heart rate is optional."
        } else {
            "Date and time default to now. Enter systolic over diastolic. Heart rate is optional."
        },
        onBack = onBack,
        backLabel = "Blood pressure",
    ) {
        SurfaceCard {
            TextLink("How to measure", onHowToMeasure)
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
            if (suggestion != null) {
                Text(
                    formatPreviousBloodPressure(suggestion.systolic, suggestion.diastolic, suggestion.heartRate),
                    color = LocalAppColors.current.inkMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
            }
            Text(if (pairEnabled) "First reading" else "Reading", color = LocalAppColors.current.ink, fontWeight = FontWeight.SemiBold)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton("Cancel", onBack, Modifier.weight(1f))
                PrimaryButton(if (isEdit) "Save changes" else "Save reading", modifier = Modifier.weight(1f), onClick = {
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
                        else commitReading(next, onSave) { savedStatus = it }
                    }
                })
            }
        }
    }
    confirmWarning?.let { warning ->
        ConfirmDeleteDialog(
            title = "Check this reading",
            body = warning,
            confirmLabel = if (isEdit) "Save changes" else "Save reading",
            onConfirm = {
                confirmWarning = null
                pending?.let { saved ->
                    commitReading(saved, onSave) { status -> savedStatus = status }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterRow(
    tags: List<BloodPressureTag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
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
