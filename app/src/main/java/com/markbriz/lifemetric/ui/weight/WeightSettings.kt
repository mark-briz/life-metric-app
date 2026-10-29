package com.markbriz.lifemetric.ui.weight

import androidx.compose.runtime.Composable
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.formatHeight
import com.markbriz.lifemetric.domain.formatWaist
import com.markbriz.lifemetric.domain.formatWeightReminder
import com.markbriz.lifemetric.domain.formatWeightWithUnit
import com.markbriz.lifemetric.domain.weightUnit
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SettingsRow

@Composable
fun WeightSettingsScreen(
    store: FitnessStore,
    onBack: () -> Unit,
    backLabel: String = "Weight",
    onGoal: () -> Unit,
    onReminders: () -> Unit,
    onUnits: () -> Unit,
    onHeight: () -> Unit,
    onWaist: () -> Unit,
) {
    val profile = store.profile
    val useKg = profile.weightDisplayKg
    val start = store.weightRecords.find { it.id == profile.weightGoalStartId }
    val goal = profile.weightGoalLb?.let { formatWeightWithUnit(it, useKg) }
    AppScreen(
        title = "Weight settings",
        lead = "Goal, reminders, units, and body measurements.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SettingsRow(
            "Weight goal",
            "Target weight and the scale reading it starts from.",
            onGoal,
            when {
                goal != null && start != null -> "$goal from ${formatWeightWithUnit(start.weight, useKg)}"
                goal != null -> goal
                else -> "Not set"
            },
        )
        SettingsRow(
            "Reminders",
            "Optional local morning reminder to log a scale reading.",
            onReminders,
            formatWeightReminder(profile.weightMorningReminder, profile.weightMorningReminderTime),
        )
        SettingsRow(
            "Units",
            "How lift and body weight are shown. Stored values stay in pounds.",
            onUnits,
            weightUnit(useKg),
        )
        SettingsRow("Height", "Used to calculate BMI on the Weight tab.", onHeight, formatHeight(profile.heightInches))
        SettingsRow(
            "Waist",
            "Optional NHLBI waist comparison. Sex is not recorded.",
            onWaist,
            formatWaist(profile.waistInches),
        )
    }
}
