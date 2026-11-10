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

// Spring (redesigned): TickTick-clean soft sage greens — calm leaf-green accent, near-neutral surfaces, green-leaf motifs (see SeasonalMotifs).
private val SpringPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF101410),
        surface = Color(0xFF181D18),
        card = Color(0xFF1E241E),
        elevated = Color(0xFF28302A),
        border = Color(0xFF39443A),
        primary = Color(0xFF8FBC8A),
        primaryGlow = Color(0xFFC4DFC0),
        primaryDark = Color(0xFF4E7A52),
        shield = Color(0xFFA9C6A2),
        shieldBright = Color(0xFFD5E8CF),
        shieldDark = Color(0xFF5F7D5C),
        shieldSurface = Color(0xFF1C2A1E),
        telemetry = Color(0xFFA5C98F),
        textPrimary = Color(0xFFF2F6F0),
        textSecondary = Color(0xFFA9BBA4),
        glowA = Color(0xFF6FA96F),
        glowB = Color(0xFF3E5C40),
        glowC = Color(0xFF2C4630),
        schemePrimary = Color(0xFF8FBC8A),
        schemeOnPrimary = Color(0xFF0E2410),
        schemePrimaryContainer = Color(0xFF2F4A33),
        schemeOnPrimaryContainer = Color(0xFFD8EDD4),
        schemeSecondary = Color(0xFFA9C6A2),
        schemeOnSecondary = Color(0xFF14250F),
        schemeSecondaryContainer = Color(0xFF243824),
        schemeOnSecondaryContainer = Color(0xFFD5E8CF),
        schemeTertiary = Color(0xFFD9C58A)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFF7FAF4),
        surface = Color.White,
        card = Color(0xFFEDF3E8),
        elevated = Color(0xFFDFEBD8),
        border = Color(0xFFC9DCC2),
        primary = Color(0xFF4E7D46),
        primaryGlow = Color(0xFF9CC493),
        primaryDark = Color(0xFF2F5229),
        shield = Color(0xFF5F7D5C),
        shieldBright = Color(0xFFA9C6A2),
        shieldDark = Color(0xFF3D5A3A),
        shieldSurface = Color(0xFFE4EFE0),
        telemetry = Color(0xFF6B8F3E),
        textPrimary = Color(0xFF1B291C),
        textSecondary = Color(0xFF5D7260),
        glowA = Color(0xFFBFD8BC),
        glowB = Color(0xFFDDEBD8),
        glowC = Color(0xFFC8DCC0),
        schemePrimary = Color(0xFF4E7D46),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFD3E8CC),
        schemeOnPrimaryContainer = Color(0xFF16300F),
        schemeSecondary = Color(0xFF5F7D5C),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFE4EFE0),
        schemeOnSecondaryContainer = Color(0xFF22331F),
        schemeTertiary = Color(0xFFA8862F)
    )
)

