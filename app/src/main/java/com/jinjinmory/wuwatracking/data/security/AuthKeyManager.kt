package com.jinjinmory.wuwatracking.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AuthKeyManager {

    private const val PREF_FILE_NAME = "wuwa_secure_prefs"
    private const val KEY_AUTH = "auth_key"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null

    private fun getPreferences(context: Context): SharedPreferences {
        val existing = cachedPrefs
        if (existing != null) return existing

        synchronized(this) {
            val doubleChecked = cachedPrefs
            if (doubleChecked != null) return doubleChecked

            val appContext = context.applicationContext
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                appContext,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            cachedPrefs = prefs
            return prefs
        }
    }

    fun saveAuthKey(context: Context, key: String) {
        val sanitized = key.trim()
        val prefs = getPreferences(context)
        prefs.edit(commit = true) {
            if (sanitized.isEmpty()) {
                remove(KEY_AUTH)
            } else {
                putString(KEY_AUTH, sanitized)
            }
        }
    }

    fun getAuthKey(context: Context): String? {
        return getPreferences(context).getString(KEY_AUTH, null)
    }
}
