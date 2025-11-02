package com.jinjinmory.wuwatracking.background

import android.content.Context
import android.util.Log
import com.jinjinmory.wuwatracking.BuildConfig
import com.jinjinmory.wuwatracking.data.preferences.UserSettingsManager
import com.jinjinmory.wuwatracking.data.remote.dto.ProfileRequest
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.data.repository.WuwaRepository
import com.jinjinmory.wuwatracking.data.security.AuthKeyManager
import com.jinjinmory.wuwatracking.di.AppContainer
import com.jinjinmory.wuwatracking.domain.ProfileResultHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProfileRefreshWorker {

    private const val TAG = "ProfileRefreshWorker"

    suspend fun refresh(context: Context) {
        val profile = UserSettingsManager.getActiveProfile(context)
        val authKey = AuthKeyManager.getAuthKey(context)?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_AUTH_KEY
        val uid = profile?.uid?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_UID
        val region = profile?.region?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_REGION

        if (authKey.isBlank() || uid.isBlank() || region.isBlank()) {
            Log.d(TAG, "Skipping refresh: missing credentials")
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

        result.onSuccess { profileResult: ProfileFetchResult ->
            ProfileResultHandler.handle(context, profileResult)
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to refresh profile", throwable)
        }
    }

    private fun repository(): WuwaRepository = AppContainer.repository
}
