package com.jinjinmory.wuwatracking.background

import android.content.Context
import android.util.Log
import com.jinjinmory.wuwatracking.BuildConfig
import com.jinjinmory.wuwatracking.data.preferences.AppPreferencesManager
import com.jinjinmory.wuwatracking.data.preferences.UserSettingsManager
import com.jinjinmory.wuwatracking.data.remote.dto.ProfileRequest
import com.jinjinmory.wuwatracking.data.repository.WuwaRepository
import com.jinjinmory.wuwatracking.data.security.AuthKeyManager
import com.jinjinmory.wuwatracking.di.AppContainer
import com.jinjinmory.wuwatracking.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ActivityReminderWorker {

    private const val TAG = "ActivityReminderWorker"
    private const val REMINDER_THRESHOLD = 100

    suspend fun checkAndNotify(context: Context) {
        val config = AppPreferencesManager.getActivityReminder(context)
        if (!config.enabled || config.hour == null || config.minute == null) return

        val profile = UserSettingsManager.getActiveProfile(context)
        val authKey = AuthKeyManager.getAuthKey(context)?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_AUTH_KEY
        val uid = profile?.uid?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_UID
        val region = profile?.region?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_REGION

        if (authKey.isBlank() || uid.isBlank() || region.isBlank()) {
            Log.d(TAG, "Skipping reminder refresh: missing credentials")
            return
        }

        val request = ProfileRequest(
            oauthCode = authKey,
            playerId = uid,
            region = region
        )

        val result = withContext(Dispatchers.IO) {
            repository().fetchProfile(request)
        }

        result.onSuccess { profileResult ->
            val profileData = profileResult.profile
            if (profileData.activityPointsCurrent < REMINDER_THRESHOLD) {
                NotificationHelper.notifyActivityReminder(
                    context = context,
                    profileName = profileData.name,
                    current = profileData.activityPointsCurrent
                )
            }
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to refresh profile for activity reminder", throwable)
        }
    }

    private fun repository(): WuwaRepository = AppContainer.repository
}
