package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jinjinmory.wuwatracking.data.security.AuthKeyManager
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object UserSettingsManager {

    private const val PREF_FILE_NAME = "wuwa_user_settings"
    private const val KEY_UID_LEGACY = "stored_uid"
    private const val KEY_REGION_LEGACY = "stored_region"
    private const val KEY_PROFILES = "stored_profiles_v1"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

    data class StoredProfile(
        val id: String,
        val uid: String,
        val region: String
    )

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun getProfiles(context: Context): List<StoredProfile> {
        ensureMigrated(context)
        val raw = prefs(context).getString(KEY_PROFILES, null) ?: return emptyList()
        return decodeProfiles(raw)
    }

    fun replaceProfiles(context: Context, profiles: List<StoredProfile>) {
        ensureMigrated(context)
        val sanitized = profiles.distinctBy { it.id }
        val preferences = prefs(context)
        persistProfiles(preferences, sanitized)
        val active = getActiveProfileId(context)
        if (active != null && sanitized.none { it.id == active }) {
            setActiveProfileId(context, sanitized.firstOrNull()?.id)
        }
    }

    fun upsertProfile(context: Context, profile: StoredProfile) {
        ensureMigrated(context)
        val preferences = prefs(context)
        val current = getProfiles(context).toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(profile)
        }
        persistProfiles(preferences, current)
    }

    fun removeProfile(context: Context, profileId: String) {
        ensureMigrated(context)
        val preferences = prefs(context)
        val current = getProfiles(context)
        if (current.none { it.id == profileId }) return

        val updated = current.filterNot { it.id == profileId }
        persistProfiles(preferences, updated)
        AuthKeyManager.removeAuthKeyForProfile(context, profileId)

        val activeId = getActiveProfileId(context)
        if (activeId == profileId) {
            setActiveProfileId(context, updated.firstOrNull()?.id)
        }
    }

    fun getActiveProfileId(context: Context): String? {
        ensureMigrated(context)
        return prefs(context).getString(KEY_ACTIVE_PROFILE_ID, null)
    }

    fun setActiveProfileId(context: Context, profileId: String?) {
        ensureMigrated(context)
        prefs(context).edit(commit = true) {
            if (profileId.isNullOrBlank()) {
                remove(KEY_ACTIVE_PROFILE_ID)
            } else {
                putString(KEY_ACTIVE_PROFILE_ID, profileId)
            }
        }
    }

    fun getActiveProfile(context: Context): StoredProfile? {
        val activeId = getActiveProfileId(context) ?: return null
        return getProfiles(context).firstOrNull { it.id == activeId }
    }

    fun ensureActiveProfileId(context: Context): String {
        ensureMigrated(context)
        val existingId = getActiveProfileId(context)
        if (!existingId.isNullOrBlank()) return existingId

        val preferences = prefs(context)
        val current = getProfiles(context)
        if (current.isNotEmpty()) {
            val first = current.first().id
            preferences.edit(commit = true) { putString(KEY_ACTIVE_PROFILE_ID, first) }
            return first
        }

        val newProfile = StoredProfile(generateProfileId(), "", "")
        persistProfiles(preferences, listOf(newProfile))
        preferences.edit(commit = true) { putString(KEY_ACTIVE_PROFILE_ID, newProfile.id) }
        return newProfile.id
    }

    fun generateProfileId(): String = UUID.randomUUID().toString()

    // Legacy compatibility helpers
    fun saveUid(context: Context, uid: String) {
        val sanitized = uid.trim()
        val activeId = ensureActiveProfileId(context)
        val profile = getProfiles(context).firstOrNull { it.id == activeId } ?: StoredProfile(activeId, "", "")
        upsertProfile(context, profile.copy(uid = sanitized))
    }

    fun saveRegion(context: Context, region: String) {
        val sanitized = region.trim()
        val activeId = ensureActiveProfileId(context)
        val profile = getProfiles(context).firstOrNull { it.id == activeId } ?: StoredProfile(activeId, "", "")
        upsertProfile(context, profile.copy(region = sanitized))
    }

    fun getUid(context: Context): String? = getActiveProfile(context)?.uid?.takeIf { it.isNotBlank() }
    fun getRegion(context: Context): String? = getActiveProfile(context)?.region?.takeIf { it.isNotBlank() }

    private fun ensureMigrated(context: Context) {
        val preferences = prefs(context)
        if (preferences.contains(KEY_PROFILES)) return

        val legacyUid = preferences.getString(KEY_UID_LEGACY, null)?.trim().orEmpty()
        val legacyRegion = preferences.getString(KEY_REGION_LEGACY, null)?.trim().orEmpty()
        val legacyAuthKey = AuthKeyManager.consumeLegacyAuthKey(context)

        val hasProfiles = if (legacyUid.isNotEmpty() || legacyRegion.isNotEmpty() || !legacyAuthKey.isNullOrEmpty()) {
            val profileId = generateProfileId()
            val profile = StoredProfile(
                id = profileId,
                uid = legacyUid,
                region = legacyRegion
            )
            persistProfiles(preferences, listOf(profile))
            if (!legacyAuthKey.isNullOrEmpty()) {
                AuthKeyManager.saveAuthKeyForProfile(context, profileId, legacyAuthKey)
            }
            preferences.edit(commit = true) {
                putString(KEY_ACTIVE_PROFILE_ID, profileId)
            }
            true
        } else {
            persistProfiles(preferences, emptyList())
            false
        }

        val hadActiveProfile = preferences.contains(KEY_ACTIVE_PROFILE_ID)
        preferences.edit(commit = true) {
            remove(KEY_UID_LEGACY)
            remove(KEY_REGION_LEGACY)
            if (!hasProfiles && hadActiveProfile) {
                remove(KEY_ACTIVE_PROFILE_ID)
            }
        }
    }

    private fun decodeProfiles(raw: String): List<StoredProfile> =
        runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id", "").takeIf { it.isNotBlank() } ?: continue
                    val uid = item.optString("uid", "")
                    val region = item.optString("region", "")
                    add(StoredProfile(id = id, uid = uid, region = region))
                }
            }
        }.getOrElse { emptyList() }

    private fun persistProfiles(prefs: SharedPreferences, profiles: List<StoredProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            val obj = JSONObject()
                .put("id", profile.id)
                .put("uid", profile.uid)
                .put("region", profile.region)
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putString(KEY_PROFILES, array.toString())
        }
    }
}
