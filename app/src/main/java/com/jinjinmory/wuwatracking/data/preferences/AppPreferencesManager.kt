package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

object AppPreferencesManager {

    private const val PREF_FILE_NAME = "wuwa_app_preferences"
    private const val KEY_BATTERY_NOTICE_ACK = "battery_notice_acknowledged"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val KEY_WIDGET_TIME_FORMAT = "widget_time_format"

    enum class AppLanguage(val code: String) {
        KOREAN("ko"),
        ENGLISH("en");

        companion object {
            fun fromCode(code: String?): AppLanguage =
                values().firstOrNull { it.code == code } ?: KOREAN
        }
    }

    enum class WidgetTimeFormat(val key: String) {
        MINUTES("minutes"),
        HOURS_MINUTES("hours_minutes"),
        ETA("eta");

        companion object {
            fun fromKey(key: String?): WidgetTimeFormat =
                values().firstOrNull { it.key == key } ?: MINUTES
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun isBatteryNoticeAcknowledged(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BATTERY_NOTICE_ACK, false)

    fun markBatteryNoticeAcknowledged(context: Context) {
        prefs(context).edit(commit = true) {
            putBoolean(KEY_BATTERY_NOTICE_ACK, true)
        }
    }

    fun getSelectedLanguage(context: Context): AppLanguage {
        val code = prefs(context).getString(KEY_SELECTED_LANGUAGE, null)
        return AppLanguage.fromCode(code)
    }

    fun setSelectedLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit(commit = true) {
            putString(KEY_SELECTED_LANGUAGE, language.code)
        }
        applyLanguage(language)
    }

    fun applyStoredLanguage(context: Context) {
        applyLanguage(getSelectedLanguage(context))
    }

    fun getWidgetTimeFormat(context: Context): WidgetTimeFormat {
        val stored = prefs(context).getString(KEY_WIDGET_TIME_FORMAT, null)
        return WidgetTimeFormat.fromKey(stored)
    }

    fun setWidgetTimeFormat(context: Context, format: WidgetTimeFormat) {
        prefs(context).edit(commit = true) {
            putString(KEY_WIDGET_TIME_FORMAT, format.key)
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
