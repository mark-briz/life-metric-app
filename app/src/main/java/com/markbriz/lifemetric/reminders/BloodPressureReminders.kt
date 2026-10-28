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
import com.markbriz.lifemetric.domain.DEFAULT_BP_EVENING_REMINDER
import com.markbriz.lifemetric.domain.DEFAULT_BP_MORNING_REMINDER
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

const val BP_REMINDER_ACTION = "com.markbriz.lifemetric.BP_REMINDER"
const val BP_REMINDER_PERIOD = "period"
private const val CHANNEL_ID = "bp_reminders"
private const val MORNING_REQUEST = 1101
private const val EVENING_REQUEST = 1102
private const val MORNING_NOTICE = 2101
private const val EVENING_NOTICE = 2102

object BloodPressureReminders {
    fun sync(context: Context, profile: UserProfile) {
        schedule(
            context,
            profile.bpMorningReminder,
            profile.bpEveningReminder,
            profile.bpMorningReminderTime.ifBlank { DEFAULT_BP_MORNING_REMINDER },
            profile.bpEveningReminderTime.ifBlank { DEFAULT_BP_EVENING_REMINDER },
        )
    }

    fun schedule(
        context: Context,
        morning: Boolean,
        evening: Boolean,
        morningTime: String,
        eveningTime: String,
    ) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        ensureChannel(context)
        setOrCancel(context, alarms, morning, morningTime, "morning", MORNING_REQUEST)
        setOrCancel(context, alarms, evening, eveningTime, "evening", EVENING_REQUEST)
    }

    private fun setOrCancel(
        context: Context,
        alarms: AlarmManager,
        enabled: Boolean,
        time: String,
        period: String,
        requestCode: Int,
    ) {
        val pending = pendingIntent(context, period, requestCode)
        if (!enabled) {
            alarms.cancel(pending)
            pending.cancel()
            return
        }
        val trigger = nextTriggerMillis(time) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
    }

    private fun pendingIntent(context: Context, period: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, BloodPressureReminderReceiver::class.java).apply {
            action = BP_REMINDER_ACTION
            putExtra(BP_REMINDER_PERIOD, period)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
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
                "Blood pressure reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Local reminders to log a home blood pressure reading."
            },
        )
    }
}

class BloodPressureReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val profile = LocalRepository(context).load().store.profile
            BloodPressureReminders.sync(context, profile)
            return
        }
        if (intent.action != BP_REMINDER_ACTION) return
        val period = intent.getStringExtra(BP_REMINDER_PERIOD) ?: return
        notify(context, period)
        val profile = LocalRepository(context).load().store.profile
        BloodPressureReminders.sync(context, profile)
    }

    private fun notify(context: Context, period: String) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val whenLabel = if (period == "evening") "evening" else "morning"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Time to log blood pressure")
            .setContentText("Record your $whenLabel reading.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(
                if (period == "evening") EVENING_NOTICE else MORNING_NOTICE,
                notification,
            )
        }
    }
}

fun nextTriggerMillis(time: String, now: LocalDateTime = LocalDateTime.now()): Long? {
    val clock = runCatching { LocalTime.parse(time) }.getOrNull() ?: return null
    var next = now.toLocalDate().atTime(clock)
    if (!next.isAfter(now.plusSeconds(15))) next = next.plusDays(1)
    return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
