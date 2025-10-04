package com.jinjinmory.wuwatracking.di

import com.jinjinmory.wuwatracking.data.repository.WuwaRepository

object AppContainer {
    val repository: WuwaRepository by lazy {
        WuwaRepository(NetworkModule.apiService)
    }
}
