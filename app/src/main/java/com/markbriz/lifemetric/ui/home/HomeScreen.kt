package com.markbriz.lifemetric.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.bloodPressureDelta
import com.markbriz.lifemetric.domain.bloodPressureLowHint
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.bloodPressureVisitSummary
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.formatBloodPressureDelta
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatDerivedBloodPressure
import com.markbriz.lifemetric.domain.formatRelativeDays
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.formatVisitSummaryLine
import com.markbriz.lifemetric.domain.formatWeightTrendDelta
import com.markbriz.lifemetric.domain.formatWeightWithUnit
import com.markbriz.lifemetric.domain.formatWorkoutChipLine
import com.markbriz.lifemetric.domain.formatWorkoutWeekLine
import com.markbriz.lifemetric.domain.latestWorkout
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.latestWeightTrend
import com.markbriz.lifemetric.domain.recentBloodPressure
import com.markbriz.lifemetric.domain.homeWeightChartModel
import com.markbriz.lifemetric.domain.weightTrendDelta
import com.markbriz.lifemetric.domain.workoutSessionsByWeek
import com.markbriz.lifemetric.domain.workoutWeekSummary
import com.markbriz.lifemetric.domain.workoutsInLastDays
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BloodPressureZoneChart
import com.markbriz.lifemetric.ui.components.BmiGauge
import com.markbriz.lifemetric.ui.components.BpBadge
import com.markbriz.lifemetric.ui.components.BpGauge
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.StatChip
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.components.WeightChart
import com.markbriz.lifemetric.ui.components.WorkoutBarChart
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun HomeScreen(
    store: FitnessStore,
    onOpenBp: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onAddBp: () -> Unit,
    onAddWeight: () -> Unit,
    onAddWorkout: () -> Unit,
) {
    val colors = LocalAppColors.current
    val latestBp = latestBloodPressure(store.bloodPressureReadings)
    val useKg = store.profile.weightDisplayKg
    val latestWeight = latestWeight(store.weightRecords)
    val bpDelta = bloodPressureDelta(store.bloodPressureReadings)
    val trend = latestWeightTrend(store.weightRecords)
    val trendDelta = weightTrendDelta(store.weightRecords)
    val showWeightTrend = latestWeight != null && store.weightRecords.size >= 2 && trend != null
    val sessions = workoutsInLastDays(store.workouts, 30)
    val sets = sessions.sumOf { it.entries.size }
    val lastWorkout = latestWorkout(store.workouts)
    val lastPlan = store.plans.find { it.id == lastWorkout?.planId }
    val bpTrend = recentBloodPressure(store.bloodPressureReadings, 10)
    val bpSummary = bloodPressureVisitSummary(
        store.bloodPressureReadings,
        store.profile.bpTargetSystolic,
        store.profile.bpTargetDiastolic,
    )
    val weightChart = homeWeightChartModel(store.weightRecords)
    val bmi = if (latestWeight != null && store.profile.heightInches != null) {
        calculateBmi(latestWeight.weight, store.profile.heightInches)
    } else Double.NaN
    val goalBmi = if (store.profile.heightInches != null && store.profile.weightGoalLb != null) {
        calculateBmi(store.profile.weightGoalLb, store.profile.heightInches)
    } else Double.NaN
    val weeks = workoutSessionsByWeek(store.workouts, 8)
    val week = workoutWeekSummary(store.workouts, store.plans)
    val firstSession = store.bloodPressureReadings.isEmpty() &&
        store.weightRecords.isEmpty() &&
        store.workouts.isEmpty()

    AppScreen(title = "Dashboard", lead = "Blood pressure, weight, and workouts at a glance.") {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip(
                "Latest BP",
                latestBp?.let { "${it.systolic}/${it.diastolic}" } ?: "—",
                latestBp?.let { reading ->
                    reading.heartRate?.let { "$it bpm" } ?: formatDerivedBloodPressure(reading.systolic, reading.diastolic)
                } ?: "No readings",
                Modifier.weight(1f),
            )
            StatChip(
                if (showWeightTrend) "7-Day weight trend" else "Latest weight",
                when {
                    showWeightTrend -> formatWeightWithUnit(trend!!, useKg)
                    latestWeight != null -> formatWeightWithUnit(latestWeight.weight, useKg)
                    else -> "—"
                },
                when {
                    trendDelta != null -> formatWeightTrendDelta(trendDelta, useKg, compact = true)
                    latestWeight != null -> formatDate(latestWeight.date)
                    else -> "No readings"
                },
                Modifier.weight(1f),
            )
            StatChip(
                if (lastWorkout != null) "Last workout" else "Workouts (30d)",
                lastWorkout?.let { formatWorkoutChipLine(it, lastPlan, useKg) } ?: "—",
                if (sessions.isNotEmpty()) "${sessions.size} in 30 days" else "No sessions",
                Modifier.weight(1f),
            )
        }
        if (firstSession) {
            SectionTitle("Get started")
            SurfaceCard {
                Text("Record your first readings and a workout so the dashboard can show trends.", color = colors.inkMuted, fontSize = 14.sp, lineHeight = 20.sp)
            }
            SettingsRow("Record blood pressure", "Systolic and diastolic. Heart rate is optional.", onAddBp)
            SettingsRow("Record weight", "Unlocks BMI after you set height.", onAddWeight)
            SettingsRow("Log a workout", "Starter plans are ready.", onAddWorkout)
        }
        DashSection("Blood pressure", "Trend and category chart", onOpenBp, Icons.Default.MonitorHeart, onAddBp, "Add reading") {
            if (latestBp == null) {
                EmptyState("No BP readings", "Record systolic and diastolic to populate the gauge and chart.")
            } else {
                val status = bloodPressureStatus(latestBp.systolic, latestBp.diastolic)
                BpGauge(latestBp.systolic, latestBp.diastolic)
                BpBadge(status, systolic = latestBp.systolic, diastolic = latestBp.diastolic)
                bloodPressureLowHint(latestBp.systolic, latestBp.diastolic)?.let { hint ->
                    Text(hint, color = colors.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
                }
                if (bpDelta != null) {
                    Text(
                        formatBloodPressureDelta(bpDelta),
                        color = when {
                            bpDelta.systolic == 0 && bpDelta.diastolic == 0 -> colors.inkMuted
                            bpDelta.systolic <= 0 && bpDelta.diastolic <= 0 -> colors.success
                            else -> colors.accent
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
                Text(formatDerivedBloodPressure(latestBp.systolic, latestBp.diastolic), color = colors.inkMuted, fontSize = 13.sp)
                Text("${formatDate(latestBp.date)} · ${formatTime(latestBp.time)}", color = colors.inkMuted, fontSize = 13.sp)
                latestBp.heartRate?.let { rate ->
                    Text("Heart rate $rate bpm", color = colors.inkMuted, fontSize = 13.sp)
                }
                if (bpSummary != null) {
                    Text(formatVisitSummaryLine(bpSummary), color = colors.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("AHA categories on this average use the last 7 days, not one reading.", color = colors.inkMuted, fontSize = 12.sp)
                }
                if (bpTrend.size > 1) {
                    Text("Recent readings trend", color = colors.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    BloodPressureChart(bpTrend, store.bloodPressureReadings, store.profile.bpMovingAverageReadings)
                } else {
                    Text("Add more readings to see a trend line.", color = colors.inkMuted, fontSize = 13.sp)
                }
                BloodPressureZoneChart(latestBp.systolic, latestBp.diastolic)
            }
        }
        DashSection("Weight", "Trend line + BMI gauge", onOpenWeight, Icons.Default.Scale, onAddWeight, "Record weight") {
            if (latestWeight == null) {
                EmptyState("No weight readings", "Record scale readings to see your trend chart and BMI gauge.")
            } else {
                if (showWeightTrend) {
                    Text(formatWeightWithUnit(trend ?: latestWeight.weight, useKg), color = colors.ink, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text("last ${formatWeightWithUnit(latestWeight.weight, useKg)}", color = colors.inkMuted, fontSize = 13.sp)
                    if (trendDelta != null) {
                        Text(
                            formatWeightTrendDelta(trendDelta, useKg),
                            color = if (trendDelta <= 0) colors.success else colors.accent,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Text(formatWeightWithUnit(latestWeight.weight, useKg), color = colors.ink, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Weight history", color = colors.inkMuted, fontSize = 13.sp)
                if (weightChart != null && weightChart.points.isNotEmpty()) {
                    WeightChart(weightChart, store.profile.weightGoalLb, useKg)
                } else {
                    Text("Add another day to see a time-scaled trend.", color = colors.inkMuted, fontSize = 13.sp)
                }
                if (!bmi.isNaN()) {
                    Text("BMI gauge", color = colors.inkMuted, fontSize = 13.sp)
                    BmiGauge(bmi, goalBmi, compact = true)
                } else {
                    Text("Set height on the Profile tab to enable the BMI gauge on this dashboard.", color = colors.inkMuted, fontSize = 13.sp)
                }
            }
        }
        DashSection("Workouts", "This week · last session", onOpenWorkouts, Icons.Default.FitnessCenter, onAddWorkout, "Log workout") {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatChip("Sessions (30d)", sessions.size.toString(), null, Modifier.weight(1f))
                StatChip("Sets logged (30d)", sets.toString(), null, Modifier.weight(1f))
                StatChip("Last session", lastWorkout?.let { formatRelativeDays(it.date) } ?: "—", lastPlan?.name, Modifier.weight(1f))
            }
            Text(formatWorkoutWeekLine(week), color = colors.inkMuted, fontSize = 13.sp)
            if (weeks.any { it.count > 0 }) {
                Text("Weekly session count (last 8 weeks)", color = colors.inkMuted, fontSize = 13.sp)
                WorkoutBarChart(weeks)
            } else {
                EmptyState("No workouts yet", "Log a session to see weekly activity bars and training consistency.")
            }
        }
    }
}

@Composable
private fun DashSection(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onLog: () -> Unit,
    logDescription: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    SurfaceCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = title, tint = colors.brand)
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = colors.inkMuted, fontSize = 13.sp)
            }
            TextLink("Log", onClick = onLog, contentDescription = logDescription)
        }
        content()
    }
}
