package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// Decorative-only seasonal ornament canvas.
//
// It adapts to the size it is given: motifs are placed as fractions of
// the canvas width/height, so a narrow or extra-tall input can't stretch
// them, and blow-out of the canvas size is limited by calling
// modulo instead of adding them up.
@Composable
fun SeasonalMotifsCanvas(
    theme: SeasonalTheme,
    modifier: Modifier = Modifier,
    seed: Int = 2026,
) {
    // Glyph and glow colours come from the active palette, so a theme
    // switch in the same session updates them immediately.
    // Uses theme colour tokens from the palettes and the flight guide to keep
    // the seasonal feedback internationally shared.
    val palette = SeasonalPalettes.getValue(theme)
    val density = LocalDensity.current
    val isDark = false // resolved elsewhere; light set only
    val motifPalette = palette.activeSeasonPaletteSet(isDark)
    val themePatterns = remember(seed) {
        // Keep the layout stable per theme and seed, so reopening the stats
        // card after a theme change doesn't restart the capsule flow.
        // Pseudo-random slot selection is seeded: drawing items are rounded
        // to the nearest motif inside the slot size (rounded to two decimals) so
        // opening the same context in different sessions returns the same layout.
        SeasonalMotifLayout(seed)
    }
    val glowA = motifPalette.lightGlowA
    val glowB = motifPalette.lightGlowB
    val glowC = motifPalette.lightGlowC
    Canvas(
        modifier = modifier
    ) {
        val minDim = min(size.width, size.height)
        val width = size.width
        val height = size.height
        // Use the minimum dimension for sizing: the motif is centered on
        // whichever axis is smaller, so both axes can't stretch it.
        // Capped by minDim so narrow cards don't shrink the frame.
        // Rotate the whole scene 360 degrees over `animationClockMs`: the value
        // above is in degrees and resets every 360 ms, so the canvas is sent a
        // stable snapshot rather than a rotating value.
        val alphaA = 0.28f
        val alphaB = 0.20f
        val alphaC = 0.12f
        val flake = glowA.copy(alpha = alphaA)
        val seedRandom = Random(seed)
        val random = Random(seedRandom.nextInt())
        when (theme) {
            SeasonalTheme.PURPLE_GLASS -> {
                // Streak drops: purple-glass rainfall/droplet orbs.
                drawDrops(
                    width = width,
                    height = height,
                    minDim = minDim,
                    random = random,
                    themeRandom = seedRandom,
                )
            }
            SeasonalTheme.SPRING -> {
                drawSpringBlossoms(
                    width = width,
                    height = height,
                    minDim = minDim,
                    random = random,
                    flake = flake,
                    glowA = glowA,
                    glowB = glowB,
                    glowC = glowC,
                    seed = seed,
                )
            }
            SeasonalTheme.SUMMER -> {
                drawSummerSuns(
                    width = width,
                    height = height,
                    minDim = minDim,
                    random = random,
                    glowA = glowA,
                    glowB = glowB,
                    seed = seed,
                )
            }
            SeasonalTheme.AUTUMN -> {
                drawAutumnLeaves(
                    width = width,
                    height = height,
                    minDim = minDim,
                    random = random,
                    flake = flake,
                    glowA = glowA,
                    glowB = glowB,
                    glowC = glowC,
                    seed = seed,
                )
            }
            SeasonalTheme.WINTER -> {
                drawWinterSnowflakes(
                    width = width,
                    height = height,
                    minDim = minDim,
                    random = random,
                    flake = flake,
                    seed = seed,
                )
            }
        }
    }
}

private data class SeasonalMotifLayout(val seed: Int)

/**
 * Draws three stacks of stylised disgusted/purple droplets.
 * Each stack is tied to the same seed, which keeps the class blobs
 * stable when the seed is the same.
 * @param seed Seed for the random generator for the whole motif set.
 */
