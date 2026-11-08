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

// Liquid Glass Purple — Dark & White (Light) Adaptive Surface & Text Palette
// One violet accent family across the whole app; warm red/amber appear only
// for true warnings (over-limit, lockdown danger) via Material error roles.
val ObsidianBg: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF0D0A17) else Color(0xFFF7F5FC)

val ObsidianSurface: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF171228) else Color(0xFFFFFFFF)

val ObsidianCard: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF1F1838) else Color(0xFFF0EBFA)

val ObsidianElevated: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF2A2150) else Color(0xFFE3DAF7)

val ObsidianBorder: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF3A2E66) else Color(0xFFD5C8F2)

// Primary Violet Accent (primary actions; formerly crimson-toned call sites)
val CrimsonPrimary = Color(0xFF8B76E8)
val CrimsonGlow = Color(0xFFB79CFF)
val CrimsonDark = Color(0xFF4B3699)

// Protected / Active Shield Accent (calm lavender in the purple system)
val EmeraldShield = Color(0xFFA78BFA)
val EmeraldBright = Color(0xFFCDBFFD)
val EmeraldDark = Color(0xFF6D5BD0)
val EmeraldSurface: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF241A4D) else Color(0xFFE9E2FD)

// Telemetry & Focus Accents (periwinkle within the same violet family)
val CyanTelemetry: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF9D8CFF) else Color(0xFF6D4AC8)

val IndigoAccent = Color(0xFF6D5BD0)
val VioletStreak = Color(0xFFB79CFF)

// Warning & Timer Accents (kept amber: warnings must read as warnings)
val AmberWarning = Color(0xFFFFAB00)

val ObsidianTextPrimary: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFFF4F1FF) else Color(0xFF1D1830)

val ObsidianTextSecondary: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFFA79FC2) else Color(0xFF6A6284)
