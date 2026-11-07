package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Reactive global theme state so all screens and cards automatically adapt
 * when switching between Dark Theme and White (Light) Theme.
 */
object AppThemeState {
    var isDarkTheme by mutableStateOf(true)
    var isPersianLanguage by mutableStateOf(false)
}

// Dark & White (Light) Adaptive Surface & Text Palette
val ObsidianBg: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF060911) else Color(0xFFF8FAFC)

val ObsidianSurface: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF0E1422) else Color(0xFFFFFFFF)

val ObsidianCard: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF151E32) else Color(0xFFF1F5F9)

val ObsidianElevated: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF1E2942) else Color(0xFFE2E8F0)

val ObsidianBorder: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF283554) else Color(0xFFCBD5E1)

// Primary Crimson / Alert Accent (Lockdown & Blocked States)
val CrimsonPrimary = Color(0xFFFF2A5F)
val CrimsonGlow = Color(0xFFFF5C8A)
val CrimsonDark = Color(0xFF9E0B33)

// Secondary / Active Shield Accent (Protected & Safe States)
val EmeraldShield = Color(0xFF00E676)
val EmeraldBright = Color(0xFF69F0AE)
val EmeraldDark = Color(0xFF00A152)
val EmeraldSurface: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF062E1E) else Color(0xFFDCFCE7)

// Telemetry & Focus Accents
val CyanTelemetry: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF00E5FF) else Color(0xFF0284C7)

val IndigoAccent = Color(0xFF6366F1)
val VioletStreak = Color(0xFFA855F7)

// Warning & Timer Accents
val AmberWarning = Color(0xFFFFAB00)

val ObsidianTextPrimary: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)

val ObsidianTextSecondary: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
