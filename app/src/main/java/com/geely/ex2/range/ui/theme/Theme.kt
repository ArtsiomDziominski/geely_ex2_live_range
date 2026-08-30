package com.geely.ex2.range.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val FlymeAccent = Color(0xFF007AFF)

private val FlymeBackgroundLight = Color(0xFFF2F3F5)
private val FlymeSurfaceLight = Color(0xFFFFFFFF)
private val FlymeSurfaceVariantLight = Color(0xFFE8EAEE)
private val FlymeOnSurfaceLight = Color(0xFF111318)
private val FlymeOnSurfaceVariantLight = Color(0xFF6B7280)
private val FlymeOutlineLight = Color(0xFFD5D8DE)

private val FlymeBackgroundDark = Color(0xFF1A1C24)
private val FlymeSurfaceDark = Color(0xFF2A2D37)
private val FlymeSurfaceVariantDark = Color(0xFF343845)
private val FlymeOnSurfaceDark = Color(0xFFF4F6FA)
private val FlymeOnSurfaceVariantDark = Color(0x9EF4F6FA)
private val FlymeOutlineDark = Color(0xFF3E4250)

val ChargingGreen = Color(0xFF34C759)
val WarningAmber = Color(0xFFE6B422)

@Immutable
data class RangeExtraColors(
    val charging: Color,
    val warning: Color,
)

private val LocalExtraColors = staticCompositionLocalOf {
    RangeExtraColors(charging = ChargingGreen, warning = WarningAmber)
}

object RangeThemeColors {
    val extra: RangeExtraColors
        @Composable
        get() = LocalExtraColors.current
}

private val LightColors = lightColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    background = FlymeBackgroundLight,
    surface = FlymeSurfaceLight,
    surfaceVariant = FlymeSurfaceVariantLight,
    onSurface = FlymeOnSurfaceLight,
    onSurfaceVariant = FlymeOnSurfaceVariantLight,
    outline = FlymeOutlineLight,
    outlineVariant = FlymeOutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    background = FlymeBackgroundDark,
    surface = FlymeSurfaceDark,
    surfaceVariant = FlymeSurfaceVariantDark,
    onSurface = FlymeOnSurfaceDark,
    onSurfaceVariant = FlymeOnSurfaceVariantDark,
    outline = FlymeOutlineDark,
    outlineVariant = FlymeOutlineDark,
)

@Composable
fun RangeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extra = RangeExtraColors(charging = ChargingGreen, warning = WarningAmber)
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
