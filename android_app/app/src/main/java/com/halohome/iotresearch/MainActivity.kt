package com.halohome.iotresearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.halohome.iotresearch.telemetry.MobileTelemetryManager
import com.halohome.iotresearch.ui.navigation.AppNavigation
import com.halohome.iotresearch.ui.theme.HaloHomeTheme
import com.halohome.iotresearch.ui.theme.SlateBackground

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HaloHomeTheme {
                // Monitor App Lifecycle to catch drop-offs when app is minimized or closed
                DisposableEffect(this) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            MobileTelemetryManager.onAppBackgrounded()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MobileTelemetryManager.stopHeartbeat()
    }
}