// Summer (redesigned): TickTick-clean warm sand neutrals with one muted sun-amber accent and sun motifs (see SeasonalMotifs).
private val SummerPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF171310),
        surface = Color(0xFF1F1A15),
        card = Color(0xFF262019),
        elevated = Color(0xFF332920),
        border = Color(0xFF463A2C),
        primary = Color(0xFFE3B76E),
        primaryGlow = Color(0xFFF5DCA8),
        primaryDark = Color(0xFF9A6E2E),
        shield = Color(0xFFCBAA7A),
        shieldBright = Color(0xFFEBD3AC),
        shieldDark = Color(0xFF8A6E42),
        shieldSurface = Color(0xFF2A2114),
        telemetry = Color(0xFFEFC987),
        textPrimary = Color(0xFFFBF4E8),
        textSecondary = Color(0xFFCDBFA8),
        glowA = Color(0xFFD9A44E),
        glowB = Color(0xFF8A6428),
        glowC = Color(0xFF5E4520),
        schemePrimary = Color(0xFFE3B76E),
        schemeOnPrimary = Color(0xFF2C1E05),
        schemePrimaryContainer = Color(0xFF54401C),
        schemeOnPrimaryContainer = Color(0xFFFBEED3),
        schemeSecondary = Color(0xFFCBAA7A),
        schemeOnSecondary = Color(0xFF2A1E0C),
        schemeSecondaryContainer = Color(0xFF38301E),
        schemeOnSecondaryContainer = Color(0xFFEBD3AC),
        schemeTertiary = Color(0xFF8FB8C4)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFFCF8F0),
        surface = Color.White,
        card = Color(0xFFF6EEDF),
        elevated = Color(0xFFF0E3CC),
        border = Color(0xFFE4D3B4),
        primary = Color(0xFFA96F1F),
        primaryGlow = Color(0xFFE3B76E),
        primaryDark = Color(0xFF6E470F),
        shield = Color(0xFF8A6E42),
        shieldBright = Color(0xFFCBAA7A),
        shieldDark = Color(0xFF5E4A26),
        shieldSurface = Color(0xFFF1E7D2),
        telemetry = Color(0xFFB07E1E),
        textPrimary = Color(0xFF2B2113),
        textSecondary = Color(0xFF7C6C52),
        glowA = Color(0xFFF2DCAE),
        glowB = Color(0xFFF8ECCF),
        glowC = Color(0xFFEED9B4),
        schemePrimary = Color(0xFFA96F1F),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFFBEED3),
        schemeOnPrimaryContainer = Color(0xFF33250A),
        schemeSecondary = Color(0xFF8A6E42),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFF1E7D2),
        schemeOnSecondaryContainer = Color(0xFF33291A),
        schemeTertiary = Color(0xFF4E7D8C)
    )
)

// Autumn (redesigned): TickTick-clean warm taupe with one muted amber-rust accent and yellow autumn-leaf motifs (see SeasonalMotifs).
private val AutumnPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF15110D),
        surface = Color(0xFF1D1712),
        card = Color(0xFF251C15),
        elevated = Color(0xFF2F241A),
        border = Color(0xFF453526),
        primary = Color(0xFFD69A5F),
        primaryGlow = Color(0xFFF0C896),
        primaryDark = Color(0xFF8A5A28),
        shield = Color(0xFFC4A582),
        shieldBright = Color(0xFFE8D0AC),
        shieldDark = Color(0xFF84643C),
        shieldSurface = Color(0xFF2A2013),
        telemetry = Color(0xFFE3BC7E),
        textPrimary = Color(0xFFFAF2E6),
        textSecondary = Color(0xFFC9B298),
        glowA = Color(0xFFB57E35),
        glowB = Color(0xFF7A4E20),
        glowC = Color(0xFF54361A),
        schemePrimary = Color(0xFFD69A5F),
        schemeOnPrimary = Color(0xFF2C1B05),
        schemePrimaryContainer = Color(0xFF54401C),
        schemeOnPrimaryContainer = Color(0xFFFBEED3),
        schemeSecondary = Color(0xFFC4A582),
        schemeOnSecondary = Color(0xFF2A1E0C),
        schemeSecondaryContainer = Color(0xFF38301E),
        schemeOnSecondaryContainer = Color(0xFFE8D0AC),
        schemeTertiary = Color(0xFFA8B284)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFFBF6EE),
        surface = Color.White,
        card = Color(0xFFF5EBDC),
        elevated = Color(0xFFEFDFC8),
        border = Color(0xFFE0CBAE),
        primary = Color(0xFFA05F22),
        primaryGlow = Color(0xFFD69A5F),
        primaryDark = Color(0xFF663C10),
        shield = Color(0xFF84643C),
        shieldBright = Color(0xFFC4A582),
        shieldDark = Color(0xFF57411F),
        shieldSurface = Color(0xFFF1E7D2),
        telemetry = Color(0xFFA86E1C),
        textPrimary = Color(0xFF2B1E10),
        textSecondary = Color(0xFF7D6750),
        glowA = Color(0xFFF0D3A8),
        glowB = Color(0xFFF7E6C6),
        glowC = Color(0xFFEAD3AC),
        schemePrimary = Color(0xFFA05F22),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFFBEED3),
        schemeOnPrimaryContainer = Color(0xFF33250A),
        schemeSecondary = Color(0xFF84643C),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFF1E7D2),
        schemeOnSecondaryContainer = Color(0xFF33291A),
        schemeTertiary = Color(0xFF5F7038)
    )
)

