package com.markbriz.lifemetric.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.markbriz.lifemetric.MainActivity
import com.markbriz.lifemetric.data.LocalRepository
import com.markbriz.lifemetric.data.UserProfile
import com.markbriz.lifemetric.domain.DEFAULT_WORKOUT_REMINDER

const val WORKOUT_REMINDER_ACTION = "com.markbriz.lifemetric.WORKOUT_REMINDER"
private const val CHANNEL_ID = "workout_reminders"
private const val MORNING_REQUEST = 1301
private const val MORNING_NOTICE = 2301

object WorkoutReminders {
    fun sync(context: Context, profile: UserProfile) {
        schedule(
            context,
            profile.workoutMorningReminder,
            profile.workoutMorningReminderTime.ifBlank { DEFAULT_WORKOUT_REMINDER },
        )
    }

    fun schedule(context: Context, enabled: Boolean, time: String) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        ensureChannel(context)
        val pending = pendingIntent(context)
        if (!enabled) {
            alarms.cancel(pending)
            pending.cancel()
            return
        }
        val trigger = nextTriggerMillis(time) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            action = WORKOUT_REMINDER_ACTION
        }
        return PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Workout reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Local reminders to log today's session."
            },
        )
    }
}

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val profile = LocalRepository(context).load().store.profile
            WorkoutReminders.sync(context, profile)
            return
        }
        if (intent.action != WORKOUT_REMINDER_ACTION) return
        notify(context)
        val profile = LocalRepository(context).load().store.profile
        WorkoutReminders.sync(context, profile)
    }

    private fun notify(context: Context) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to log a workout")
            .setContentText("Log today's session.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(MORNING_NOTICE, notification)
        }
    }
}
