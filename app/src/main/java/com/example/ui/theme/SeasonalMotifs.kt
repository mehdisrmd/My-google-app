package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Seasonal motifs (Fix 3 — themes changed per owner's request, not
 * removed): each seasonal theme carries its ELEMENT as a subtle
 * decorative drawing, not just colors:
 * - SPRING: green leaves
 * - SUMMER: a soft sun with rays
 * - AUTUMN: yellow/amber falling leaves
 * - WINTER: snowflakes and snow dots
 *
 * Drawn with Compose Canvas in fraction-of-size coordinates, so the same
 * drawing works full-screen (ambient background via GlassGlowBackground)
 * and tiny (theme-picker card previews in the Info tab). Everything is
 * low-alpha, non-interactive (a bare Canvas takes no input), and kept
 * away from text-dense areas by design of the placements. The default
 * PURPLE_GLASS theme draws nothing — it stays exactly as before.
 */
@Composable
fun SeasonalMotifCanvas(
    theme: SeasonalTheme,
    palette: SeasonPaletteSet,
    modifier: Modifier = Modifier
) {
    if (theme == SeasonalTheme.PURPLE_GLASS) return
    Canvas(modifier = modifier) {
        when (theme) {
            SeasonalTheme.SPRING -> drawSpringLeaves(palette)
            SeasonalTheme.SUMMER -> drawSummerSun(palette)
            SeasonalTheme.AUTUMN -> drawAutumnLeaves(palette)
            SeasonalTheme.WINTER -> drawWinterSnow(palette)
            SeasonalTheme.PURPLE_GLASS -> Unit
        }
    }
}

/** One leaf: two quadratic curves meeting at a tip. Drawn pointing up, rotated around its base. */
private fun DrawScope.drawLeaf(
    cx: Float,
    cy: Float,
    length: Float,
    rotationDeg: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(cx, cy)
        quadraticBezierTo(cx + length * 0.42f, cy - length * 0.38f, cx, cy - length)
        quadraticBezierTo(cx - length * 0.42f, cy - length * 0.38f, cx, cy)
        close()
    }
    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        drawPath(path = path, color = color)
    }
}

private data class LeafSpec(
    val xFrac: Float,
    val yFrac: Float,
    val lenFrac: Float,
    val rotation: Float,
    val alpha: Float,
    val colorSlot: Int // 0 = primary, 1 = telemetry, 2 = primaryGlow
)

private fun DrawScope.drawLeaves(palette: SeasonPaletteSet, specs: List<LeafSpec>) {
    val minDim = min(size.width, size.height)
    val colors = listOf(palette.primary, palette.telemetry, palette.primaryGlow)
    specs.forEach { spec ->
        drawLeaf(
            cx = size.width * spec.xFrac,
            cy = size.height * spec.yFrac,
            length = minDim * spec.lenFrac,
            rotationDeg = spec.rotation,
            color = colors[spec.colorSlot % colors.size].copy(alpha = spec.alpha)
        )
    }
}

private fun DrawScope.drawSpringLeaves(palette: SeasonPaletteSet) {
    drawLeaves(
        palette,
        listOf(
            LeafSpec(0.08f, 0.34f, 0.17f, -28f, 0.16f, 0),
            LeafSpec(0.22f, 0.68f, 0.12f, 22f, 0.11f, 1),
            LeafSpec(0.88f, 0.26f, 0.16f, 30f, 0.15f, 0),
            LeafSpec(0.76f, 0.58f, 0.11f, -16f, 0.10f, 1),
            LeafSpec(0.52f, 0.90f, 0.13f, 12f, 0.10f, 0),
            LeafSpec(0.38f, 0.14f, 0.10f, -38f, 0.09f, 2)
        )
    )
}

private fun DrawScope.drawAutumnLeaves(palette: SeasonPaletteSet) {
    drawLeaves(
        palette,
        listOf(
            LeafSpec(0.12f, 0.20f, 0.14f, 24f, 0.17f, 0),
            LeafSpec(0.30f, 0.52f, 0.11f, -20f, 0.12f, 1),
            LeafSpec(0.84f, 0.36f, 0.15f, -30f, 0.16f, 1),
            LeafSpec(0.68f, 0.74f, 0.12f, 18f, 0.11f, 0),
            LeafSpec(0.46f, 0.16f, 0.10f, 40f, 0.10f, 2),
            LeafSpec(0.58f, 0.92f, 0.13f, -12f, 0.10f, 0)
        )
    )
}

