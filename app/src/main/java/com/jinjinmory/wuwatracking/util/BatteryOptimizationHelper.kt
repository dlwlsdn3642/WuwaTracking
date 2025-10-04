package com.jinjinmory.wuwatracking.util

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

object BatteryOptimizationHelper {

    fun isIgnoringOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }
}

