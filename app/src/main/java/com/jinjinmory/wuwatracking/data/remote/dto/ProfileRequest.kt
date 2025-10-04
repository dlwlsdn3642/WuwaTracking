package com.jinjinmory.wuwatracking.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileRequest(
    @SerializedName("oauthCode") val oauthCode: String,
    @SerializedName("playerId") val playerId: String,
    @SerializedName("region") val region: String
)
