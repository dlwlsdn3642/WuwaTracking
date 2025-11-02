package com.jinjinmory.wuwatracking.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.jinjinmory.wuwatracking.data.preferences.UserSettingsManager
import org.json.JSONObject

object AuthKeyManager {

    private const val PREF_FILE_NAME = "wuwa_secure_prefs"
    private const val KEY_AUTH_LEGACY = "auth_key"
    private const val KEY_AUTH_MAP = "auth_key_map"

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
        val activeProfileId = UserSettingsManager.getActiveProfileId(context)
            ?: UserSettingsManager.ensureActiveProfileId(context)
        saveAuthKeyForProfile(context, activeProfileId, key)
    }

    fun getAuthKey(context: Context): String? {
        val activeProfileId = UserSettingsManager.getActiveProfileId(context) ?: return null
        return getAuthKeyForProfile(context, activeProfileId)
    }

    fun saveAuthKeyForProfile(context: Context, profileId: String, key: String) {
        val sanitized = key.trim()
        val prefs = getPreferences(context)
        val map = loadAuthMap(prefs)
        if (sanitized.isEmpty()) {
            map.remove(profileId)
        } else {
            map[profileId] = sanitized
        }
        persistAuthMap(prefs, map)
    }

    fun removeAuthKeyForProfile(context: Context, profileId: String) {
        val prefs = getPreferences(context)
        val map = loadAuthMap(prefs)
        if (map.remove(profileId) != null) {
            persistAuthMap(prefs, map)
        }
    }

    fun getAuthKeyForProfile(context: Context, profileId: String): String? {
        val prefs = getPreferences(context)
        val map = loadAuthMap(prefs)
        return map[profileId]
    }

    internal fun consumeLegacyAuthKey(context: Context): String? {
        val prefs = getPreferences(context)
        val legacy = prefs.getString(KEY_AUTH_LEGACY, null)?.trim().orEmpty()
        if (legacy.isNotEmpty()) {
            prefs.edit(commit = true) {
                remove(KEY_AUTH_LEGACY)
            }
            return legacy
        }
        return null
    }

    private fun loadAuthMap(prefs: SharedPreferences): MutableMap<String, String> {
        val raw = prefs.getString(KEY_AUTH_MAP, null) ?: return mutableMapOf()
        return runCatching {
            val json = JSONObject(raw)
            val iterator = json.keys()
            val map = mutableMapOf<String, String>()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val value = json.optString(key, "")
                if (value.isNotEmpty()) {
                    map[key] = value
                }
            }
            map
        }.getOrElse { mutableMapOf() }
    }

    private fun persistAuthMap(prefs: SharedPreferences, map: Map<String, String>) {
        prefs.edit(commit = true) {
            remove(KEY_AUTH_LEGACY)
            if (map.isEmpty()) {
                remove(KEY_AUTH_MAP)
            } else {
                val json = JSONObject()
                map.forEach { (profileId, authKey) ->
                    json.put(profileId, authKey)
                }
                putString(KEY_AUTH_MAP, json.toString())
            }
        }
    }
}
