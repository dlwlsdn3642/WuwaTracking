package com.jinjinmory.wuwatracking.domain

import android.content.Context
import com.jinjinmory.wuwatracking.data.preferences.NotificationSettingsManager
import com.jinjinmory.wuwatracking.data.preferences.ProfileCacheManager
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import com.jinjinmory.wuwatracking.data.repository.ProfileFetchResult
import com.jinjinmory.wuwatracking.notifications.NotificationHelper
import com.jinjinmory.wuwatracking.widget.WuwaWidgetProvider
import com.jinjinmory.wuwatracking.widget.WuwaMiniWidgetProvider
import com.jinjinmory.wuwatracking.domain.AlertResource
import com.jinjinmory.wuwatracking.domain.currentValue

object ProfileResultHandler {

    private const val FULL_CAPACITY = 240

    fun handle(context: Context, result: ProfileFetchResult) {
        val appContext = context.applicationContext
        ProfileCacheManager.savePayload(appContext, result.rawPayload, result.fetchedAtMillis)
        ProfileCacheManager.saveProfile(appContext, result.profile)
        val profile = result.profile
        evaluateFullAlert(appContext, profile)
        evaluateThresholdAlerts(appContext, profile)
        WuwaWidgetProvider.updateAll(appContext)
        WuwaMiniWidgetProvider.updateAll(appContext)
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

    private fun evaluateThresholdAlerts(context: Context, profile: WuwaProfile) {
        val canNotify = NotificationHelper.canPostNotifications(context)
        AlertResource.entries.forEach { resource ->
            val thresholds = NotificationSettingsManager.getThresholds(context, resource)
            if (thresholds.isEmpty()) {
                NotificationSettingsManager.clearThresholdAlert(context, resource)
                return@forEach
            }
            val current = resource.currentValue(profile)
            val triggeredThresholds = thresholds.filter { current >= it }.toSet()
            if (triggeredThresholds.isEmpty()) {
                NotificationSettingsManager.clearThresholdAlert(context, resource)
                return@forEach
            }
            val previouslyTriggered = NotificationSettingsManager.getLastThresholdAlertValues(context, resource)
            val newThresholds = triggeredThresholds - previouslyTriggered
            if (newThresholds.isNotEmpty() && canNotify) {
                val resourceName = context.getString(resource.titleRes)
                newThresholds.sorted().forEach { threshold ->
                    NotificationHelper.notifyResourceThreshold(
                        context = context,
                        resource = resource,
                        resourceName = resourceName,
                        threshold = threshold,
                        current = current,
                        profileName = profile.name
                    )
                }
            }
            val valuesToPersist = if (canNotify) {
                triggeredThresholds
            } else {
                previouslyTriggered.intersect(triggeredThresholds)
            }
            NotificationSettingsManager.saveLastThresholdAlertValues(context, resource, valuesToPersist)
        }
    }
}

