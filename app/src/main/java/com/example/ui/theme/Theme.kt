package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// Liquid Glass color schemes, built from the active seasonal palette
// (default PURPLE_GLASS = the original violet system). Error colors stay
// fixed across palettes: danger must always read as danger.
private fun shieldDarkColorScheme(palette: SeasonPaletteSet): ColorScheme = darkColorScheme(
    primary = palette.schemePrimary,
    onPrimary = palette.schemeOnPrimary,
    primaryContainer = palette.schemePrimaryContainer,
    onPrimaryContainer = palette.schemeOnPrimaryContainer,
    secondary = palette.schemeSecondary,
    onSecondary = palette.schemeOnSecondary,
    secondaryContainer = palette.schemeSecondaryContainer,
    onSecondaryContainer = palette.schemeOnSecondaryContainer,
    tertiary = palette.schemeTertiary,
    background = palette.bg,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.card,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFC9C2)
)

private fun shieldLightColorScheme(palette: SeasonPaletteSet): ColorScheme = lightColorScheme(
    primary = palette.schemePrimary,
    onPrimary = palette.schemeOnPrimary,
    primaryContainer = palette.schemePrimaryContainer,
    onPrimaryContainer = palette.schemeOnPrimaryContainer,
    secondary = palette.schemeSecondary,
    onSecondary = palette.schemeOnSecondary,
    secondaryContainer = palette.schemeSecondaryContainer,
    onSecondaryContainer = palette.schemeOnSecondaryContainer,
    tertiary = palette.schemeTertiary,
    background = palette.bg,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.card,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF7A1010)
)

// iOS-inspired generous rounding (Liquid Glass surfaces are soft, never sharp).
private val ShieldShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = AppThemeState.isDarkTheme,
    isPersianRtl: Boolean = AppThemeState.isPersianLanguage,
    seasonalTheme: SeasonalTheme = AppThemeState.seasonalTheme,
    content: @Composable () -> Unit
) {
    AppThemeState.isDarkTheme = darkTheme
    AppThemeState.isPersianLanguage = isPersianRtl
    AppThemeState.seasonalTheme = seasonalTheme

    val palette = SeasonalPalettes.getValue(seasonalTheme)
    val colorScheme = if (darkTheme) {
        shieldDarkColorScheme(palette.dark)
    } else {
        shieldLightColorScheme(palette.light)
    }
    val layoutDirection = if (isPersianRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ShieldShapes,
            content = content
        )
    }
}
