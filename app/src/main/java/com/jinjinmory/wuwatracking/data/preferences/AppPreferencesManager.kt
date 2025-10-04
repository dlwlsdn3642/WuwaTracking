package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object AppPreferencesManager {

    private const val PREF_FILE_NAME = "wuwa_app_preferences"
    private const val KEY_BATTERY_NOTICE_ACK = "battery_notice_acknowledged"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun isBatteryNoticeAcknowledged(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BATTERY_NOTICE_ACK, false)

    fun markBatteryNoticeAcknowledged(context: Context) {
        prefs(context).edit(commit = true) {
            putBoolean(KEY_BATTERY_NOTICE_ACK, true)
        }
    }
}
