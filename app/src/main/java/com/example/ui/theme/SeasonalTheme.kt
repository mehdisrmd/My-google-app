package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Seasonal theme palettes (Feature: Seasonal Themes).
 *
 * [PURPLE_GLASS] is the original iOS-26 "Liquid Glass" purple look and stays
 * the default — its values are byte-for-byte the pre-seasonal hardcoded ones,
 * so existing users see zero visual change until they pick a season.
 *
 * The four seasons only swap palette *values* (surfaces, accents, glows and
 * the Material 3 scheme seeds) through the exact same theme layer; no
 * component structure changes. Translucency / glass surfaces are untouched:
 * every surface keeps the same role, just tinted by the season.
 */
enum class SeasonalTheme {
    PURPLE_GLASS,
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    companion object {
        fun fromName(name: String?): SeasonalTheme =
            entries.firstOrNull { it.name == name } ?: PURPLE_GLASS
    }
}

/** One full set of palette values for a single (theme × dark/light) combo. */
data class SeasonPaletteSet(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val elevated: Color,
    val border: Color,
    val primary: Color,
    val primaryGlow: Color,
    val primaryDark: Color,
    val shield: Color,
    val shieldBright: Color,
    val shieldDark: Color,
    val shieldSurface: Color,
    val telemetry: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color,
    // Material 3 color-scheme seeds for this palette/mode.
    val schemePrimary: Color,
    val schemeOnPrimary: Color,
    val schemePrimaryContainer: Color,
    val schemeOnPrimaryContainer: Color,
    val schemeSecondary: Color,
    val schemeOnSecondary: Color,
    val schemeSecondaryContainer: Color,
    val schemeOnSecondaryContainer: Color,
    val schemeTertiary: Color
)

data class SeasonPalette(
    val dark: SeasonPaletteSet,
    val light: SeasonPaletteSet
)

/** The palette set active right now (selected theme × current dark/light). */
val activeSeasonPaletteSet: SeasonPaletteSet
    get() {
        val palette = SeasonalPalettes.getValue(AppThemeState.seasonalTheme)
        return if (AppThemeState.isDarkTheme) palette.dark else palette.light
    }

private val PurpleGlassPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF0D0A17),
        surface = Color(0xFF171228),
        card = Color(0xFF1F1838),
        elevated = Color(0xFF2A2150),
        border = Color(0xFF3A2E66),
        primary = Color(0xFF8B76E8),
        primaryGlow = Color(0xFFB79CFF),
        primaryDark = Color(0xFF4B3699),
        shield = Color(0xFFA78BFA),
        shieldBright = Color(0xFFCDBFFD),
        shieldDark = Color(0xFF6D5BD0),
        shieldSurface = Color(0xFF241A4D),
        telemetry = Color(0xFF9D8CFF),
        textPrimary = Color(0xFFF4F1FF),
        textSecondary = Color(0xFFA79FC2),
        glowA = Color(0xFF6D5BD0),
        glowB = Color(0xFFB45CFF),
        glowC = Color(0xFF5A6EFF),
        schemePrimary = Color(0xFFA78BFA),
        schemeOnPrimary = Color(0xFF22104A),
        schemePrimaryContainer = Color(0xFF4B3699),
        schemeOnPrimaryContainer = Color(0xFFE4DEFF),
        schemeSecondary = Color(0xFFCDBFFD),
        schemeOnSecondary = Color(0xFF2A2150),
        schemeSecondaryContainer = Color(0xFF241A4D),
        schemeOnSecondaryContainer = Color(0xFFE4DEFF),
        schemeTertiary = Color(0xFFFFAB00)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFF7F5FC),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFF0EBFA),
        elevated = Color(0xFFE3DAF7),
        border = Color(0xFFD5C8F2),
        primary = Color(0xFF8B76E8),
        primaryGlow = Color(0xFFB79CFF),
        primaryDark = Color(0xFF4B3699),
        shield = Color(0xFFA78BFA),
        shieldBright = Color(0xFFCDBFFD),
        shieldDark = Color(0xFF6D5BD0),
        shieldSurface = Color(0xFFE9E2FD),
        telemetry = Color(0xFF6D4AC8),
        textPrimary = Color(0xFF1D1830),
        textSecondary = Color(0xFF6A6284),
        glowA = Color(0xFF6D5BD0),
        glowB = Color(0xFFB45CFF),
        glowC = Color(0xFF5A6EFF),
        schemePrimary = Color(0xFF6D4AC8),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFE4DEFF),
        schemeOnPrimaryContainer = Color(0xFF2E1065),
        schemeSecondary = Color(0xFF6D5BD0),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFE9E2FD),
        schemeOnSecondaryContainer = Color(0xFF2A2150),
        schemeTertiary = Color(0xFFD97706)
    )
)

