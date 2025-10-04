package com.jinjinmory.wuwatracking.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.util.concurrent.TimeUnit

object BackgroundRefreshScheduler {

    private const val REQUEST_CODE = 2001
    private val INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(6)

    fun scheduleNext(context: Context, anchorTimeMillis: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = calculateNextTrigger(anchorTimeMillis)
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

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(appContext))
    }

    private fun calculateNextTrigger(anchorTimeMillis: Long): Long {
        val slotIndex = (anchorTimeMillis / INTERVAL_MILLIS) + 1
        return slotIndex * INTERVAL_MILLIS
    }

    private fun pendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            ProfileRefreshReceiver.intent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
