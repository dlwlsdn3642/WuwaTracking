package com.jinjinmory.wuwatracking.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinjinmory.wuwatracking.BuildConfig
import com.jinjinmory.wuwatracking.data.remote.dto.ProfileRequest
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.data.repository.WuwaRepository
import com.jinjinmory.wuwatracking.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repository: WuwaRepository = AppContainer.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(
        authKey: String = BuildConfig.DEFAULT_AUTH_KEY,
        uid: String = BuildConfig.DEFAULT_UID,
        region: String? = null
    ) {
        val effectiveAuthKey = authKey.ifBlank { BuildConfig.DEFAULT_AUTH_KEY }
        if (effectiveAuthKey.isBlank()) {
            _uiState.value = MainUiState.Empty("Authentication key is missing. Update settings to proceed.")
            return
        }

        val effectiveUid = uid.ifBlank { BuildConfig.DEFAULT_UID }
        if (effectiveUid.isBlank()) {
            _uiState.value = MainUiState.Empty("UID is empty. Update DEFAULT_UID before refreshing.")
            return
        }

        val effectiveRegion = region?.ifBlank { BuildConfig.DEFAULT_REGION } ?: BuildConfig.DEFAULT_REGION
        if (effectiveRegion.isBlank()) {
            _uiState.value = MainUiState.Empty("Region is missing. Update settings to proceed.")
            return
        }

        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            val request = ProfileRequest(
                oauthCode = effectiveAuthKey,
                playerId = effectiveUid,
                region = effectiveRegion
            )
            val result = withContext(Dispatchers.IO) {
                repository.fetchProfile(request)
            }
            result
                .onSuccess { profileResult: ProfileFetchResult ->
                    _uiState.value = MainUiState.Success(
                        profile = profileResult.profile,
                        rawPayload = profileResult.rawPayload,
                        fetchedAtMillis = profileResult.fetchedAtMillis
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = MainUiState.Error(throwable.message ?: "Unknown error")
                }
        }
    }
}