// Spring: fresh leaf greens with blossom-pink shield accents and spring sky telemetry.
private val SpringPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF0B1512),
        surface = Color(0xFF12241C),
        card = Color(0xFF183326),
        elevated = Color(0xFF21452F),
        border = Color(0xFF2E5C40),
        primary = Color(0xFF7ED491),
        primaryGlow = Color(0xFFFFA8C5),
        primaryDark = Color(0xFF2E7D4F),
        shield = Color(0xFFF49AC1),
        shieldBright = Color(0xFFFFC7DC),
        shieldDark = Color(0xFFB04A77),
        shieldSurface = Color(0xFF2A1A24),
        telemetry = Color(0xFF7FC8F8),
        textPrimary = Color(0xFFF2FBF4),
        textSecondary = Color(0xFFA8C3AE),
        glowA = Color(0xFF43A047),
        glowB = Color(0xFFF48FB1),
        glowC = Color(0xFF7FC8F8),
        schemePrimary = Color(0xFF7ED491),
        schemeOnPrimary = Color(0xFF0B2E17),
        schemePrimaryContainer = Color(0xFF2E7D4F),
        schemeOnPrimaryContainer = Color(0xFFD9F5DE),
        schemeSecondary = Color(0xFFF49AC1),
        schemeOnSecondary = Color(0xFF3A0E22),
        schemeSecondaryContainer = Color(0xFF2A1A24),
        schemeOnSecondaryContainer = Color(0xFFFFD9E8),
        schemeTertiary = Color(0xFFFFAB00)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFF4FBF2),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFE7F5E4),
        elevated = Color(0xFFD6EDCB),
        border = Color(0xFFBCDDB4),
        primary = Color(0xFF2E7D32),
        primaryGlow = Color(0xFFF48FB1),
        primaryDark = Color(0xFF1B5E20),
        shield = Color(0xFFD81B60),
        shieldBright = Color(0xFFF8BBD0),
        shieldDark = Color(0xFFAD1457),
        shieldSurface = Color(0xFFFCE4EC),
        telemetry = Color(0xFF0288D1),
        textPrimary = Color(0xFF12261A),
        textSecondary = Color(0xFF56705C),
        glowA = Color(0xFF43A047),
        glowB = Color(0xFFF48FB1),
        glowC = Color(0xFF7FC8F8),
        schemePrimary = Color(0xFF2E7D32),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFC8EAC9),
        schemeOnPrimaryContainer = Color(0xFF0B2E17),
        schemeSecondary = Color(0xFFD81B60),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFFCE4EC),
        schemeOnSecondaryContainer = Color(0xFF4A0E24),
        schemeTertiary = Color(0xFFD97706)
    )
)

