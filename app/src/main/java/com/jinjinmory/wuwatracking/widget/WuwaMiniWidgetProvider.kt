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
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager

class WuwaMiniWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, WuwaMiniWidgetProvider::class.java)
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
            val views = RemoteViews(context.packageName, R.layout.widget_wuwa_mini)
            val profile = ProfileCacheManager.getProfile(context)

            if (profile != null) {
                val max = profile.waveplatesMax.coerceAtLeast(1)
                val current = profile.waveplatesCurrent.coerceIn(0, max)
                val remaining = (max - current).coerceAtLeast(0)
                val minutesToFull = remaining * 6

                views.setViewVisibility(R.id.widget_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_placeholder, View.GONE)

                views.setTextViewText(
                    R.id.widget_waveplates_value,
                    context.getString(R.string.format_resource_progress, current, max)
                )
                views.setTextViewText(
                    R.id.widget_subtext,
                    context.getString(
                        R.string.widget_label_waveplates_detail_mini,
                        profile.wavesubstance,
                        minutesToFull
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
    }
}

