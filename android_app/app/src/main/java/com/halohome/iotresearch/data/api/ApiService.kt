package com.halohome.iotresearch.data.api

import com.halohome.iotresearch.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/user/profile")
    suspend fun getProfile(): Response<AuthResponse>

    @POST("api/commission/calculate")
    suspend fun calculateScores(@Body request: CalculateRequest): Response<CalculateResponse>

    @POST("api/commission/deploy")
    suspend fun deployGateway(@Body request: DeployRequest): Response<DeployResponse>

    @POST("api/telemetry/event")
    suspend fun logEvent(@Body request: TelemetryEventRequest): Response<Map<String, Any>>

    @POST("api/telemetry/heartbeat")
    suspend fun sendHeartbeat(): Response<Map<String, Any>>

    @POST("api/telemetry/abandon")
    suspend fun abandonSession(@Body request: Map<String, String>): Response<Map<String, Any>>
}
