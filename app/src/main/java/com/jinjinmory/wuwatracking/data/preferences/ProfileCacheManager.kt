package com.jinjinmory.wuwatracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile

object ProfileCacheManager {

    private const val PREF_FILE_NAME = "wuwa_profile_cache"
    private const val KEY_RAW_PAYLOAD = "raw_payload"
    private const val KEY_FETCHED_AT = "fetched_at"

    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_UID = "profile_uid"
    private const val KEY_PROFILE_RESONANCE = "profile_resonance"
    private const val KEY_PROFILE_WAVEPLATES_CURRENT = "profile_waveplates_current"
    private const val KEY_PROFILE_WAVEPLATES_MAX = "profile_waveplates_max"
    private const val KEY_PROFILE_WAVESUBSTANCE = "profile_wavesubstance"
    private const val KEY_PROFILE_ACTIVITY_CURRENT = "profile_activity_current"
    private const val KEY_PROFILE_ACTIVITY_MAX = "profile_activity_max"
    private const val KEY_PROFILE_PODCAST_CURRENT = "profile_podcast_current"
    private const val KEY_PROFILE_PODCAST_MAX = "profile_podcast_max"

    data class CachedPayload(
        val rawPayload: String,
        val fetchedAtMillis: Long
    )

    data class CachedProfile(
        val name: String,
        val uid: String,
        val resonanceLevel: Int,
        val waveplatesCurrent: Int,
        val waveplatesMax: Int,
        val wavesubstance: Int,
        val activityPointsCurrent: Int,
        val activityPointsMax: Int,
        val podcastCurrent: Int,
        val podcastMax: Int
    )

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    fun savePayload(context: Context, rawPayload: String, fetchedAtMillis: Long) {
        prefs(context).edit(commit = true) {
            putString(KEY_RAW_PAYLOAD, rawPayload)
            putLong(KEY_FETCHED_AT, fetchedAtMillis)
        }
    }

    fun getPayload(context: Context): CachedPayload? {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_RAW_PAYLOAD, null) ?: return null
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, -1L)
        if (fetchedAt <= 0L) return null
        return CachedPayload(raw, fetchedAt)
    }

    fun saveProfile(context: Context, profile: WuwaProfile) {
        prefs(context).edit(commit = true) {
            putString(KEY_PROFILE_NAME, profile.name)
            putString(KEY_PROFILE_UID, profile.uid)
            putInt(KEY_PROFILE_RESONANCE, profile.resonanceLevel)
            putInt(KEY_PROFILE_WAVEPLATES_CURRENT, profile.waveplatesCurrent)
            putInt(KEY_PROFILE_WAVEPLATES_MAX, profile.waveplatesMax)
            putInt(KEY_PROFILE_WAVESUBSTANCE, profile.wavesubstance)
            putInt(KEY_PROFILE_ACTIVITY_CURRENT, profile.activityPointsCurrent)
            putInt(KEY_PROFILE_ACTIVITY_MAX, profile.activityPointsMax)
            putInt(KEY_PROFILE_PODCAST_CURRENT, profile.podcastCurrent)
            putInt(KEY_PROFILE_PODCAST_MAX, profile.podcastMax)
        }
    }

    fun getProfile(context: Context): CachedProfile? {
        val prefs = prefs(context)
        val name = prefs.getString(KEY_PROFILE_NAME, null) ?: return null
        val uid = prefs.getString(KEY_PROFILE_UID, null) ?: return null
        return CachedProfile(
            name = name,
            uid = uid,
            resonanceLevel = prefs.getInt(KEY_PROFILE_RESONANCE, 0),
            waveplatesCurrent = prefs.getInt(KEY_PROFILE_WAVEPLATES_CURRENT, 0),
            waveplatesMax = prefs.getInt(KEY_PROFILE_WAVEPLATES_MAX, 0),
            wavesubstance = prefs.getInt(KEY_PROFILE_WAVESUBSTANCE, 0),
            activityPointsCurrent = prefs.getInt(KEY_PROFILE_ACTIVITY_CURRENT, 0),
            activityPointsMax = prefs.getInt(KEY_PROFILE_ACTIVITY_MAX, 0),
            podcastCurrent = prefs.getInt(KEY_PROFILE_PODCAST_CURRENT, 0),
            podcastMax = prefs.getInt(KEY_PROFILE_PODCAST_MAX, 0)
        )
    }

    fun clear(context: Context) {
        prefs(context).edit(commit = true) {
            remove(KEY_RAW_PAYLOAD)
            remove(KEY_FETCHED_AT)
            remove(KEY_PROFILE_NAME)
            remove(KEY_PROFILE_UID)
            remove(KEY_PROFILE_RESONANCE)
            remove(KEY_PROFILE_WAVEPLATES_CURRENT)
            remove(KEY_PROFILE_WAVEPLATES_MAX)
            remove(KEY_PROFILE_WAVESUBSTANCE)
            remove(KEY_PROFILE_ACTIVITY_CURRENT)
            remove(KEY_PROFILE_ACTIVITY_MAX)
            remove(KEY_PROFILE_PODCAST_CURRENT)
            remove(KEY_PROFILE_PODCAST_MAX)
        }
    }
}
