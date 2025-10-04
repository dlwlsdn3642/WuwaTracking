package com.jinjinmory.wuwatracking.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlin.math.max

data class WuwaProfile(
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

data class QueryRoleResponse(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("data") val data: JsonElement? = null
) {
    val displayMessage: String?
        get() = message ?: msg

    fun extractPayload(): String? {
        val element = data ?: return null
        return when {
            element.isJsonArray -> element.asJsonArray.firstOrNull()?.takeIf { it.isJsonPrimitive }?.asString
            element.isJsonObject -> element.asJsonObject.entrySet().firstOrNull()?.value?.takeIf { it.isJsonPrimitive }?.asString
            element.isJsonPrimitive -> element.asString
            else -> null
        }
    }
}

data class QueryRolePayload(
    @SerializedName("BattlePass") val battlePass: BattlePass? = null,
    @SerializedName("Base") val base: Base? = null
) {
    fun toProfile(): WuwaProfile? {
        val baseData = base ?: return null
        val name = baseData.name ?: return null
        val uid = baseData.id ?: return null
        val rawLevel = baseData.level ?: 0
        val resonanceLevel = max(rawLevel - 1, 0)

        val battle = battlePass
        val waveplatesCurrent = baseData.energy ?: 0
        val waveplatesMax = baseData.maxEnergy ?: 0
        val wavesubstance = baseData.storeEnergy ?: 0
        val activityPointsCurrent = baseData.liveness ?: 0
        val activityPointsMax = baseData.livenessMaxCount ?: 0
        val podcastCurrent = battle?.weekExp ?: 0
        val podcastMax = battle?.weekMaxExp ?: 0

        return WuwaProfile(
            name = name,
            uid = uid,
            resonanceLevel = resonanceLevel,
            waveplatesCurrent = waveplatesCurrent,
            waveplatesMax = waveplatesMax,
            wavesubstance = wavesubstance,
            activityPointsCurrent = activityPointsCurrent,
            activityPointsMax = activityPointsMax,
            podcastCurrent = podcastCurrent,
            podcastMax = podcastMax
        )
    }
}

data class BattlePass(
    @SerializedName("WeekExp") val weekExp: Int? = null,
    @SerializedName("WeekMaxExp") val weekMaxExp: Int? = null
)

data class Base(
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Id") val id: String? = null,
    @SerializedName("Level") val level: Int? = null,
    @SerializedName("Energy") val energy: Int? = null,
    @SerializedName("MaxEnergy") val maxEnergy: Int? = null,
    @SerializedName("StoreEnergy") val storeEnergy: Int? = null,
    @SerializedName("Liveness") val liveness: Int? = null,
    @SerializedName("LivenessMaxCount") val livenessMaxCount: Int? = null
)