// Summer: warm sunny amber primary with clear-sky blue shield accents.
private val SummerPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF0D1420),
        surface = Color(0xFF14202F),
        card = Color(0xFF1B2C40),
        elevated = Color(0xFF27405C),
        border = Color(0xFF33536F),
        primary = Color(0xFFFFC94D),
        primaryGlow = Color(0xFFFF9E57),
        primaryDark = Color(0xFFB97F06),
        shield = Color(0xFF4FC3F7),
        shieldBright = Color(0xFFA5E3FF),
        shieldDark = Color(0xFF0288D1),
        shieldSurface = Color(0xFF12303F),
        telemetry = Color(0xFF64DFC2),
        textPrimary = Color(0xFFFFF9EC),
        textSecondary = Color(0xFFA9BCCE),
        glowA = Color(0xFFFFB300),
        glowB = Color(0xFFFF7043),
        glowC = Color(0xFF29B6F6),
        schemePrimary = Color(0xFFFFC94D),
        schemeOnPrimary = Color(0xFF332400),
        schemePrimaryContainer = Color(0xFF8A6100),
        schemeOnPrimaryContainer = Color(0xFFFFEFC4),
        schemeSecondary = Color(0xFF4FC3F7),
        schemeOnSecondary = Color(0xFF06283D),
        schemeSecondaryContainer = Color(0xFF12303F),
        schemeOnSecondaryContainer = Color(0xFFCFEFFF),
        schemeTertiary = Color(0xFFFFAB00)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFFFFBF0),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFFFF3D6),
        elevated = Color(0xFFFFE7AE),
        border = Color(0xFFF2D98C),
        primary = Color(0xFF9C6D00),
        primaryGlow = Color(0xFFFF8A3D),
        primaryDark = Color(0xFF7A5200),
        shield = Color(0xFF0288D1),
        shieldBright = Color(0xFF81D4FA),
        shieldDark = Color(0xFF01579B),
        shieldSurface = Color(0xFFE1F5FE),
        telemetry = Color(0xFF00897B),
        textPrimary = Color(0xFF2A2118),
        textSecondary = Color(0xFF7A6A54),
        glowA = Color(0xFFFFB300),
        glowB = Color(0xFFFF7043),
        glowC = Color(0xFF29B6F6),
        schemePrimary = Color(0xFF8A6100),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFFFE7AE),
        schemeOnPrimaryContainer = Color(0xFF332400),
        schemeSecondary = Color(0xFF0288D1),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFE1F5FE),
        schemeOnSecondaryContainer = Color(0xFF06283D),
        schemeTertiary = Color(0xFFD97706)
    )
)

// Autumn: amber / rust / warm-brown harvest tones.
private val AutumnPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF180F08),
        surface = Color(0xFF26180F),
        card = Color(0xFF332216),
        elevated = Color(0xFF472F1B),
        border = Color(0xFF5C3D21),
        primary = Color(0xFFFF9E45),
        primaryGlow = Color(0xFFFFC46B),
        primaryDark = Color(0xFF8A4B12),
        shield = Color(0xFFE2725B),
        shieldBright = Color(0xFFFFAB91),
        shieldDark = Color(0xFFB34724),
        shieldSurface = Color(0xFF33190D),
        telemetry = Color(0xFFD9A441),
        textPrimary = Color(0xFFFFF4E4),
        textSecondary = Color(0xFFC4A582),
        glowA = Color(0xFFE65100),
        glowB = Color(0xFFFF8F00),
        glowC = Color(0xFF8D6E63),
        schemePrimary = Color(0xFFFF9E45),
        schemeOnPrimary = Color(0xFF38200A),
        schemePrimaryContainer = Color(0xFF8A4B12),
        schemeOnPrimaryContainer = Color(0xFFFFDEC2),
        schemeSecondary = Color(0xFFE2725B),
        schemeOnSecondary = Color(0xFF38100A),
        schemeSecondaryContainer = Color(0xFF33190D),
        schemeOnSecondaryContainer = Color(0xFFFFD9CC),
        schemeTertiary = Color(0xFFFFAB00)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFFBF3E6),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFF7E8D2),
        elevated = Color(0xFFF0D8B4),
        border = Color(0xFFE3C491),
        primary = Color(0xFFB3540E),
        primaryGlow = Color(0xFFE8A33D),
        primaryDark = Color(0xFF7A3B06),
        shield = Color(0xFFC2451E),
        shieldBright = Color(0xFFFFAB91),
        shieldDark = Color(0xFF93330F),
        shieldSurface = Color(0xFFFBE3D3),
        telemetry = Color(0xFF9C7A12),
        textPrimary = Color(0xFF2E1B0C),
        textSecondary = Color(0xFF8A6B4D),
        glowA = Color(0xFFE65100),
        glowB = Color(0xFFFF8F00),
        glowC = Color(0xFF8D6E63),
        schemePrimary = Color(0xFFB3540E),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFFFDDB3),
        schemeOnPrimaryContainer = Color(0xFF38200A),
        schemeSecondary = Color(0xFFC2451E),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFFBE3D3),
        schemeOnSecondaryContainer = Color(0xFF4A1503),
        schemeTertiary = Color(0xFFD97706)
    )
)

