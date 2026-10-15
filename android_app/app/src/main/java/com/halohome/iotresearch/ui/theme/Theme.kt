package com.halohome.iotresearch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = CardSurface,
    primaryContainer = LightBlueBg,
    onPrimaryContainer = NavyPrimary,
    background = SlateBackground,
    surface = CardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderLight
)

@Composable
fun HaloHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
