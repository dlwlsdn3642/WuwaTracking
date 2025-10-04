package com.jinjinmory.wuwatracking.data.repository

import com.google.gson.Gson
import com.jinjinmory.wuwatracking.data.remote.WuwaApiService
import com.jinjinmory.wuwatracking.data.remote.dto.ProfileRequest
import com.jinjinmory.wuwatracking.data.remote.dto.QueryRolePayload
import com.jinjinmory.wuwatracking.data.remote.dto.QueryRoleResponse
import com.jinjinmory.wuwatracking.data.remote.dto.WuwaProfile
import kotlinx.coroutines.delay


data class ProfileFetchResult(
    val profile: WuwaProfile,
    val rawPayload: String,
    val fetchedAtMillis: Long
)

class WuwaRepository(
    private val api: WuwaApiService,
    private val gson: Gson = Gson()
) {

    suspend fun fetchProfile(request: ProfileRequest): Result<ProfileFetchResult> {
        return runCatching {
            val requestTimestamp = System.currentTimeMillis()

            runCatching { api.preflight(requestTimestamp) }

            var attempt = 0
            var lastBody: QueryRoleResponse? = null
            while (attempt < MAX_ATTEMPTS) {
                val response = api.queryRole(requestTimestamp, request)
                if (!response.isSuccessful) {
                    throw IllegalStateException("Network error: ${'$'}{response.code()} ${'$'}{response.message()}")
                }

                val body = response.body() ?: throw IllegalStateException("Empty response body")
                if (body.code == CODE_RETRY && attempt < MAX_ATTEMPTS - 1) {
                    attempt += 1
                    delay(RETRY_DELAY_MS)
                    continue
                }
                lastBody = body
                break
            }

            val finalBody = lastBody ?: throw IllegalStateException("Empty response body")
            if (finalBody.code != CODE_SUCCESS && finalBody.code != CODE_SUCCESS_ALT) {
                val message = finalBody.displayMessage ?: "Unexpected response code ${'$'}{finalBody.code}"
                throw IllegalStateException(message)
            }

            val payloadString = finalBody.extractPayload()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Missing payload data")

            val payload = gson.fromJson(payloadString, QueryRolePayload::class.java)
            val profile = payload.toProfile() ?: throw IllegalStateException("Malformed payload data")

            ProfileFetchResult(
                profile = profile,
                rawPayload = payloadString,
                fetchedAtMillis = System.currentTimeMillis()
            )
        }
    }

    private companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 700L
        private const val CODE_SUCCESS = 0
        private const val CODE_SUCCESS_ALT = 200
        private const val CODE_RETRY = 1005
    }
}
