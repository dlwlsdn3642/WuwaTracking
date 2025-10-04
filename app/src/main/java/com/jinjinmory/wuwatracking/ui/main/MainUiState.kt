package com.jinjinmory.wuwatracking.ui.main

import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Success(
        val profile: WuwaProfile,
        val rawPayload: String,
        val fetchedAtMillis: Long
    ) : MainUiState
    data class Error(val message: String) : MainUiState
    data class Empty(val message: String) : MainUiState
}
