package com.markbriz.lifemetric.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.chronologicalWeights
import com.markbriz.lifemetric.domain.formatDelta
import com.markbriz.lifemetric.domain.formatRelativeDays
import com.markbriz.lifemetric.domain.formatWeight
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.recentBloodPressure
import com.markbriz.lifemetric.domain.weightDelta
import com.markbriz.lifemetric.domain.workoutSessionsByWeek
import com.markbriz.lifemetric.domain.workoutsInLastDays
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BmiGauge
import com.markbriz.lifemetric.ui.components.BpBadge
import com.markbriz.lifemetric.ui.components.BpGauge
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.StatChip
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.WeightChart
import com.markbriz.lifemetric.ui.components.WorkoutBarChart
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun HomeScreen(store: FitnessStore, onOpenBp: () -> Unit, onOpenWeight: () -> Unit, onOpenWorkouts: () -> Unit) {
    val colors = LocalAppColors.current
    val latestBp = latestBloodPressure(store.bloodPressureReadings)
    val latestWeight = latestWeight(store.weightRecords)
    val delta = weightDelta(store.weightRecords)
    val sessions = workoutsInLastDays(store.workouts, 30)
    val sets = sessions.sumOf { it.entries.size }
    val lastWorkout = store.workouts.maxByOrNull { it.date }
    val lastPlan = store.plans.find { it.id == lastWorkout?.planId }?.name
    val bpTrend = recentBloodPressure(store.bloodPressureReadings, 10)
    val weightTrend = chronologicalWeights(store.weightRecords).takeLast(10)
    val bmi = if (latestWeight != null && store.profile.heightInches != null) {
        calculateBmi(latestWeight.weight, store.profile.heightInches)
    } else Double.NaN
    val goalBmi = if (store.profile.heightInches != null && store.profile.weightGoalLb != null) {
        calculateBmi(store.profile.weightGoalLb, store.profile.heightInches)
    } else Double.NaN
    val weeks = workoutSessionsByWeek(store.workouts, 8)

    AppScreen(title = "Dashboard", lead = "Blood pressure, weight, and workouts at a glance.") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Latest BP", latestBp?.let { "${it.systolic}/${it.diastolic}" } ?: "—", latestBp?.let { "${it.heartRate} bpm" } ?: "No readings", Modifier.weight(1f))
            StatChip(
                "Latest weight",
                latestWeight?.let { "${formatWeight(it.weight)} lb" } ?: "—",
                delta?.let { formatDelta(it, "lb") } ?: latestWeight?.let { formatDate(it.date) } ?: "No readings",
                Modifier.weight(1f),
            )
            StatChip("Workouts (30d)", sessions.size.toString(), formatRelativeDays(lastWorkout?.date), Modifier.weight(1f))
        }
        DashSection("Blood pressure", "Gauge + systolic/diastolic trend", onOpenBp, Icons.Default.MonitorHeart) {
            if (latestBp == null) {
                EmptyState("No BP readings", "Record systolic, diastolic, and heart rate to populate the gauge and chart.")
            } else {
                val status = bloodPressureStatus(latestBp.systolic, latestBp.diastolic)
                BpGauge(latestBp.systolic, latestBp.diastolic)
                BpBadge(status)
                Text("${formatDate(latestBp.date)} · ${formatTime(latestBp.time)}", color = colors.inkMuted, fontSize = 13.sp)
                Text("Heart rate ${latestBp.heartRate} bpm", color = colors.inkMuted, fontSize = 13.sp)
                if (bpTrend.size > 1) {
                    Text("Recent readings trend", color = colors.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    BloodPressureChart(bpTrend, store.bloodPressureReadings, store.profile.bpMovingAverageReadings)
                } else {
                    Text("Add more readings to see a trend line.", color = colors.inkMuted, fontSize = 13.sp)
                }
            }
        }
        DashSection("Weight", "Trend line + BMI gauge", onOpenWeight, Icons.Default.Scale) {
            if (latestWeight == null) {
                EmptyState("No weight readings", "Log scale readings to see your trend chart and BMI gauge.")
            } else {
                Text("${formatWeight(latestWeight.weight)} lb", color = colors.ink, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                if (delta != null) {
                    Text(
                        "${formatDelta(delta, "lb")} vs prior",
                        color = if (delta <= 0) colors.success else colors.accent,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text("Weight history", color = colors.inkMuted, fontSize = 13.sp)
                WeightChart(weightTrend, store.profile.weightGoalLb)
                if (!bmi.isNaN()) {
                    Text("BMI gauge", color = colors.inkMuted, fontSize = 13.sp)
                    BmiGauge(bmi, goalBmi, compact = true)
                } else {
                    Text("Set height on the Profile tab to enable the BMI gauge on this dashboard.", color = colors.inkMuted, fontSize = 13.sp)
                }
            }
        }
        DashSection("Workouts", "Sessions per week · bar chart", onOpenWorkouts, Icons.Default.FitnessCenter) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Sessions (30d)", sessions.size.toString(), null, Modifier.weight(1f))
                StatChip("Sets logged (30d)", sets.toString(), null, Modifier.weight(1f))
                StatChip("Last session", lastWorkout?.let { formatRelativeDays(it.date) } ?: "—", lastPlan, Modifier.weight(1f))
            }
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
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    SurfaceCard(Modifier.clickable(onClick = onClick)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = colors.brand)
            Column {
                Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = colors.inkMuted, fontSize = 13.sp)
            }
        }
        content()
    }
}