private fun DrawScope.drawDrops(
    width: Float,
    height: Float,
    minDim: Float,
    random: Random,
    themeRandom: Random,
) {
    val count = 9
    // Use a min transparent charcoal to keep the fountain legible on a
    // light backdrop.
    val droplet = Color(0xFF6B5B7A).copy(alpha = 0.35f)
    val dropletOutline = Color(0xFF6B5B7A).copy(alpha = 0.22f)
    val teardrop = Path().apply {
        moveTo(0f, -1.0f)
        cubicTo(0.55f, 0.15f, 0.75f, 0.55f, 0.55f, 0.95f)
        cubicTo(0.4f, 1.12f, -0.4f, 1.12f, -0.55f, 0.95f)
        cubicTo(-0.75f, 0.55f, -0.55f, 0.15f, 0f, -1.0f)
        close()
    }
    repeat(count) { index ->
        val x = width * when (index) {
            0 -> 0.08f
            1 -> 0.20f
            2 -> 0.32f
            3 -> 0.44f
            4 -> 0.56f
            5 -> 0.68f
            6 -> 0.80f
            7 -> 0.92f
            else -> 0.50f
        }
        val y = height * (0.1f + (index % 3) * 0.08f)
        val radius = minDim * (0.08f + index * 0.006f)
        drawPath(
            path = teardrop,
            color = droplet,
            size = androidx.compose.ui.geometry.Size(radius, radius),
            topLeft = androidx.compose.ui.geometry.Offset(x - radius / 2f, y - radius / 2f),
        )
        drawPath(
            path = teardrop,
            color = dropletOutline,
            size = androidx.compose.ui.geometry.Size(radius, radius),
            topLeft = androidx.compose.ui.geometry.Offset(x - radius / 2f, y - radius / 2f),
            style = Stroke(width = 1f),
        )
    }
    val large = Random(themeRandom.nextInt()).nextFloat() * 0.3f
    // Seasonal decorations are not interactive or focusable; the names
    // use neutral wording and are visible as part of the canvas.
    repeat(8) { index ->
        // Draw only high-level shapes to keep the canvas readable.
        val x = width * (0.1f + index * 0.12f)
        val y = height * (0.1f + large)
        val radius = minDim * 0.05f
        // A coloured droplet that starts at the stroke start position.
        drawCircle(
            color = droplet.copy(alpha = 0.22f),
            radius = radius,
            center = androidx.compose.ui.geometry.Offset(x, y),
        )
    }
}

/** Draws 5 simple blossom clusters with a rounded centre and 5 petals each. */
private fun DrawScope.drawSpringBlossoms(
    width: Float,
    height: Float,
    minDim: Float,
    random: Random,
    flake: Color,
    glowA: Color,
    glowB: Color,
    glowC: Color,
    seed: Int,
) {
    val centers = listOf(
        0.12f to 0.20f,
        0.35f to 0.55f,
        0.58f to 0.18f,
        0.78f to 0.56f,
        0.90f to 0.22f,
    )
    centers.forEachIndexed { index, (fx, fy) ->
        val cx = width * fx
        val cy = height * fy
        val petal = minDim * 0.07f
        val angleStep = (2 * PI / 5).toFloat()
        repeat(5) { i ->
            val angle = i * angleStep
            val px = cx + (petal * 0.75f) * cos(angle)
            val py = cy + (petal * 0.75f) * sin(angle)
            drawCircle(
                color = glowB.copy(alpha = 0.18f + index * 0.02f),
                radius = petal * 0.62f,
                center = androidx.compose.ui.geometry.Offset(px, py),
            )
        }
        drawCircle(
            color = glowA.copy(alpha = 0.25f),
            radius = petal * 0.42f,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
        )
    }
    // Seasonal decorations are not interactive or focusable; the
    // wide prose is only a visual marker and does not interrupt the UI.
    val random = Random(seed)
    repeat( (random.nextInt(3) + 3) ) {
        val x = width * random.nextFloat()
        val y = height * random.nextFloat()
        drawCircle(
            color = glowC.copy(alpha = 0.08f),
            radius = minDim * 0.04f,
            center = androidx.compose.ui.geometry.Offset(x, y),
        )
    }
}

/** Draws two summer suns as radiating dot clusters. */
private fun DrawScope.drawSummerSuns(
    width: Float,
    height: Float,
    minDim: Float,
    random: Random,
    glowA: Color,
    glowB: Color,
    seed: Int,
) {
    val centers = listOf(0.18f to 0.30f, 0.82f to 0.38f)
    centers.forEach { (fx, fy) ->
        val cx = width * fx
        val cy = height * fy
        val coreRadius = minDim * 0.055f
        val rayLength = minDim * 0.11f
        val angleStep = (2 * PI / 12).toFloat()
        // Draw 12 rays as short ticks around the core.
        repeat(12) { i ->
            val angle = i * angleStep
            val startX = cx + cos(angle) * (coreRadius * 1.15f)
            val startY = cy + sin(angle) * (coreRadius * 1.15f)
            val endX = cx + cos(angle) * (coreRadius + rayLength)
            val endY = cy + sin(angle) * (coreRadius + rayLength)
            drawLine(
                color = glowB.copy(alpha = 0.30f),
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = minDim * 0.008f,
            )
        }
        drawCircle(
            color = glowA.copy(alpha = 0.25f),
            radius = coreRadius,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
        )
    }
    // A couple of small warm dots in the background for depth.
    val random = Random(seed)
    repeat(7) {
        drawCircle(
            color = glowB.copy(alpha = 0.10f),
            radius = minDim * 0.03f + random.nextFloat() * minDim * 0.02f,
            center = androidx.compose.ui.geometry.Offset(
                width * random.nextFloat(),
                height * random.nextFloat()
            ),
        )
    }
}

private fun arcPath(cx: Float, cy: Float, rx: Float, ry: Float, path: Path) {
    val px = 0f
    val py = 0f
    // Approximate a path that bends the leaf around the horizontal axis.
    path.moveTo(cx - rx, cy)
    path.cubicTo(cx - rx * 0.55f, cy - ry, cx + rx * 0.55f, cy - ry, cx + rx, cy)
    path.cubicTo(cx + rx * 0.55f, cy + ry, cx - rx * 0.55f, cy + ry, cx - rx, cy)
    path.close()
}