// Winter: icy blues and silver, calm and bright.
private val WinterPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF0A121E),
        surface = Color(0xFF111E30),
        card = Color(0xFF182A42),
        elevated = Color(0xFF22385A),
        border = Color(0xFF2F4A73),
        primary = Color(0xFF7FD4FF),
        primaryGlow = Color(0xFFCFE8FF),
        primaryDark = Color(0xFF33608F),
        shield = Color(0xFFB0C4DE),
        shieldBright = Color(0xFFE3ECF7),
        shieldDark = Color(0xFF5B7CA6),
        shieldSurface = Color(0xFF16283F),
        telemetry = Color(0xFF9FE8FF),
        textPrimary = Color(0xFFF2F8FF),
        textSecondary = Color(0xFFA3B8CC),
        glowA = Color(0xFF4FC3F7),
        glowB = Color(0xFFB0C4DE),
        glowC = Color(0xFF7986CB),
        schemePrimary = Color(0xFF7FD4FF),
        schemeOnPrimary = Color(0xFF0A2438),
        schemePrimaryContainer = Color(0xFF33608F),
        schemeOnPrimaryContainer = Color(0xFFD6EFFF),
        schemeSecondary = Color(0xFFB0C4DE),
        schemeOnSecondary = Color(0xFF16222E),
        schemeSecondaryContainer = Color(0xFF16283F),
        schemeOnSecondaryContainer = Color(0xFFE3ECF7),
        schemeTertiary = Color(0xFFFFAB00)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFF2F7FC),
        surface = Color(0xFFFFFFFF),
        card = Color(0xFFE3EEF8),
        elevated = Color(0xFFCFE0F2),
        border = Color(0xFFB4CBE4),
        primary = Color(0xFF1565A0),
        primaryGlow = Color(0xFF90CAF9),
        primaryDark = Color(0xFF0D3B5C),
        shield = Color(0xFF546E7A),
        shieldBright = Color(0xFFB0BEC5),
        shieldDark = Color(0xFF37474F),
        shieldSurface = Color(0xFFECEFF1),
        telemetry = Color(0xFF00838F),
        textPrimary = Color(0xFF12222E),
        textSecondary = Color(0xFF5A7184),
        glowA = Color(0xFF4FC3F7),
        glowB = Color(0xFFB0C4DE),
        glowC = Color(0xFF7986CB),
        schemePrimary = Color(0xFF1565A0),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFCFE8FF),
        schemeOnPrimaryContainer = Color(0xFF0A2438),
        schemeSecondary = Color(0xFF546E7A),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFECEFF1),
        schemeOnSecondaryContainer = Color(0xFF263238),
        schemeTertiary = Color(0xFFD97706)
    )
)

val SeasonalPalettes: Map<SeasonalTheme, SeasonPalette> = mapOf(
    SeasonalTheme.PURPLE_GLASS to PurpleGlassPalette,
    SeasonalTheme.SPRING to SpringPalette,
    SeasonalTheme.SUMMER to SummerPalette,
    SeasonalTheme.AUTUMN to AutumnPalette,
    SeasonalTheme.WINTER to WinterPalette
)