// Winter (redesigned): TickTick-clean cool gray-blues with one muted ice-blue accent and snow motifs (see SeasonalMotifs).
private val WinterPalette = SeasonPalette(
    dark = SeasonPaletteSet(
        bg = Color(0xFF0E141B),
        surface = Color(0xFF151D26),
        card = Color(0xFF1B2531),
        elevated = Color(0xFF243140),
        border = Color(0xFF33465C),
        primary = Color(0xFF9CC3E5),
        primaryGlow = Color(0xFFD3E7F7),
        primaryDark = Color(0xFF47719C),
        shield = Color(0xFFAFC6D8),
        shieldBright = Color(0xFFDCE9F4),
        shieldDark = Color(0xFF5F7E99),
        shieldSurface = Color(0xFF1A2836),
        telemetry = Color(0xFFA8D8EA),
        textPrimary = Color(0xFFF1F6FA),
        textSecondary = Color(0xFFA9BCCB),
        glowA = Color(0xFF6E9CC0),
        glowB = Color(0xFF3E6184),
        glowC = Color(0xFF2A4258),
        schemePrimary = Color(0xFF9CC3E5),
        schemeOnPrimary = Color(0xFF0A2438),
        schemePrimaryContainer = Color(0xFF2C4A68),
        schemeOnPrimaryContainer = Color(0xFFD6EFFF),
        schemeSecondary = Color(0xFFAFC6D8),
        schemeOnSecondary = Color(0xFF16222E),
        schemeSecondaryContainer = Color(0xFF243140),
        schemeOnSecondaryContainer = Color(0xFFDCE9F4),
        schemeTertiary = Color(0xFFC9B458)
    ),
    light = SeasonPaletteSet(
        bg = Color(0xFFF4F8FB),
        surface = Color.White,
        card = Color(0xFFE7EFF6),
        elevated = Color(0xFFD8E5F0),
        border = Color(0xFFBFD2E2),
        primary = Color(0xFF3D6E99),
        primaryGlow = Color(0xFF9CC3E5),
        primaryDark = Color(0xFF234A6B),
        shield = Color(0xFF5F7E99),
        shieldBright = Color(0xFFAFC6D8),
        shieldDark = Color(0xFF38546B),
        shieldSurface = Color(0xFFE2EBF3),
        telemetry = Color(0xFF2E7D8F),
        textPrimary = Color(0xFF16242F),
        textSecondary = Color(0xFF5A7184),
        glowA = Color(0xFFC4DCEF),
        glowB = Color(0xFFE0EDF7),
        glowC = Color(0xFFCBDEF0),
        schemePrimary = Color(0xFF3D6E99),
        schemeOnPrimary = Color.White,
        schemePrimaryContainer = Color(0xFFCFE8FF),
        schemeOnPrimaryContainer = Color(0xFF0A2438),
        schemeSecondary = Color(0xFF5F7E99),
        schemeOnSecondary = Color.White,
        schemeSecondaryContainer = Color(0xFFE2EBF3),
        schemeOnSecondaryContainer = Color(0xFF22333F),
        schemeTertiary = Color(0xFF8F7420)
    )
)

val SeasonalPalettes: Map<SeasonalTheme, SeasonPalette> = mapOf(
    SeasonalTheme.PURPLE_GLASS to PurpleGlassPalette,
    SeasonalTheme.SPRING to SpringPalette,
    SeasonalTheme.SUMMER to SummerPalette,
    SeasonalTheme.AUTUMN to AutumnPalette,
    SeasonalTheme.WINTER to WinterPalette
)
