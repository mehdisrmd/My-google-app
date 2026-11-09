package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Reactive global theme state so all screens and cards automatically adapt
 * when switching between Dark Theme and White (Light) Theme, between
 * Persian/English, and between the seasonal palettes (see [SeasonalTheme]).
 */
object AppThemeState {
    var isDarkTheme by mutableStateOf(true)
    var isPersianLanguage by mutableStateOf(false)
    var seasonalTheme by mutableStateOf(SeasonalTheme.PURPLE_GLASS)
}

// Liquid Glass surface & text palette — values come from the active seasonal
// palette (default PURPLE_GLASS = the original iOS-26 Liquid Glass purple).
// One accent family per season across the whole app; warm red/amber appear
// only for true warnings (over-limit, lockdown danger) via Material error
// roles and the semantic streak colors below.
val ObsidianBg: Color
    get() = activeSeasonPaletteSet.bg

val ObsidianSurface: Color
    get() = activeSeasonPaletteSet.surface

val ObsidianCard: Color
    get() = activeSeasonPaletteSet.card

val ObsidianElevated: Color
    get() = activeSeasonPaletteSet.elevated

val ObsidianBorder: Color
    get() = activeSeasonPaletteSet.border

// Primary Accent (primary actions; formerly crimson-toned call sites)
val CrimsonPrimary: Color
    get() = activeSeasonPaletteSet.primary

val CrimsonGlow: Color
    get() = activeSeasonPaletteSet.primaryGlow

val CrimsonDark: Color
    get() = activeSeasonPaletteSet.primaryDark

// Protected / Active Shield Accent (calm accent within the active palette)
val EmeraldShield: Color
    get() = activeSeasonPaletteSet.shield

val EmeraldBright: Color
    get() = activeSeasonPaletteSet.shieldBright

val EmeraldDark: Color
    get() = activeSeasonPaletteSet.shieldDark

val EmeraldSurface: Color
    get() = activeSeasonPaletteSet.shieldSurface

// Telemetry & Focus Accents
val CyanTelemetry: Color
    get() = activeSeasonPaletteSet.telemetry

val IndigoAccent: Color
    get() = activeSeasonPaletteSet.shieldDark

val VioletStreak: Color
    get() = activeSeasonPaletteSet.primaryGlow

// Warning & Timer Accents (kept amber: warnings must read as warnings)
val AmberWarning = Color(0xFFFFAB00)

val ObsidianTextPrimary: Color
    get() = activeSeasonPaletteSet.textPrimary

val ObsidianTextSecondary: Color
    get() = activeSeasonPaletteSet.textSecondary

// Streak Calendar semantic colors (Feature: Streak Calendar Colors).
// Deliberately NOT season-tinted: a clean day must always read green, a
// relapse always red, and today always gray — in every theme and mode.
// iOS-system-inspired shades keep them calm next to the glass surfaces.
val StreakTodayGray: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF9E9CAB) else Color(0xFF75738A)

val StreakCleanGreen: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFF3ED97E) else Color(0xFF1E9E50)

val StreakRelapseRed: Color
    get() = if (AppThemeState.isDarkTheme) Color(0xFFFF6F61) else Color(0xFFD63A2F)
