package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Why an enum instead of named vals: the persisted setting is a single
// string key ("default" / "purple" / "spring" / "summer" / "autumn" /
// "winter"); an enum forces the when() below to be exhaustive, so adding a
// season is a compile error until StreakCalendarScreen covers it too.

enum class SeasonalTheme {
    // Purple glass is the theme envelopes when a season заяр
    PURPLE_GLASS,
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER
}

/** One full set of theme colours for a season or festive theme. */
data class SeasonPaletteSet(
    // Light / day.
    val lightHeading: Color,
    val lightBody: Color,
    val lightMuted: Color,
    val lightFill: Color,
    val lightAltFill: Color,
    val lightCard: Color,
    val lightGlyph: Color,
    val lightGlowA: Color,
    val lightGlowB: Color,
    val lightGlowC: Color,
    val lightSchemePrimary: Color,
    val lightSchemeOnPrimary: Color,
    val lightSchemeTertiary: Color,
    // Dark / night.
    val darkHeading: Color,
    val darkBody: Color,
    val darkMuted: Color,
    val darkFill: Color,
    val darkAltFill: Color,
    val darkCard: Color,
    val darkGlyph: Color,
    val darkGlowA: Color,
    val darkGlowB: Color,
    val darkGlowC: Color,
    val darkSchemePrimary: Color,
    val darkSchemeOnPrimary: Color,
    val darkSchemeTertiary: Color
)

/** A season's full palette, composed of a light and a dark [SeasonPaletteSet]. */
data class SeasonPalette(
    val light: SeasonPaletteSet,
    val dark: SeasonPaletteSet,
)

/** Light/dark accessor: use the set matching [isDark]. */
fun SeasonPalette.activeSeasonPaletteSet(isDark: Boolean): SeasonPaletteSet =
    if (isDark) dark else light

// --- Palettes ---

private val PurpleGlassPalette = SeasonPalette(
    light = SeasonPaletteSet(
        heading = Color(0xFF6B43C2),
        body = Color(0xFF3A2B58),
        muted = Color(0xFF75688E),
        fill = Color(0xFFF0EBF8),
        altFill = Color(0xFFE6DCF2),
        card = Color(0xFFFCFBFD),
        glyph = Color(0xFFFDFBFF),
        glowA = Color(0xFF8E6AD4),
        glowB = Color(0xFFDEA63E),
        glowC = Color(0xFFA973EF),
        schemePrimary = Color(0xFF8B5CF6),
        schemeOnPrimary = Color(0xFFFFFFFF),
        schemeTertiary = Color(0xFFB98AE8),
    ),
    dark = SeasonPaletteSet(
        heading = Color(0xFFC3A6F5),
        body = Color(0xFFE6E0F2),
        muted = Color(0xFFAD9FC7),
        fill = Color(0xFF1A1426),
        altFill = Color(0xFF211A33),
        card = Color(0xFF14101F),
        glyph = Color(0xFF14101F),
        glowA = Color(0xFF6B43C2),
        glowB = Color(0xFF9B6A2E),
        glowC = Color(0xFF8B5CF6),
        schemePrimary = Color(0xFF9576E8),
        schemeOnPrimary = Color(0xFF2A1A4E),
        schemeTertiary = Color(0xFFB381D2),
    )
)

private val SpringPalette = SeasonPalette(
    light = SeasonPaletteSet(
        heading = Color(0xFF2F6B3A),
        body = Color(0xFF2E3B2A),
        muted = Color(0xFF6F7A68),
        fill = Color(0xFFEDF5EA),
        altFill = Color(0xFFDDEAD4),
        card = Color(0xFFFBFDF9),
        glyph = Color(0xFFFBFDF9),
        glowA = Color(0xFF8FBC8A),
        glowB = Color(0xFFE3C878),
        glowC = Color(0xFFA8D8A8),
        schemePrimary = Color(0xFF4CAF50),
        schemeOnPrimary = Color(0xFFFFFFFF),
        schemeTertiary = Color(0xFF8BC34A),
    ),
    dark = SeasonPaletteSet(
        heading = Color(0xFF9BD49A),
        body = Color(0xFFE3EBDD),
        muted = Color(0xFFA1B39A),
        fill = Color(0xFF121A12),
        altFill = Color(0xFF1A241A),
        card = Color(0xFF0F140F),
        glyph = Color(0xFF0F140F),
        glowA = Color(0xFF4A7A48),
        glowB = Color(0xFF8F7420),
        glowC = Color(0xFF47753F),
        schemePrimary = Color(0xFF6FCF73),
        schemeOnPrimary = Color(0xFF1B3A1C),
        schemeTertiary = Color(0xFFA5D6A7),
    )
)

