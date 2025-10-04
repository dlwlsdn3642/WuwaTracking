package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object NotificationSettingsManager {

    private const val PREF_FILE_NAME = "wuwa_notification_settings"
    private const val KEY_WAVEPLATE_THRESHOLD = "waveplate_threshold"
    private const val KEY_LAST_FULL_ALERT_COUNT = "last_full_alert_count"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUE = "last_threshold_alert_value"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun saveWaveplateThreshold(context: Context, threshold: Int?) {
        prefs(context).edit(commit = true) {
            if (threshold == null || threshold <= 0) {
                remove(KEY_WAVEPLATE_THRESHOLD)
            } else {
                putInt(KEY_WAVEPLATE_THRESHOLD, threshold)
            }
        }
    }

    fun getWaveplateThreshold(context: Context): Int? {
        val stored = prefs(context).getInt(KEY_WAVEPLATE_THRESHOLD, Int.MIN_VALUE)
        return if (stored == Int.MIN_VALUE) null else stored
    }

    fun getLastFullAlertCount(context: Context): Int? {
        val stored = prefs(context).getInt(KEY_LAST_FULL_ALERT_COUNT, Int.MIN_VALUE)
        return if (stored == Int.MIN_VALUE) null else stored
    }

    fun markFullAlertSent(context: Context, count: Int) {
        prefs(context).edit(commit = true) {
            putInt(KEY_LAST_FULL_ALERT_COUNT, count)
        }
    }

    fun clearFullAlert(context: Context) {
        prefs(context).edit(commit = true) {
            remove(KEY_LAST_FULL_ALERT_COUNT)
        }
    }

    fun getLastThresholdAlertValue(context: Context): Int? {
        val stored = prefs(context).getInt(KEY_LAST_THRESHOLD_ALERT_VALUE, Int.MIN_VALUE)
        return if (stored == Int.MIN_VALUE) null else stored
    }

    fun markThresholdAlertSent(context: Context, threshold: Int) {
        prefs(context).edit(commit = true) {
            putInt(KEY_LAST_THRESHOLD_ALERT_VALUE, threshold)
        }
    }

    fun clearThresholdAlert(context: Context) {
        prefs(context).edit(commit = true) {
            remove(KEY_LAST_THRESHOLD_ALERT_VALUE)
        }
    }
}
