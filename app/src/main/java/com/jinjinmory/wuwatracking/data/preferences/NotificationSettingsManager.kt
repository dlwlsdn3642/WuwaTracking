package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object NotificationSettingsManager {

    private const val PREF_FILE_NAME = "wuwa_notification_settings"
    private const val KEY_WAVEPLATE_THRESHOLD = "waveplate_threshold"
    private const val KEY_WAVEPLATE_THRESHOLDS = "waveplate_thresholds"
    private const val KEY_LAST_FULL_ALERT_COUNT = "last_full_alert_count"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUE = "last_threshold_alert_value"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUES = "last_threshold_alert_values"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    private fun sanitizeThresholds(thresholds: Collection<Int>): List<Int> =
        thresholds
            .mapNotNull { value ->
                value.takeIf { it in 1..240 }
            }
            .distinct()
            .sorted()

    private fun encodeThresholds(values: List<Int>): String =
        values.joinToString(separator = ",") { it.toString() }

    private fun decodeThresholds(raw: String?): List<Int> =
        raw
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 1..240 }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    fun saveWaveplateThresholds(context: Context, thresholds: Collection<Int>) {
        val sanitized = sanitizeThresholds(thresholds)
        prefs(context).edit(commit = true) {
            if (sanitized.isEmpty()) {
                remove(KEY_WAVEPLATE_THRESHOLDS)
            } else {
                putString(KEY_WAVEPLATE_THRESHOLDS, encodeThresholds(sanitized))
            }
            remove(KEY_WAVEPLATE_THRESHOLD)
        }
    }

    fun getWaveplateThresholds(context: Context): List<Int> {
        val preferences = prefs(context)
        val stored = preferences.getString(KEY_WAVEPLATE_THRESHOLDS, null)
        val decoded = decodeThresholds(stored)
        if (decoded.isNotEmpty()) {
            return decoded
        }
        val legacy = preferences.getInt(KEY_WAVEPLATE_THRESHOLD, Int.MIN_VALUE)
        return if (legacy == Int.MIN_VALUE || legacy <= 0) {
            emptyList()
        } else {
            listOf(legacy)
        }
    }

    fun saveWaveplateThreshold(context: Context, threshold: Int?) {
        val thresholds = threshold?.let { listOf(it) } ?: emptyList()
        saveWaveplateThresholds(context, thresholds)
    }

    fun getWaveplateThreshold(context: Context): Int? {
        return getWaveplateThresholds(context).firstOrNull()
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
        return getLastThresholdAlertValues(context).firstOrNull()
    }

    fun getLastThresholdAlertValues(context: Context): Set<Int> {
        val preferences = prefs(context)
        val stored = preferences.getString(KEY_LAST_THRESHOLD_ALERT_VALUES, null)
        val decoded = decodeThresholds(stored)
        if (decoded.isNotEmpty()) {
            return decoded.toSet()
        }
        val legacy = preferences.getInt(KEY_LAST_THRESHOLD_ALERT_VALUE, Int.MIN_VALUE)
        return if (legacy == Int.MIN_VALUE || legacy <= 0) {
            emptySet()
        } else {
            setOf(legacy)
        }
    }

    fun saveLastThresholdAlertValues(context: Context, values: Collection<Int>) {
        val sanitized = sanitizeThresholds(values)
        prefs(context).edit(commit = true) {
            if (sanitized.isEmpty()) {
                remove(KEY_LAST_THRESHOLD_ALERT_VALUES)
            } else {
                putString(KEY_LAST_THRESHOLD_ALERT_VALUES, encodeThresholds(sanitized))
            }
            remove(KEY_LAST_THRESHOLD_ALERT_VALUE)
        }
    }

    fun markThresholdAlertSent(context: Context, threshold: Int) {
        val updated = getLastThresholdAlertValues(context) + threshold
        saveLastThresholdAlertValues(context, updated)
    }

    fun clearThresholdAlert(context: Context) {
        saveLastThresholdAlertValues(context, emptySet())
    }

    fun clearThresholdAlert(context: Context, threshold: Int) {
        val updated = getLastThresholdAlertValues(context) - threshold
        saveLastThresholdAlertValues(context, updated)
    }
}
