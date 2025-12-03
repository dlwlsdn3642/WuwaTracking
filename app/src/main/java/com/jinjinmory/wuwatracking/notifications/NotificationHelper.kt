package com.jinjinmory.wuwatracking.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jinjinmory.wuwatracking.MainActivity
import com.jinjinmory.wuwatracking.R
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import com.jinjinmory.wuwatracking.domain.AlertResource

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "waveplate_alerts"

    private const val NOTIFICATION_ID_FULL = 1001
    private const val NOTIFICATION_ID_THRESHOLD_BASE = 2000
    private const val NOTIFICATION_ID_THRESHOLD_MULTIPLIER = 10000

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
        val waveplateName = context.getString(R.string.proper_waveplates)
        val builder = baseBuilder(
            context = context,
            title = context.getString(R.string.notification_full_title, waveplateName),
            content = context.getString(
                R.string.notification_full_body,
                waveplateName,
                current,
                max,
                name
            )
        )
        notifySafely(context, NOTIFICATION_ID_FULL, builder)
    }

    fun notifyWaveplatesThreshold(context: Context, threshold: Int, profile: WuwaProfile) {
        val waveplateName = context.getString(R.string.proper_waveplates)
        notifyResourceThreshold(
            context = context,
            resource = AlertResource.WAVEPLATES,
            resourceName = waveplateName,
            threshold = threshold,
            current = profile.waveplatesCurrent,
            profileName = profile.name
        )
    }

    fun notifyResourceThreshold(
        context: Context,
        resource: AlertResource,
        resourceName: String,
        threshold: Int,
        current: Int,
        profileName: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val builder = baseBuilder(
            context = context,
            title = context.getString(R.string.notification_threshold_title, resourceName, threshold),
            content = context.getString(
                R.string.notification_threshold_body,
                resourceName,
                profileName,
                current
            )
        )
        val notificationId = NOTIFICATION_ID_THRESHOLD_BASE +
            (resource.ordinal * NOTIFICATION_ID_THRESHOLD_MULTIPLIER) +
            threshold
        notifySafely(context, notificationId, builder)
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

    private fun notifySafely(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(notificationId, builder.build())
        } catch (security: SecurityException) {
            Log.w(TAG, "Unable to post notification. Permission missing?", security)
        }
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
