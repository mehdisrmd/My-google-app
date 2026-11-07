package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val ShieldDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldShield,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF062E1E),
    onSecondaryContainer = EmeraldBright,
    tertiary = AmberWarning,
    background = Color(0xFF060911),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0E1422),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF151E32),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF283554)
)

private val ShieldLightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4EC),
    onPrimaryContainer = CrimsonDark,
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
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
            content = content
        )
    }
}