/** Draws 6 stylised autumn leaves across the canvas. */
private fun DrawScope.drawAutumnLeaves(
    width: Float,
    height: Float,
    minDim: Float,
    random: Random,
    flake: Color,
    glowA: Color,
    glowB: Color,
    glowC: Color,
    seed: Int,
) {
    // Leaf silhouette: a closed curved shape, plus its stem.
    val leafCount = 6
    val leafColors = listOf(
        glowA.copy(alpha = 0.24f),
        glowB.copy(alpha = 0.20f),
        glowC.copy(alpha = 0.22f),
    )
    repeat(leafCount) { index ->
        val fx = 0.10f + index * 0.15f
        val fy = 0.15f + (index % 3) * 0.22f
        val cx = width * fx
        val cy = height * fy
        val rx = minDim * 0.05f
        val ry = minDim * 0.03f
        val leaf = Path().apply {
            arcPath(cx, cy, rx, ry, this)
        }
        drawPath(
            path = leaf,
            color = leafColors[index % leafColors.size],
        )
        drawLine(
            color = Color(0xFF6B3E22).copy(alpha = 0.35f),
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(cx + rx * 1.3f, cy + ry * 0.9f),
            strokeWidth = minDim * 0.006f,
        )
    }
    // A few drifting specks in the background.
    val random = Random(seed)
    repeat(8) {
        drawCircle(
            color = glowC.copy(alpha = 0.10f),
            radius = minDim * 0.03f,
            center = androidx.compose.ui.geometry.Offset(
                width * random.nextFloat(),
                height * random.nextFloat()
            ),
        )
    }
}

private fun drawSnowflake(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    alpha: Float = 0.28f,
) {
    // Placeholder: the current DrawScope.drawSnowflake call below is
    // highlighted via drawLine segments.
}

private fun DrawScope.drawSnowflake(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
    spin: Float,
) {
    val angleStep = (PI / 3).toFloat()
    repeat(6) { i ->
        val angle = i * angleStep + spin
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(x, y),
            strokeWidth = radius * 0.13f,
        )
        // Small tick near the arm tip to suggest a crystal branch.
        val midX = cx + cos(angle) * radius * 0.65f
        val midY = cy + sin(angle) * radius * 0.65f
        val tick = radius * 0.22f
        val tAngle1 = angle + 0.6f
        val tAngle2 = angle - 0.6f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(midX, midY),
            end = androidx.compose.ui.geometry.Offset(
                midX + cos(tAngle1) * tick,
                midY + sin(tAngle1) * tick
            ),
            strokeWidth = radius * 0.11f,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(midX, midY),
            end = androidx.compose.ui.geometry.Offset(
                midX + cos(tAngle2) * tick,
                midY + sin(tAngle2) * tick
            ),
            strokeWidth = radius * 0.11f,
        )
    }
    drawCircle(
        color = color,
        radius = radius * 0.14f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
    )
}

/** Draws winter snowflakes: one large at top-left, three smaller below. */
private fun DrawScope.drawWinterSnowflakes(
    width: Float,
    height: Float,
    minDim: Float,
    random: Random,
    flake: Color,
    seed: Int,
) {
    // The reduced-motion fallback renders the standard motif using the
    // seed to keep the layout stable; the large flakes can be positioned
    // for a seamless parallax.
    val sway = 0f
    val spin = 0f
    val largeAlpha = 0.32f
    val smallAlpha = 0.26f
    drawSnowflake(
        size.width * 0.10f + sway,
        size.height * 0.16f,
        minDim * 0.075f,
        flake.copy(alpha = largeAlpha),
        spin
    )
    drawSnowflake(
        size.width * 0.24f + sway,
        size.height * 0.64f,
        minDim * 0.035f,
        flake.copy(alpha = smallAlpha),
        spin
    )
    drawSnowflake(
        size.width * 0.52f + sway,
        size.height * 0.27f,
        minDim * 0.035f,
        flake.copy(alpha = smallAlpha),
        spin
    )
    drawSnowflake(
        size.width * 0.72f + sway,
        size.height * 0.82f,
        minDim * 0.05f,
        flake.copy(alpha = smallAlpha),
        spin
    )
    drawSnowflake(
        size.width * 0.86f + sway,
        size.height * 0.30f,
        minDim * 0.035f,
        flake.copy(alpha = smallAlpha),
        spin
    )
    // Draw two frozen droplets under the snowflake block for depth.
    val random = Random(seed)
    repeat(4) {
        drawCircle(
            color = flake.copy(alpha = 0.09f),
            radius = minDim * 0.035f,
            center = androidx.compose.ui.geometry.Offset(
                width * random.nextFloat(),
                height * random.nextFloat()
            ),
        )
    }
}

private fun sigmoid(x: Float): Float {
    return 1f / (1f + kotlin.math.exp(-x))
}

private fun Dp.toPx(density: androidx.compose.ui.unit.Density): Float {
    return with(density) { toPx() }
}
