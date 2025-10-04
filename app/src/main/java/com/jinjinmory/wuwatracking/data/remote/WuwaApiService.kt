package com.jinjinmory.wuwatracking.data.remote

import com.jinjinmory.wuwatracking.data.remote.dto.ProfileRequest
import com.jinjinmory.wuwatracking.data.remote.dto.QueryRoleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.OPTIONS
import retrofit2.http.POST
import retrofit2.http.Query

interface WuwaApiService {

    @Headers(
        "Origin: null",
        "Access-Control-Request-Method: POST",
        "Access-Control-Request-Headers: content-type"
    )
    @OPTIONS("game/queryRole")
    suspend fun preflight(@Query("_t") timestamp: Long): Response<Unit>

    @Headers(
        "Origin: null",
        "Content-Type: application/json",
        "Accept: application/json, text/plain, */*"
    )
    @POST("game/queryRole")
    suspend fun queryRole(
        @Query("_t") timestamp: Long,
        @Body request: ProfileRequest
    ): Response<QueryRoleResponse>
}
