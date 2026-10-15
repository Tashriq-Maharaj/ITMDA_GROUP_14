package com.halohome.iotresearch.data.models

import com.google.gson.annotations.SerializedName

// --- Auth Models ---
data class RegisterRequest(
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

data class UserDto(
    val id: Int,
    @SerializedName("participant_id") val participantId: String,
    @SerializedName("email_hash") val emailHash: String,
    @SerializedName("created_at") val createdAt: String? = null
)

// --- Commissioning & Auditor Models ---
data class CommissionSettingsDto(
    @SerializedName("auth_credential") var authCredential: String = "default_pin",
    @SerializedName("transport_crypto") var transportCrypto: String = "mqtt_plain_1883",
    @SerializedName("network_topology") var networkTopology: String = "shared_home_wifi",
    @SerializedName("remote_exposure") var remoteExposure: String = "upnp_port",
    @SerializedName("firmware_policy") var firmwarePolicy: String = "manual_never",
    @SerializedName("data_privacy") var dataPrivacy: String = "full_cloud_sharing",
    @SerializedName("action_auth") var actionAuth: String = "one_tap_always"
)

data class CommissionMetricsDto(
    @SerializedName("toggle_count") val toggleCount: Int = 0,
    @SerializedName("warning_reversals") val warningReversals: Int = 0
)

data class DeployRequest(
    val settings: CommissionSettingsDto,
    val metrics: CommissionMetricsDto
)

data class DeployResponse(
    val success: Boolean,
    val message: String? = null,
    @SerializedName("audit_report") val auditReport: AuditReportDto? = null,
    val error: String? = null
)

data class CalculateRequest(
    val settings: CommissionSettingsDto
)

data class CalculateResponse(
    @SerializedName("security_score") val securityScore: Float,
    @SerializedName("usability_score") val usabilityScore: Float,
    val grade: String,
    @SerializedName("risk_tier") val riskTier: String,
    val risks: List<RiskDto> = emptyList(),
    val strengths: List<String> = emptyList()
)

data class AuditReportDto(
    @SerializedName("participant_id") val participantId: String,
    @SerializedName("security_score") val securityScore: Float,
    @SerializedName("usability_score") val usabilityScore: Float,
    val grade: String,
    @SerializedName("risk_tier") val riskTier: String,
    @SerializedName("total_time_seconds") val totalTimeSeconds: Float? = null,
    @SerializedName("toggle_count") val toggleCount: Int = 0,
    @SerializedName("warning_reversals") val warningReversals: Int = 0,
    val risks: List<RiskDto> = emptyList(),
    val strengths: List<String> = emptyList()
)

data class RiskDto(
    val name: String,
    val desc: String
)

// --- Telemetry Models ---
data class TelemetryEventRequest(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("event_details") val eventDetails: Map<String, Any>? = null
)
