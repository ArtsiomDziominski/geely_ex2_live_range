package com.geely.ex2.range.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.geely.ex2.range.domain.model.AppThemeMode

val FlymeAccent = Color(0xFF0A6BFF)

// --- Light ---------------------------------------------------------------
private val BackgroundLight = Color(0xFFF4F5F8)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceContainerLight = Color(0xFFFAFBFD)
private val SurfaceContainerHighLight = Color(0xFFEFF1F5)
private val SurfaceVariantLight = Color(0xFFE7EAF0)
private val OnSurfaceLight = Color(0xFF111318)
private val OnSurfaceVariantLight = Color(0xFF5A6270)
private val OutlineLight = Color(0xFFBFC5D0)
private val OutlineVariantLight = Color(0xFFDCE0E7)

// --- Dark ----------------------------------------------------------------
private val BackgroundDark = Color(0xFF101218)
private val SurfaceDark = Color(0xFF1A1D25)
private val SurfaceContainerDark = Color(0xFF1F232C)
private val SurfaceContainerHighDark = Color(0xFF262A34)
private val SurfaceVariantDark = Color(0xFF2C313C)
private val OnSurfaceDark = Color(0xFFF1F3F7)
private val OnSurfaceVariantDark = Color(0xFFB7BECC)
private val OutlineDark = Color(0xFF565D6C)
private val OutlineVariantDark = Color(0xFF343945)

// --- Semantic ------------------------------------------------------------
val ChargingGreen = Color(0xFF1E8E3E)
val WarningAmber = Color(0xFFB26A00)
val LowSocRed = Color(0xFFC5221F)

private val ChargingGreenDark = Color(0xFF6DD58C)
private val WarningAmberDark = Color(0xFFFFC46B)
private val LowSocRedDark = Color(0xFFFF897D)

/**
 * Семантические и графиковые цвета, которых нет в [androidx.compose.material3.ColorScheme].
 * Поля [charging] / [warning] / [lowSoc] сохранены из прежней версии темы.
 */
@Immutable
data class RangeExtraColors(
    val charging: Color,
    val warning: Color,
    val lowSoc: Color,
    val success: Color,
    val info: Color,
    val chartSpeed: Color,
    val chartSoc: Color,
    val chartTemp: Color,
    val skeleton: Color,
)

private val LightExtraColors = RangeExtraColors(
    charging = ChargingGreen,
    warning = WarningAmber,
    lowSoc = LowSocRed,
    success = ChargingGreen,
    info = FlymeAccent,
    chartSpeed = Color(0xFF1565C0),
    chartSoc = Color(0xFF2E7D32),
    chartTemp = Color(0xFFC62828),
    skeleton = Color(0xFFE3E6EC),
)

private val DarkExtraColors = RangeExtraColors(
    charging = ChargingGreenDark,
    warning = WarningAmberDark,
    lowSoc = LowSocRedDark,
    success = ChargingGreenDark,
    info = Color(0xFF8FBCFF),
    chartSpeed = Color(0xFF68B0FF),
    chartSoc = Color(0xFF7ED694),
    chartTemp = Color(0xFFFF9C92),
    skeleton = Color(0xFF2A2F3A),
)

private val LocalExtraColors = staticCompositionLocalOf { LightExtraColors }

object RangeThemeColors {
    val extra: RangeExtraColors
        @Composable
        get() = LocalExtraColors.current
}

private val LightColors = lightColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF002F63),
    secondary = Color(0xFF4E5D70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE3EE),
    onSecondaryContainer = Color(0xFF0C1B2B),
    tertiary = ChargingGreen,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SurfaceContainerLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBCFF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFBAC6D9),
    onSecondary = Color(0xFF24313F),
    secondaryContainer = Color(0xFF3A4756),
    onSecondaryContainer = Color(0xFFD6E3F7),
    tertiary = ChargingGreenDark,
    onTertiary = Color(0xFF00391B),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF161920),
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = Color(0xFF31363F),
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
)

val RangeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun resolveDarkTheme(mode: AppThemeMode): Boolean {
    return when (mode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}

@Composable
fun RangeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extra = if (darkTheme) DarkExtraColors else LightExtraColors
    CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = RangeTypography,
            shapes = RangeShapes,
            content = content,
        )
    }
}