private val SummerPalette = SeasonPalette(
    light = SeasonPaletteSet(
        heading = Color(0xFF5B6212),
        body = Color(0xFF3A3F1A),
        muted = Color(0xFF76795F),
        fill = Color(0xFFF6F4DE),
        altFill = Color(0xFFE9E4BC),
        card = Color(0xFFFEFDF6),
        glyph = Color(0xFFFEFDF6),
        glowA = Color(0xFFE3C878),
        glowB = Color(0xFF7FB4C8),
        glowC = Color(0xFFA8D8A8),
        schemePrimary = Color(0xFFFFC107),
        schemeOnPrimary = Color(0xFF3A2B00),
        schemeTertiary = Color(0xFF03A9F4),
    ),
    dark = SeasonPaletteSet(
        heading = Color(0xFFD8DE8A),
        body = Color(0xFFEBEBC8),
        muted = Color(0xFFA8AD7F),
        fill = Color(0xFF1A1B0E),
        altFill = Color(0xFF25260F),
        card = Color(0xFF12130A),
        glyph = Color(0xFF12130A),
        glowA = Color(0xFF9B8F2E),
        glowB = Color(0xFF336B84),
        glowC = Color(0xFFFFC107),
        schemePrimary = Color(0xFFFFD54F),
        schemeOnPrimary = Color(0xFF3A2B00),
        schemeTertiary = Color(0xFF4FC3F7),
    )
)

private val AutumnPalette = SeasonPalette(
    light = SeasonPaletteSet(
        heading = Color(0xFF9C4A1E),
        body = Color(0xFF3E2D24),
        muted = Color(0xFF7C6D62),
        fill = Color(0xFFF9F0E9),
        altFill = Color(0xFFF0DFCF),
        card = Color(0xFFFEFBF8),
        glyph = Color(0xFFFEFBF8),
        glowA = Color(0xFFFF8C42),
        glowB = Color(0xFFD6A94B),
        glowC = Color(0xFFE3B76E),
        schemePrimary = Color(0xFFFF5722),
        schemeOnPrimary = Color(0xFFFFFFFF),
        schemeTertiary = Color(0xFFFF9800),
    ),
    dark = SeasonPaletteSet(
        heading = Color(0xFFFF9E6E),
        body = Color(0xFFE8DCD2),
        muted = Color(0xFFAC9A8C),
        fill = Color(0xFF1D1510),
        altFill = Color(0xFF2A1D16),
        card = Color(0xFF14100D),
        glyph = Color(0xFF14100D),
        glowA = Color(0xFF9C4A1E),
        glowB = Color(0xFF7D5E24),
        glowC = Color(0xFFA05F22),
        schemePrimary = Color(0xFFFF7B47),
        schemeOnPrimary = Color(0xFF4A1900),
        schemeTertiary = Color(0xFFFFB74D),
    )
)

private val WinterPalette = SeasonPalette(
    light = SeasonPaletteSet(
        heading = Color(0xFF1E5A7A),
        body = Color(0xFF25333B),
        muted = Color(0xFF647A85),
        fill = Color(0xFFEAF3F6),
        altFill = Color(0xFFD9E8EE),
        card = Color(0xFFFBFDFE),
        glyph = Color(0xFFFBFDFE),
        glowA = Color(0xFF7FB4C8),
        glowB = Color(0xFFB8D4E0),
        glowC = Color(0xFF5A8FA8),
        schemePrimary = Color(0xFF3D6E99),
        schemeOnPrimary = Color(0xFFFFFFFF),
        schemeTertiary = Color(0xFF8F7420),
    ),
    dark = SeasonPaletteSet(
        heading = Color(0xFF8FC8DE),
        body = Color(0xFFDDEBEF),
        muted = Color(0xFF9DB0B8),
        fill = Color(0xFF0E1A20),
        altFill = Color(0xFF16242B),
        card = Color(0xFF0B1216),
        glyph = Color(0xFF0B1216),
        glowA = Color(0xFF1E5A7A),
        glowB = Color(0xFF5A8FA8),
        glowC = Color(0xFF2E7A9B),
        schemePrimary = Color(0xFF6FA8C6),
        schemeOnPrimary = Color(0xFF0A2E45),
        schemeTertiary = Color(0xFF8F7420),
    )
)

val SeasonalPalettes: Map<SeasonalTheme, SeasonPalette> = mapOf(
    SeasonalTheme.PURPLE_GLASS to PurpleGlassPalette,
    SeasonalTheme.SPRING to SpringPalette,
    SeasonalTheme.SUMMER to SummerPalette,
    SeasonalTheme.AUTUMN to AutumnPalette,
    SeasonalTheme.WINTER to WinterPalette
)
