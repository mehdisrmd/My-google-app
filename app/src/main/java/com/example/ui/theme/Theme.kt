package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
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

// Liquid Glass Purple color schemes: one violet seed, tonal surfaces,
// error reserved strictly for over-limit / danger states.
private val ShieldDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF22104A),
    primaryContainer = Color(0xFF4B3699),
    onPrimaryContainer = Color(0xFFE4DEFF),
    secondary = Color(0xFFCDBFFD),
    onSecondary = Color(0xFF2A2150),
    secondaryContainer = Color(0xFF241A4D),
    onSecondaryContainer = Color(0xFFE4DEFF),
    tertiary = AmberWarning,
    background = Color(0xFF0D0A17),
    onBackground = Color(0xFFF4F1FF),
    surface = Color(0xFF171228),
    onSurface = Color(0xFFF4F1FF),
    surfaceVariant = Color(0xFF1F1838),
    onSurfaceVariant = Color(0xFFA79FC2),
    outline = Color(0xFF3A2E66),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF4A1620),
    onErrorContainer = Color(0xFFFFC9C2)
)

private val ShieldLightColorScheme = lightColorScheme(
    primary = Color(0xFF6D4AC8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DEFF),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF6D5BD0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E2FD),
    onSecondaryContainer = Color(0xFF2A2150),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF7F5FC),
    onBackground = Color(0xFF1D1830),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1830),
    surfaceVariant = Color(0xFFF0EBFA),
    onSurfaceVariant = Color(0xFF6A6284),
    outline = Color(0xFFD5C8F2),
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
    content: @Composable () -> Unit
) {
    AppThemeState.isDarkTheme = darkTheme
    AppThemeState.isPersianLanguage = isPersianRtl

    val colorScheme = if (darkTheme) ShieldDarkColorScheme else ShieldLightColorScheme
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
