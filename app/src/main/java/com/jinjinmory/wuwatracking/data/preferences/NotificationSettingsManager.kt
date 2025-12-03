package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jinjinmory.wuwatracking.domain.AlertResource

object NotificationSettingsManager {

    private const val PREF_FILE_NAME = "wuwa_notification_settings"
    private const val KEY_WAVEPLATE_THRESHOLD = "waveplate_threshold"
    private const val KEY_WAVEPLATE_THRESHOLDS = "waveplate_thresholds"
    private const val KEY_LAST_FULL_ALERT_COUNT = "last_full_alert_count"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUE = "last_threshold_alert_value"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUES = "last_threshold_alert_values"
    private const val KEY_THRESHOLDS_PREFIX = "thresholds_"
    private const val KEY_LAST_THRESHOLD_ALERT_VALUES_PREFIX = "last_threshold_alert_values_"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    private fun thresholdKey(resource: AlertResource): String =
        "$KEY_THRESHOLDS_PREFIX${resource.key}"

    private fun lastThresholdKey(resource: AlertResource): String =
        "$KEY_LAST_THRESHOLD_ALERT_VALUES_PREFIX${resource.key}"

    private fun sanitizeThresholds(resource: AlertResource, thresholds: Collection<Int>): List<Int> =
        thresholds
            .mapNotNull { value ->
                value.takeIf { it in 1..resource.maxInput }
            }
            .distinct()
            .sorted()

    private fun encodeThresholds(values: List<Int>): String =
        values.joinToString(separator = ",") { it.toString() }

    private fun decodeThresholds(raw: String?, resource: AlertResource): List<Int> =
        raw
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it in 1..resource.maxInput }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    fun saveThresholds(context: Context, resource: AlertResource, thresholds: Collection<Int>) {
        val sanitized = sanitizeThresholds(resource, thresholds)
        prefs(context).edit(commit = true) {
            if (sanitized.isEmpty()) {
                remove(thresholdKey(resource))
            } else {
                putString(thresholdKey(resource), encodeThresholds(sanitized))
            }
            if (resource == AlertResource.WAVEPLATES) {
                remove(KEY_WAVEPLATE_THRESHOLD)
                remove(KEY_WAVEPLATE_THRESHOLDS)
            }
        }
    }

    fun getThresholds(context: Context, resource: AlertResource): List<Int> {
        val preferences = prefs(context)
        val stored = preferences.getString(thresholdKey(resource), null)
        val decoded = decodeThresholds(stored, resource)
        if (decoded.isNotEmpty()) {
            return decoded
        }
        if (resource == AlertResource.WAVEPLATES) {
            val legacyString = preferences.getString(KEY_WAVEPLATE_THRESHOLDS, null)
            val legacyDecoded = decodeThresholds(legacyString, resource)
            if (legacyDecoded.isNotEmpty()) {
                saveThresholds(context, resource, legacyDecoded)
                return legacyDecoded
            }
            val legacySingle = preferences.getInt(KEY_WAVEPLATE_THRESHOLD, Int.MIN_VALUE)
            return if (legacySingle == Int.MIN_VALUE || legacySingle <= 0) {
                emptyList()
            } else {
                val normalized = listOf(legacySingle)
                saveThresholds(context, resource, normalized)
                normalized
            }
        }
        return emptyList()
    }

    fun saveWaveplateThresholds(context: Context, thresholds: Collection<Int>) {
        saveThresholds(context, AlertResource.WAVEPLATES, thresholds)
    }

    fun getWaveplateThresholds(context: Context): List<Int> {
        return getThresholds(context, AlertResource.WAVEPLATES)
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
        return getLastThresholdAlertValues(context, AlertResource.WAVEPLATES).firstOrNull()
    }

    fun getLastThresholdAlertValues(context: Context, resource: AlertResource): Set<Int> {
        val preferences = prefs(context)
        val stored = preferences.getString(lastThresholdKey(resource), null)
        val decoded = decodeThresholds(stored, resource)
        if (decoded.isNotEmpty()) {
            return decoded.toSet()
        }
        if (resource == AlertResource.WAVEPLATES) {
            val legacyStored = preferences.getString(KEY_LAST_THRESHOLD_ALERT_VALUES, null)
            val legacyDecoded = decodeThresholds(legacyStored, resource)
            if (legacyDecoded.isNotEmpty()) {
                saveLastThresholdAlertValues(context, resource, legacyDecoded)
                return legacyDecoded.toSet()
            }
            val legacySingle = preferences.getInt(KEY_LAST_THRESHOLD_ALERT_VALUE, Int.MIN_VALUE)
            if (legacySingle != Int.MIN_VALUE && legacySingle > 0) {
                val values = setOf(legacySingle)
                saveLastThresholdAlertValues(context, resource, values)
                return values
            }
        }
        return emptySet()
    }

    fun saveLastThresholdAlertValues(context: Context, resource: AlertResource, values: Collection<Int>) {
        val sanitized = sanitizeThresholds(resource, values)
        prefs(context).edit(commit = true) {
            if (sanitized.isEmpty()) {
                remove(lastThresholdKey(resource))
            } else {
                putString(lastThresholdKey(resource), encodeThresholds(sanitized))
            }
            if (resource == AlertResource.WAVEPLATES) {
                remove(KEY_LAST_THRESHOLD_ALERT_VALUE)
                remove(KEY_LAST_THRESHOLD_ALERT_VALUES)
            }
        }
    }

    fun saveLastThresholdAlertValues(context: Context, values: Collection<Int>) {
        saveLastThresholdAlertValues(context, AlertResource.WAVEPLATES, values)
    }

    fun markThresholdAlertSent(context: Context, resource: AlertResource, threshold: Int) {
        val updated = getLastThresholdAlertValues(context, resource) + threshold
        saveLastThresholdAlertValues(context, resource, updated)
    }

    fun markThresholdAlertSent(context: Context, threshold: Int) {
        markThresholdAlertSent(context, AlertResource.WAVEPLATES, threshold)
    }

    fun clearThresholdAlert(context: Context) {
        clearThresholdAlert(context, AlertResource.WAVEPLATES)
    }

    fun clearThresholdAlert(context: Context, resource: AlertResource) {
        saveLastThresholdAlertValues(context, resource, emptySet())
    }

    fun clearThresholdAlert(context: Context, resource: AlertResource, threshold: Int) {
        val updated = getLastThresholdAlertValues(context, resource) - threshold
        saveLastThresholdAlertValues(context, resource, updated)
    }
}
