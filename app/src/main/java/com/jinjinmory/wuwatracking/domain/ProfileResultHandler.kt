package com.jinjinmory.wuwatracking.domain

import android.content.Context
import com.jinjinmory.wuwatracking.data.preferences.NotificationSettingsManager
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.notifications.NotificationHelper
import com.jinjinmory.wuwatracking.widget.WuwaWidgetProvider

object ProfileResultHandler {

    private const val FULL_CAPACITY = 240

    fun handle(context: Context, result: ProfileFetchResult) {
        val appContext = context.applicationContext
        ProfileCacheManager.savePayload(appContext, result.rawPayload, result.fetchedAtMillis)
        ProfileCacheManager.saveProfile(appContext, result.profile)
        val profile = result.profile
        evaluateFullAlert(appContext, profile)
        evaluateThresholdAlert(appContext, profile)
        WuwaWidgetProvider.updateAll(appContext)
    }

    private fun evaluateFullAlert(context: Context, profile: WuwaProfile) {
        val current = profile.waveplatesCurrent
        if (current >= FULL_CAPACITY) {
            val last = NotificationSettingsManager.getLastFullAlertCount(context)
            val shouldNotify = last == null
            if (shouldNotify && NotificationHelper.canPostNotifications(context)) {
                NotificationHelper.notifyWaveplatesFull(context, profile)
                NotificationSettingsManager.markFullAlertSent(context, current)
            }
        } else {
            NotificationSettingsManager.clearFullAlert(context)
        }
    }

    private fun evaluateThresholdAlert(context: Context, profile: WuwaProfile) {
        val thresholds = NotificationSettingsManager.getWaveplateThresholds(context)
        if (thresholds.isEmpty()) {
            NotificationSettingsManager.saveLastThresholdAlertValues(context, emptySet())
            return
        }
        val current = profile.waveplatesCurrent
        val triggeredThresholds = thresholds.filter { current >= it }.toSet()
        if (triggeredThresholds.isEmpty()) {
            NotificationSettingsManager.saveLastThresholdAlertValues(context, emptySet())
            return
        }
        val previouslyTriggered = NotificationSettingsManager.getLastThresholdAlertValues(context)
        val newThresholds = triggeredThresholds - previouslyTriggered
        val canNotify = NotificationHelper.canPostNotifications(context)
        if (newThresholds.isNotEmpty() && canNotify) {
            newThresholds.sorted().forEach { threshold ->
                NotificationHelper.notifyWaveplatesThreshold(context, threshold, profile)
            }
        }
        val valuesToPersist = if (canNotify) {
            triggeredThresholds
        } else {
            previouslyTriggered.intersect(triggeredThresholds)
        }
        NotificationSettingsManager.saveLastThresholdAlertValues(context, valuesToPersist)
    }
}

