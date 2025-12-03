package com.jinjinmory.wuwatracking.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.jinjinmory.wuwatracking.MainActivity
import com.jinjinmory.wuwatracking.R
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WuwaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WuwaWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (appWidgetId in ids) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_wuwa)
            val profile = ProfileCacheManager.getProfile(context)

            if (profile != null) {
                val max = profile.waveplatesMax.coerceAtLeast(1)
                val current = profile.waveplatesCurrent.coerceIn(0, max)
                views.setViewVisibility(R.id.widget_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_placeholder, View.GONE)
                views.setTextViewText(R.id.widget_name, profile.name)
                views.setTextViewText(
                    R.id.widget_waveplates_value,
                    context.getString(R.string.format_resource_progress, current, max)
                )
                views.setProgressBar(R.id.widget_waveplates_progress, max, current, false)
                views.setTextViewText(
                    R.id.widget_resonance,
                    context.getString(R.string.widget_label_resonance, profile.resonanceLevel)
                )
                // Reserve and time to full in one line: "Reserve X | Full in Y min"
                val remaining = (max - current).coerceAtLeast(0)
                val minutesToFull = remaining * 6
                val timeFormat = AppPreferencesManager.getWidgetTimeFormat(context)
                val timeText = formatRemainingText(context, minutesToFull, timeFormat)
                views.setTextViewText(
                    R.id.widget_subtext,
                    context.getString(
                        R.string.widget_label_waveplates_detail,
                        profile.wavesubstance,
                        timeText
                    )
                )
            } else {
                views.setViewVisibility(R.id.widget_content, View.GONE)
                views.setViewVisibility(R.id.widget_placeholder, View.VISIBLE)
            }

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatRemainingText(
            context: Context,
            minutesToFull: Int,
            format: AppPreferencesManager.WidgetTimeFormat
        ): String {
            return when (format) {
                AppPreferencesManager.WidgetTimeFormat.MINUTES ->
                    context.getString(R.string.widget_time_left_minutes, minutesToFull)
                AppPreferencesManager.WidgetTimeFormat.HOURS_MINUTES -> {
                    val hours = minutesToFull / 60
                    val minutes = minutesToFull % 60
                    context.getString(R.string.widget_time_left_hours_minutes, hours, minutes)
                }
                AppPreferencesManager.WidgetTimeFormat.ETA -> {
                    val target = LocalDateTime.now().plusMinutes(minutesToFull.toLong())
                    val pattern = context.getString(R.string.widget_time_left_eta_pattern)
                    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                    val formatted = runCatching { target.format(formatter) }.getOrElse { "" }
                    context.getString(R.string.widget_time_left_eta, formatted)
                }
            }
        }
    }
}
