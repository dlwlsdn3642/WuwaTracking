package com.jinjinmory.wuwatracking.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager
import java.util.Calendar

object ActivityReminderScheduler {

    private const val REQUEST_CODE = 3001

    fun schedule(context: Context, config: AppPreferencesManager.ActivityReminderConfig) {
        if (!config.enabled || config.hour == null || config.minute == null) {
            cancel(context)
            return
        }
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = calculateNextTrigger(config.hour, config.minute)
        val pendingIntent = pendingIntent(appContext)
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun rescheduleIfNeeded(context: Context) {
        val config = AppPreferencesManager.getActivityReminder(context)
        schedule(context, config)
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(appContext))
    }

    private fun calculateNextTrigger(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            ActivityReminderReceiver.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
