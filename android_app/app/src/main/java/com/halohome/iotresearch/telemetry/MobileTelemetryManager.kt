package com.halohome.iotresearch.telemetry

import android.util.Log
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.TelemetryEventRequest
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

object MobileTelemetryManager {

    private const val TAG = "MobileTelemetry"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var toggleCount: Int = 0
    var warningReversals: Int = 0

    private var heartbeatJob: Job? = null
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

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

    fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun recordToggle(settingKey: String, chosenOption: String) {
        toggleCount++
        logEvent("setting_toggled", mapOf("setting" to settingKey, "chosen" to chosenOption, "total_toggles" to toggleCount))
    }

    fun recordWarningReversal(settingKey: String, previousOption: String, newOption: String) {
        warningReversals++
        logEvent("warning_reversal", mapOf(
            "setting" to settingKey,
            "from" to previousOption,
            "to" to newOption,
            "total_reversals" to warningReversals
        ))
    }

    fun onAppBackgrounded() {
        logEvent("app_closed_or_backgrounded", mapOf("toggles_so_far" to toggleCount))
    }

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

    fun markAbandonment(reason: String = "voluntary_exit", onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                NetworkModule.apiService.abandonSession(mapOf("reason" to reason))
            } catch (e: Exception) {
                Log.w(TAG, "Abandon failed: ${e.message}")
            } finally {
                stopHeartbeat()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
