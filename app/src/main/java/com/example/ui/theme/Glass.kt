package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass material for X Blocker.
 *
 * Android has no built-in backdrop-blur material like iOS 26, so the glass
 * look is built the performant way: living seasonal glows sit behind the
 * whole UI ([GlassGlowBackground]) and key surfaces use [liquidGlass] — a
 * translucent fill with a specular gradient edge (bright at the top, fading
 * down), exactly like light catching the rim of real glass.
 */

@Composable
fun GlassGlowBackground(modifier: Modifier = Modifier) {
    val dark = AppThemeState.isDarkTheme
    val violetAlpha = if (dark) 0.38f else 0.16f
    val magentaAlpha = if (dark) 0.26f else 0.12f
    val blueAlpha = if (dark) 0.22f else 0.10f
    Box(modifier = modifier.fillMaxSize().background(ObsidianBg)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-60).dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(activeSeasonPaletteSet.glowA.copy(alpha = violetAlpha), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 40.dp)
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        listOf(activeSeasonPaletteSet.glowB.copy(alpha = magentaAlpha), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 90.dp, y = (-40).dp)
                .size(180.dp)
                .background(
                    Brush.radialGradient(
                        listOf(activeSeasonPaletteSet.glowC.copy(alpha = blueAlpha), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Seasonal motifs (Fix 3): leaves / sun / autumn leaves / snow for
        // the four seasonal themes, drawn over the glows but under all UI
        // (this background sits behind the whole app). Draws nothing for
        // the default purple Liquid Glass theme.
        SeasonalMotifCanvas(
            theme = AppThemeState.seasonalTheme,
            palette = activeSeasonPaletteSet,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Translucent glass surface: tinted fill + specular gradient border.
 * Use on floating/key surfaces (tab bar, hero, sheets) — not on every card,
 * so the effect stays special and scrolling stays at 60fps.
 */
fun Modifier.liquidGlass(
    shape: Shape,
    fillAlpha: Float = 0.62f,
    highlightAlpha: Float = 0.38f
): Modifier = this
    .clip(shape)
    .background(ObsidianSurface.copy(alpha = fillAlpha))
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = highlightAlpha),
                ObsidianBorder.copy(alpha = 0.45f)
            )
        ),
        shape = shape
    )
