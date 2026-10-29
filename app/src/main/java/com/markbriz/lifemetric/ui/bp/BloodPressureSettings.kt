package com.markbriz.lifemetric.ui.bp

import androidx.compose.runtime.Composable
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.formatBloodPressureReminders
import com.markbriz.lifemetric.domain.formatBloodPressureTarget
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SettingsRow

@Composable
fun BloodPressureSettingsScreen(
    store: FitnessStore,
    onBack: () -> Unit,
    backLabel: String = "Blood pressure",
    onTarget: () -> Unit,
    onWindow: () -> Unit,
    onReminders: () -> Unit,
) {
    val profile = store.profile
    AppScreen(
        title = "Blood pressure settings",
        lead = "Target, chart average, and reminders for blood pressure.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SettingsRow(
            "BP target",
            "The numbers your clinician asked you to stay at or under.",
            onTarget,
            formatBloodPressureTarget(profile.bpTargetSystolic, profile.bpTargetDiastolic),
        )
        SettingsRow(
            "Average window",
            "How many recent readings the blood pressure chart averages.",
            onWindow,
            "${profile.bpMovingAverageReadings}",
        )
        SettingsRow(
            "Reminders",
            "Optional local morning and evening measurement reminders.",
            onReminders,
            formatBloodPressureReminders(
                profile.bpMorningReminder,
                profile.bpEveningReminder,
                profile.bpMorningReminderTime,
                profile.bpEveningReminderTime,
            ),
        )
    }
}
