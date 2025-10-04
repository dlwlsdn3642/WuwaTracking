package com.jinjinmory.wuwatracking.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jinjinmory.wuwatracking.MainActivity
import com.jinjinmory.wuwatracking.R
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile

object NotificationHelper {

    private const val CHANNEL_ID = "waveplate_alerts"

    private const val NOTIFICATION_ID_FULL = 1001
    private const val NOTIFICATION_ID_THRESHOLD = 1002

    private const val CONTENT_REQUEST_CODE = 4001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notifyWaveplatesFull(context: Context, profile: WuwaProfile) {
        notifyWaveplatesFull(
            context = context,
            name = profile.name,
            current = profile.waveplatesCurrent,
            max = profile.waveplatesMax.takeIf { it > 0 } ?: 240
        )
    }

    fun notifyWaveplatesFull(context: Context, name: String, current: Int, max: Int) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val builder = baseBuilder(
            context = context,
            title = context.getString(R.string.notification_full_title),
            content = context.getString(
                R.string.notification_full_body,
                current,
                max,
                name
            )
        )
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FULL, builder.build())
    }

    fun notifyWaveplatesThreshold(context: Context, threshold: Int, profile: WuwaProfile) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val builder = baseBuilder(
            context = context,
            title = context.getString(R.string.notification_threshold_title, threshold),
            content = context.getString(
                R.string.notification_threshold_body,
                profile.waveplatesCurrent,
                profile.name
            )
        )
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_THRESHOLD, builder.build())
    }

    private fun baseBuilder(context: Context, title: String, content: String): NotificationCompat.Builder {
        val accentColor = ContextCompat.getColor(context, R.color.purple_200)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_waveplate)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setColor(accentColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createContentIntent(context))
    }

    private fun createContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            CONTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
