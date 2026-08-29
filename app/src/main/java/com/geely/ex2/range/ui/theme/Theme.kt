package com.geely.ex2.range.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val FlymeAccent = Color(0xFF007AFF)
val FlymeBackground = Color(0xFF1A1C24)
val FlymeSurface = Color(0xFF2A2D37)
val FlymeSurfaceVariant = Color(0xFF343845)
val FlymeOnSurface = Color(0xFFF4F6FA)
val FlymeMuted = Color(0x9EF4F6FA)

private val DarkColors = darkColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    background = FlymeBackground,
    surface = FlymeSurface,
    surfaceVariant = FlymeSurfaceVariant,
    onSurface = FlymeOnSurface,
    onSurfaceVariant = FlymeMuted,
    outlineVariant = Color(0xFF3E4250),
)

@Composable
fun RangeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