private fun DrawScope.drawSummerSun(palette: SeasonPaletteSet) {
    val minDim = min(size.width, size.height)
    val cx = size.width * 0.82f
    val cy = size.height * 0.18f
    val r = minDim * 0.11f
    // Rays
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val inner = r * 1.30f
        val outer = r * 1.75f
        drawLine(
            color = palette.primary.copy(alpha = 0.38f),
            start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
            end = Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
            strokeWidth = r * 0.12f,
            cap = StrokeCap.Round
        )
    }
    // Soft halo + disc
    drawCircle(color = palette.primaryGlow.copy(alpha = 0.30f), radius = r * 1.55f, center = Offset(cx, cy))
    drawCircle(color = palette.primaryGlow.copy(alpha = 0.60f), radius = r, center = Offset(cx, cy))
    drawCircle(color = palette.primary.copy(alpha = 0.35f), radius = r * 0.70f, center = Offset(cx, cy))
    // Two gentle warm dots, bottom area
    drawCircle(
        color = palette.telemetry.copy(alpha = 0.16f),
        radius = minDim * 0.035f,
        center = Offset(size.width * 0.16f, size.height * 0.80f)
    )
    drawCircle(
        color = palette.primary.copy(alpha = 0.12f),
        radius = minDim * 0.025f,
        center = Offset(size.width * 0.30f, size.height * 0.66f)
    )
}

private fun DrawScope.drawSnowflake(cx: Float, cy: Float, r: Float, color: Color) {
    for (i in 0 until 6) {
        val angle = Math.toRadians(i * 60.0)
        val ex = cx + (cos(angle) * r).toFloat()
        val ey = cy + (sin(angle) * r).toFloat()
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(ex, ey),
            strokeWidth = r * 0.14f,
            cap = StrokeCap.Round
        )
        // Small branch ticks near the tip
        for (sign in listOf(-1, 1)) {
            val branchAngle = angle + sign * Math.toRadians(38.0)
            val bx = ex - (cos(angle) * r * 0.38f).toFloat()
            val by = ey - (sin(angle) * r * 0.38f).toFloat()
            drawLine(
                color = color,
                start = Offset(bx, by),
                end = Offset(
                    bx + (cos(branchAngle) * r * 0.34f).toFloat(),
                    by + (sin(branchAngle) * r * 0.34f).toFloat()
                ),
                strokeWidth = r * 0.11f,
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(color = color, radius = r * 0.13f, center = Offset(cx, cy))
}

private fun DrawScope.drawWinterSnow(palette: SeasonPaletteSet) {
    val minDim = min(size.width, size.height)
    val flake = palette.primaryGlow
    drawSnowflake(size.width * 0.14f, size.height * 0.18f, minDim * 0.075f, flake.copy(alpha = 0.34f))
    drawSnowflake(size.width * 0.85f, size.height * 0.30f, minDim * 0.095f, flake.copy(alpha = 0.30f))
    drawSnowflake(size.width * 0.66f, size.height * 0.82f, minDim * 0.06f, palette.telemetry.copy(alpha = 0.26f))
    // Falling snow dots
    val dots = listOf(
        Triple(0.30f, 0.44f, 0.016f), Triple(0.50f, 0.22f, 0.012f),
        Triple(0.74f, 0.58f, 0.014f), Triple(0.22f, 0.72f, 0.012f),
        Triple(0.44f, 0.64f, 0.010f), Triple(0.92f, 0.70f, 0.012f),
        Triple(0.58f, 0.42f, 0.010f), Triple(0.08f, 0.52f, 0.011f)
    )
    dots.forEach { (x, y, rr) ->
        drawCircle(
            color = flake.copy(alpha = 0.28f),
            radius = minDim * rr,
            center = Offset(size.width * x, size.height * y)
        )
    }
}
