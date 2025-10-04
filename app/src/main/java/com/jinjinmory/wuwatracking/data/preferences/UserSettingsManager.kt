package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object UserSettingsManager {

    private const val PREF_FILE_NAME = "wuwa_user_settings"
    private const val KEY_UID = "stored_uid"
    private const val KEY_REGION = "stored_region"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun saveUid(context: Context, uid: String) {
        prefs(context).edit(commit = true) {
            if (uid.isBlank()) {
                remove(KEY_UID)
            } else {
                putString(KEY_UID, uid.trim())
            }
        }
    }

    fun saveRegion(context: Context, region: String) {
        prefs(context).edit(commit = true) {
            if (region.isBlank()) {
                remove(KEY_REGION)
            } else {
                putString(KEY_REGION, region.trim())
            }
        }
    }

    fun getUid(context: Context): String? = prefs(context).getString(KEY_UID, null)

    fun getRegion(context: Context): String? = prefs(context).getString(KEY_REGION, null)
}
