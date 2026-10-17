package com.halohome.iotresearch.telemetry

import android.util.Log
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.TelemetryEventRequest
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages participant behavioral telemetry during gateway commissioning.
 * Dispatches background heartbeats, tracks setting adjustments, and records
 * warning reversals (when risk alerts motivate users to harden settings).
 */
object MobileTelemetryManager {

    private const val TAG = "MobileTelemetry"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Total number of configuration options clicked/toggled during setup. */
    var toggleCount: Int = 0

    /** Count of times a user reverted a dangerous setting after seeing a risk alert. */
    var warningReversals: Int = 0

    private var heartbeatJob: Job? = null
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Starts a coroutine that pings the server every 10 seconds.
     * Keeps the active session alive during commissioning.
     */
    fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(10000)
                try {
                    if (NetworkModule.authToken != null) {
                        NetworkModule.apiService.sendHeartbeat()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat failed: ${e.message}")
                }
            }
        }
    }

    /** Stops the active heartbeat coroutine. */
    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /** Logs an individual setting toggle event and increments the counter. */
    fun recordToggle(settingKey: String, chosenOption: String) {
        toggleCount++
        logEvent("setting_toggled", mapOf("setting" to settingKey, "chosen" to chosenOption, "total_toggles" to toggleCount))
    }

    /**
     * Records when a participant modifies a high-risk setting to a more secure option
     * after viewing the auditor's risk alert banner.
     */
    fun recordWarningReversal(settingKey: String, previousOption: String, newOption: String) {
        warningReversals++
        logEvent("warning_reversal", mapOf(
            "setting" to settingKey,
            "from" to previousOption,
            "to" to newOption,
            "total_reversals" to warningReversals
        ))
    }

    /** Logs when the mobile app is paused or placed into the background. */
    fun onAppBackgrounded() {
        logEvent("app_closed_or_backgrounded", mapOf("toggles_so_far" to toggleCount))
    }

    /** Sends an asynchronous telemetry event to the Flask backend. */
    fun logEvent(eventType: String, details: Map<String, Any> = emptyMap()) {
        scope.launch {
            try {
                if (NetworkModule.authToken != null) {
                    val fullDetails = details.toMutableMap().apply {
                        put("client_timestamp", isoFormat.format(Date()))
                        put("platform", "android_jetpack_compose")
                    }
                    NetworkModule.apiService.logEvent(
                        TelemetryEventRequest(
                            eventType = eventType,
                            eventDetails = fullDetails
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to log event $eventType: ${e.message}")
            }
        }
    }

    /** Logs out the participant and cleans up background telemetry tasks. */
    fun logLogout(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                NetworkModule.apiService.logout()
            } catch (e: Exception) {
                Log.w(TAG, "Logout request failed: ${e.message}")
            } finally {
                stopHeartbeat()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    /** Compatibility alias for logout. */
    fun markAbandonment(reason: String = "voluntary_exit", onComplete: () -> Unit = {}) {
        logLogout(onComplete)
    }
}
