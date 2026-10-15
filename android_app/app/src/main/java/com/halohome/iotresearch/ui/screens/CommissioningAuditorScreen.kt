package com.halohome.iotresearch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.*
import com.halohome.iotresearch.telemetry.MobileTelemetryManager
import com.halohome.iotresearch.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissioningAuditorScreen(
    onCommissionSuccess: (AuditReportDto) -> Unit,
    onAbandoned: () -> Unit
) {
    // Current 7 settings (Option A factory defaults)
    var authCredential by remember { mutableStateOf("default_pin") }
    var transportCrypto by remember { mutableStateOf("mqtt_plain_1883") }
    var networkTopology by remember { mutableStateOf("shared_home_wifi") }
    var remoteExposure by remember { mutableStateOf("upnp_port") }
    var firmwarePolicy by remember { mutableStateOf("manual_never") }
    var dataPrivacy by remember { mutableStateOf("full_cloud_sharing") }
    var actionAuth by remember { mutableStateOf("one_tap_always") }

    // Live Audit State
    var securityScore by remember { mutableFloatStateOf(15.0f) }
    var usabilityScore by remember { mutableFloatStateOf(100.0f) }
    var grade by remember { mutableStateOf("D-") }
    var riskTier by remember { mutableStateOf("High Vulnerability") }

    // Dialog state
    var isDeploying by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableIntStateOf(0) }
    var showScanDialog by remember { mutableStateOf(false) }
    var auditReport by remember { mutableStateOf<AuditReportDto?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Function to calculate scores locally
    fun recalculate() {
        var sec = 0f
        var use = 0f

        when (authCredential) {
            "default_pin" -> { sec += 2f; use += 15f }
            "pin_6" -> { sec += 10f; use += 12f }
            "passphrase_biometric" -> { sec += 15f; use += 6f }
        }
        when (transportCrypto) {
            "mqtt_plain_1883" -> { sec += 2f; use += 20f }
            "tls_8883" -> { sec += 16f; use += 16f }
            "mtls_strict" -> { sec += 20f; use += 7f }
        }
        when (networkTopology) {
            "shared_home_wifi" -> { sec += 3f; use += 15f }
            "isolated_vlan" -> { sec += 15f; use += 8f }
        }
        when (remoteExposure) {
            "upnp_port" -> { sec += 2f; use += 20f }
            "cloud_relay" -> { sec += 16f; use += 17f }
            "local_lan" -> { sec += 20f; use += 6f }
        }
        when (firmwarePolicy) {
            "manual_never" -> { sec += 2f; use += 10f }
            "auto_nightly_patch" -> { sec += 10f; use += 7f }
        }
        when (dataPrivacy) {
            "full_cloud_sharing" -> { sec += 2f; use += 10f }
            "anonymized_telemetry" -> { sec += 7f; use += 9f }
            "local_only" -> { sec += 10f; use += 5f }
        }
        when (actionAuth) {
            "one_tap_always" -> { sec += 2f; use += 10f }
            "biometric_step_up" -> { sec += 10f; use += 6f }
        }

        securityScore = sec
        usabilityScore = use

        grade = when {
            sec >= 88f -> "A"
            sec >= 72f -> "B"
            sec >= 50f -> "C"
            else -> "D"
        }

        riskTier = when {
            sec >= 88f -> "Hardened Zero-Trust"
            sec >= 72f -> "Balanced Consumer Protection"
            sec >= 50f -> "Moderate Exposure Risk"
            else -> "High Vulnerability"
        }
    }

    LaunchedEffect(Unit) {
        recalculate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HaloGuard Gateway Setup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Participant ${NetworkModule.participantId} • Live Security Auditor", fontSize = 11.sp, color = BlueAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { showExitDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit Study", tint = DangerRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBackground)
            )
        },
        containerColor = SlateBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // STICKY TOP AUDITOR GAUGES CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LIVE AUDIT POSTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        BadgeGrade(grade)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Security Gauge
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Security Health", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("${securityScore.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = getScoreColor(securityScore))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val animatedSec by animateFloatAsState(targetValue = securityScore / 100f, animationSpec = tween(400))
                            val secColor by animateColorAsState(targetValue = getScoreColor(securityScore), animationSpec = tween(400))
                            LinearProgressIndicator(
                                progress = { animatedSec },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = secColor,
                                trackColor = BorderLight
                            )
                        }

                        // Usability Gauge
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Convenience Index", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("${usabilityScore.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val animatedUse by animateFloatAsState(targetValue = usabilityScore / 100f, animationSpec = tween(400))
                            LinearProgressIndicator(
                                progress = { animatedUse },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = BlueAccent,
                                trackColor = BorderLight
                            )
                        }
                    }

                    Text(
                        text = "Current Tier: $riskTier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = getScoreColor(securityScore)
                    )
                }
            }

            // SCROLLABLE CONFIGURATION SETTINGS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Select your preferred configuration trade-offs:", fontSize = 12.sp, color = TextSecondary)

                // 1. Device Authentication
                SettingCard(
                    title = "1. Device Authentication Credential",
                    options = listOf(
                        SettingOption("default_pin", "Default PIN (1234)", "Instant access. Vulnerable to dictionary brute-forcing.", isRisky = true),
                        SettingOption("pin_6", "6-Digit Custom PIN", "Standard security. Good balance for smartphone entry."),
                        SettingOption("passphrase_biometric", "Complex Passphrase + Biometrics", "Highest security. Annoying to type on mobile keyboards.")
                    ),
                    selectedKey = authCredential,
                    onSelect = {
                        val prev = authCredential
                        authCredential = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("auth_credential", it)
                        if (prev == "default_pin" && it != "default_pin") {
                            MobileTelemetryManager.recordWarningReversal("auth_credential", prev, it)
                        }
                    }
                )

                // 2. Transport Protocol Encryption
                SettingCard(
                    title = "2. Transport Protocol Encryption",
                    options = listOf(
                        SettingOption("mqtt_plain_1883", "Plaintext MQTT (Port 1883)", "Works with all cheap sensors. Transmits Wi-Fi & data unencrypted.", isRisky = true),
                        SettingOption("tls_8883", "Standard TLS Encrypted (Port 8883)", "Protects against local packet sniffing. Minor CPU overhead."),
                        SettingOption("mtls_strict", "Strict Mutual TLS (mTLS)", "Cryptographic certificates on hub and phone. Requires manual cert setup.")
                    ),
                    selectedKey = transportCrypto,
                    onSelect = {
                        val prev = transportCrypto
                        transportCrypto = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("transport_crypto", it)
                        if (prev == "mqtt_plain_1883" && it != "mqtt_plain_1883") {
                            MobileTelemetryManager.recordWarningReversal("transport_crypto", prev, it)
                        }
                    }
                )

                // 3. Network Architecture & Isolation
                SettingCard(
                    title = "3. Wi-Fi Network Architecture",
                    options = listOf(
                        SettingOption("shared_home_wifi", "Shared Home Wi-Fi Subnet", "Instant auto-discovery. If a bulb is hacked, home PCs are reachable.", isRisky = true),
                        SettingOption("isolated_vlan", "Isolated IoT / Guest VLAN", "Cordon off smart devices. Requires router VLAN support.")
                    ),
                    selectedKey = networkTopology,
                    onSelect = {
                        val prev = networkTopology
                        networkTopology = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("network_topology", it)
                        if (prev == "shared_home_wifi" && it != "shared_home_wifi") {
                            MobileTelemetryManager.recordWarningReversal("network_topology", prev, it)
                        }
                    }
                )

                // 4. Remote Access & Firewall
                SettingCard(
                    title = "4. Remote Inbound Access & Firewall",
                    options = listOf(
                        SettingOption("upnp_port", "UPnP Auto-Port Forward", "Control anywhere without setup. Exposes router port to internet bots.", isRisky = true),
                        SettingOption("cloud_relay", "Authenticated Cloud Relay Bridge", "Access from anywhere via cloud account. No open router ports."),
                        SettingOption("local_lan", "Local-Only LAN Mode", "Zero internet attack surface. Cannot control device away from home.")
                    ),
                    selectedKey = remoteExposure,
                    onSelect = {
                        val prev = remoteExposure
                        remoteExposure = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("remote_exposure", it)
                        if (prev == "upnp_port" && it != "upnp_port") {
                            MobileTelemetryManager.recordWarningReversal("remote_exposure", prev, it)
                        }
                    }
                )

                // 5. Firmware Patching
                SettingCard(
                    title = "5. Firmware Patch Management",
                    options = listOf(
                        SettingOption("manual_never", "Manual Updates Only", "100% uptime. Hub leaves known CVE exploits unpatched for months.", isRisky = true),
                        SettingOption("auto_nightly_patch", "Automated Security Patches", "Always patched against zero-days. May reboot at 3 AM.")
                    ),
                    selectedKey = firmwarePolicy,
                    onSelect = {
                        val prev = firmwarePolicy
                        firmwarePolicy = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("firmware_policy", it)
                        if (prev == "manual_never" && it != "manual_never") {
                            MobileTelemetryManager.recordWarningReversal("firmware_policy", prev, it)
                        }
                    }
                )

                // 6. Cloud Telemetry & Privacy
                SettingCard(
                    title = "6. Cloud Telemetry & Data Privacy",
                    options = listOf(
                        SettingOption("full_cloud_sharing", "Full Cloud Analytics & AI", "Enables cloud backup & predictive AI. Data stored externally.", isRisky = true),
                        SettingOption("anonymized_telemetry", "Anonymized Diagnostics Only", "Balanced telemetry without sharing household identity."),
                        SettingOption("local_only", "Strictly Local Storage Only", "Complete data sovereignty. No cloud backups or remote history.")
                    ),
                    selectedKey = dataPrivacy,
                    onSelect = {
                        val prev = dataPrivacy
                        dataPrivacy = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("data_privacy", it)
                        if (prev == "full_cloud_sharing" && it != "full_cloud_sharing") {
                            MobileTelemetryManager.recordWarningReversal("data_privacy", prev, it)
                        }
                    }
                )

                // 7. Action Authorization
                SettingCard(
                    title = "7. Action Authorization Threshold",
                    options = listOf(
                        SettingOption("one_tap_always", "One-Tap Control (Stay Logged In)", "Fastest control. Anyone holding the phone can control house.", isRisky = true),
                        SettingOption("biometric_step_up", "Step-Up Biometric / PIN on Critical Actions", "Requires fingerprint to change climate/rules. High protection.")
                    ),
                    selectedKey = actionAuth,
                    onSelect = {
                        val prev = actionAuth
                        actionAuth = it
                        recalculate()
                        MobileTelemetryManager.recordToggle("action_auth", it)
                        if (prev == "one_tap_always" && it != "one_tap_always") {
                            MobileTelemetryManager.recordWarningReversal("action_auth", prev, it)
                        }
                    }
                )
            }

            // BOTTOM DEPLOY BUTTON
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurface,
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            showScanDialog = true
                            isDeploying = true
                            scope.launch {
                                // Diagnostic scan simulation steps
                                scanProgress = 1
                                delay(700)
                                scanProgress = 2
                                delay(800)
                                scanProgress = 3
                                delay(600)

                                val settingsPayload = CommissionSettingsDto(
                                    authCredential = authCredential,
                                    transportCrypto = transportCrypto,
                                    networkTopology = networkTopology,
                                    remoteExposure = remoteExposure,
                                    firmwarePolicy = firmwarePolicy,
                                    dataPrivacy = dataPrivacy,
                                    actionAuth = actionAuth
                                )
                                val metricsPayload = CommissionMetricsDto(
                                    toggleCount = MobileTelemetryManager.toggleCount,
                                    warningReversals = MobileTelemetryManager.warningReversals
                                )

                                try {
                                    val res = NetworkModule.apiService.deployGateway(
                                        DeployRequest(settingsPayload, metricsPayload)
                                    )
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        auditReport = res.body()?.auditReport
                                    }
                                } catch (e: Exception) {
                                    // Fallback local report if server offline
                                    auditReport = AuditReportDto(
                                        participantId = NetworkModule.participantId,
                                        securityScore = securityScore,
                                        usabilityScore = usabilityScore,
                                        grade = grade,
                                        riskTier = riskTier,
                                        toggleCount = MobileTelemetryManager.toggleCount,
                                        warningReversals = MobileTelemetryManager.warningReversals
                                    )
                                } finally {
                                    isDeploying = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deploy & Commission Gateway", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    // PENETRATION SCAN & COMMISSIONING CERTIFICATE DIALOG
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss while scanning */ },
            title = {
                Text(
                    text = if (isDeploying) "Vulnerability Scan in Progress" else "Commissioning Certificate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                if (isDeploying) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScanRow("Testing Transport Encryption...", scanProgress >= 1)
                        ScanRow("Probing Remote Port Exposure...", scanProgress >= 2)
                        ScanRow("Evaluating Subnet Isolation...", scanProgress >= 3)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                } else {
                    auditReport?.let { report ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Audit Grade", fontSize = 12.sp, color = TextSecondary)
                                        BadgeGrade(report.grade)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Security Health", fontSize = 12.sp, color = TextSecondary)
                                        Text("${report.securityScore.toInt()}%", fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Convenience Index", fontSize = 12.sp, color = TextSecondary)
                                        Text("${report.usabilityScore.toInt()}%", fontWeight = FontWeight.Bold, color = BlueAccent)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Posture Classification", fontSize = 12.sp, color = TextSecondary)
                                        Text(report.riskTier, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (report.risks.isNotEmpty()) {
                                Text("Vulnerability Findings:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                report.risks.forEach { r ->
                                    Text("• ${r.name}: ${r.desc}", fontSize = 11.sp, color = TextSecondary)
                                }
                            } else {
                                Text("Zero high-severity vulnerabilities detected.", fontSize = 12.sp, color = SuccessGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isDeploying && auditReport != null) {
                    Button(
                        onClick = {
                            showScanDialog = false
                            onCommissionSuccess(auditReport!!)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Open Active Hub Dashboard")
                    }
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Study Session?") },
            text = { Text("Leaving mid-session records an abandonment event in the study database.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    MobileTelemetryManager.markAbandonment("voluntary_exit") {
                        onAbandoned()
                    }
                }) {
                    Text("Exit", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// HELPER COMPOSABLES
// ---------------------------------------------------------------------------
data class SettingOption(
    val key: String,
    val title: String,
    val desc: String,
    val isRisky: Boolean = false
)

@Composable
private fun SettingCard(
    title: String,
    options: List<SettingOption>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

            options.forEach { opt ->
                val isSelected = opt.key == selectedKey
                Surface(
                    onClick = { onSelect(opt.key) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) LightBlueBg else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isSelected) Modifier.border(1.5.dp, BlueAccent, RoundedCornerShape(8.dp)) else Modifier)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(opt.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = BlueAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(opt.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                            Text(opt.desc, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Nudge warning banner if risky choice active
            val currentSelected = options.find { it.key == selectedKey }
            if (currentSelected?.isRisky == true) {
                Surface(
                    color = WarningLight,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                        Text("Risk Alert: This choice trades off security for convenience.", fontSize = 11.sp, color = Color(0xFF92400E))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanRow(text: String, isComplete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        if (isComplete) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun BadgeGrade(grade: String) {
    val bg = when (grade) {
        "A" -> SuccessLight
        "B" -> Color(0xFFE0F2FE)
        "C" -> WarningLight
        else -> DangerLight
    }
    val fg = when (grade) {
        "A" -> SuccessGreen
        "B" -> BlueAccent
        "C" -> WarningAmber
        else -> DangerRed
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "GRADE $grade",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun getScoreColor(score: Float): Color {
    return when {
        score >= 72f -> SuccessGreen
        score >= 50f -> WarningAmber
        else -> DangerRed
    }
}
