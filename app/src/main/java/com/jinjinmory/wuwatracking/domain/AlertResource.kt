package com.jinjinmory.wuwatracking.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jinjinmory.wuwatracking.R
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile

enum class AlertResource(
    val key: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val maxInput: Int
) {
    WAVEPLATES("waveplates", R.string.proper_waveplates, R.drawable.waveplates, 240),
    WAVESUBSTANCE("wavesubstance", R.string.proper_wavesubstance, R.drawable.wavesubstance, 999),
    ACTIVITY_POINTS("activity_points", R.string.proper_activity_points, R.drawable.activity_points, 240),
    PODCAST("podcast", R.string.proper_podcast, R.drawable.podcast, 999)
}

fun AlertResource.currentValue(profile: WuwaProfile): Int =
    when (this) {
        AlertResource.WAVEPLATES -> profile.waveplatesCurrent
        AlertResource.WAVESUBSTANCE -> profile.wavesubstance
        AlertResource.ACTIVITY_POINTS -> profile.activityPointsCurrent
        AlertResource.PODCAST -> profile.podcastCurrent
    }

fun AlertResource.maxValue(profile: WuwaProfile): Int? =
    when (this) {
        AlertResource.WAVEPLATES -> profile.waveplatesMax
        AlertResource.WAVESUBSTANCE -> null
        AlertResource.ACTIVITY_POINTS -> profile.activityPointsMax
        AlertResource.PODCAST -> profile.podcastMax
    }
