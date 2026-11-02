package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient as AndroidLinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RadialGradient as AndroidRadialGradient
import android.graphics.Shader as AndroidShader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance animated weather wallpaper canvas in Jetpack Compose.
 */
@Composable
fun WeatherWallpaperCanvas(
    condition: WeatherCondition,
    artStyle: WallpaperArtStyle,
    timeOfDay: TimeOfDay,
    temperatureC: Double,
    windSpeedKmh: Double,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnimation")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val effectiveTime = resolveEffectiveTimeOfDay(timeOfDay, isDay)
        val timeMs = (animationProgress * 10000).toLong()

        // 1. Draw atmospheric sky gradient
        drawSky(width, height, condition, effectiveTime, timeMs)

        // 2. Draw Celestial body (Sun / Moon & Stars)
        drawCelestial(width, height, effectiveTime, condition, timeMs)

        // 3. Draw Scenery based on Art Style
        drawScenery(width, height, artStyle, effectiveTime, condition, timeMs)

        // 4. Draw Weather Particles (Rain, Snow, Clouds, Fog, Thunderstorm)
        drawWeatherParticles(width, height, condition, windSpeedKmh, timeMs)
    }
}

private fun resolveEffectiveTimeOfDay(timeOfDay: TimeOfDay, isDay: Boolean): TimeOfDay {
    if (timeOfDay != TimeOfDay.AUTO) return timeOfDay
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..7 -> TimeOfDay.DAWN
        hour in 8..17 -> if (isDay) TimeOfDay.DAY else TimeOfDay.NIGHT
        hour in 18..20 -> TimeOfDay.DUSK
        else -> TimeOfDay.NIGHT
    }
}

private fun DrawScope.drawSky(
    width: Float,
    height: Float,
    condition: WeatherCondition,
    timeOfDay: TimeOfDay,
    timeMs: Long
) {
    // Check lightning flash during thunderstorm
    val isLightningFlash = condition == WeatherCondition.THUNDERSTORM && ((timeMs % 2500) in 300..420)

    val colors = when {
        isLightningFlash -> listOf(
            Color(0xFFE2E8F0),
            Color(0xFF94A3B8),
            Color(0xFF64748B)
        )
        condition == WeatherCondition.THUNDERSTORM -> listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF334155)
        )
        condition == WeatherCondition.OVERCAST || condition == WeatherCondition.RAIN -> listOf(
            Color(0xFF334155),
            Color(0xFF475569),
            Color(0xFF64748B)
        )
        condition == WeatherCondition.FOGGY -> listOf(
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFFCBD5E1)
        )
        timeOfDay == TimeOfDay.DAWN -> listOf(
            Color(0xFF4C1D95),
            Color(0xFFBE185D),
            Color(0xFFF97316),
            Color(0xFFFDE047)
        )
        timeOfDay == TimeOfDay.DUSK -> listOf(
            Color(0xFF1E1B4B),
            Color(0xFF831843),
            Color(0xFFEA580C),
            Color(0xFFFBBF24)
        )
        timeOfDay == TimeOfDay.NIGHT -> listOf(
            Color(0xFF030712),
            Color(0xFF0B1329),
            Color(0xFF172554)
        )
        else -> listOf(
            Color(0xFF0284C7),
            Color(0xFF38BDF8),
            Color(0xFFBAE6FD)
        )
    }

    drawRect(
        brush = Brush.verticalGradient(colors = colors),
        size = Size(width, height)
    )
}

private fun DrawScope.drawCelestial(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    if (condition == WeatherCondition.OVERCAST || condition == WeatherCondition.THUNDERSTORM) {
        return // Obscured by thick clouds
    }

    val isNight = timeOfDay == TimeOfDay.NIGHT

    if (isNight) {
        // Draw starry sky
        val starCount = if (condition == WeatherCondition.PARTLY_CLOUDY) 30 else 60
        for (i in 0 until starCount) {
            val seedX = ((i * 73 + 17) % 100) / 100f
            val seedY = ((i * 37 + 43) % 100) / 100f * 0.55f // top 55% of screen
            val shimmer = (sin((timeMs * 0.003f) + i) + 1f) * 0.5f // 0..1
            val alpha = (0.3f + shimmer * 0.7f).coerceIn(0.1f, 1f)
            val radius = if (i % 7 == 0) 2.5f else 1.5f

            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(seedX * width, seedY * height)
            )
        }

        // Moon
        val moonCenter = Offset(width * 0.78f, height * 0.18f)
        val moonRadius = width * 0.08f

        // Moon glow
        drawCircle(
            color = Color(0xFF93C5FD).copy(alpha = 0.15f),
            radius = moonRadius * 1.8f,
            center = moonCenter
        )

        // Moon body
        drawCircle(
            color = Color(0xFFF8FAFC),
            radius = moonRadius,
            center = moonCenter
        )

        // Crescent shadow
        drawCircle(
            color = Color(0xFF0B1329).copy(alpha = 0.9f),
            radius = moonRadius * 0.88f,
            center = Offset(moonCenter.x - moonRadius * 0.35f, moonCenter.y - moonRadius * 0.2f)
        )
    } else {
        // Sun
        val (sunCenter, sunRadiusMultiplier) = when (timeOfDay) {
            TimeOfDay.DAWN -> Offset(width * 0.5f, height * 0.45f) to 0.14f
            TimeOfDay.DUSK -> Offset(width * 0.5f, height * 0.52f) to 0.15f
            else -> Offset(width * 0.75f, height * 0.16f) to 0.11f
        }
        val sunRadius = width * sunRadiusMultiplier

        val glowColor = when (timeOfDay) {
            TimeOfDay.DAWN -> Color(0xFFFB923C)
            TimeOfDay.DUSK -> Color(0xFFF97316)
            else -> Color(0xFFFDE047)
        }

        // Corona glow
        drawCircle(
            color = glowColor.copy(alpha = 0.25f),
            radius = sunRadius * 1.9f,
            center = sunCenter
        )
        drawCircle(
            color = glowColor.copy(alpha = 0.45f),
            radius = sunRadius * 1.35f,
            center = sunCenter
        )
        // Core Sun
        drawCircle(
            color = if (timeOfDay == TimeOfDay.DAY) Color(0xFFFFFBEB) else Color(0xFFFED7AA),
            radius = sunRadius,
            center = sunCenter
        )
    }
}

private fun DrawScope.drawScenery(
    width: Float,
    height: Float,
    artStyle: WallpaperArtStyle,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    when (artStyle) {
        WallpaperArtStyle.MINIMAL_LANDSCAPE -> drawMinimalLandscape(width, height, timeOfDay, condition)
        WallpaperArtStyle.STUDIO_ANIME -> drawStudioAnime(width, height, timeOfDay, condition, timeMs)
        WallpaperArtStyle.CYBERPUNK_NEON -> drawCyberpunkCity(width, height, timeOfDay, condition, timeMs)
        WallpaperArtStyle.HORIZON_ATMOSPHERE -> drawCinematicHorizon(width, height, timeOfDay, condition, timeMs)
        WallpaperArtStyle.DYNAMIC_ABSTRACT -> drawDynamicAbstract(width, height, timeOfDay, condition, timeMs)
    }
}

private fun DrawScope.drawMinimalLandscape(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition
) {
    val isNight = timeOfDay == TimeOfDay.NIGHT

    val ridge3Color = if (isNight) Color(0xFF1E293B) else Color(0xFF475569)
    val ridge2Color = if (isNight) Color(0xFF0F172A) else Color(0xFF1E293B)
    val ridge1Color = if (isNight) Color(0xFF020617) else Color(0xFF0F172A)

    // Back Mountain
    val path3 = Path().apply {
        moveTo(0f, height * 0.65f)
        lineTo(width * 0.35f, height * 0.45f)
        lineTo(width * 0.7f, height * 0.58f)
        lineTo(width, height * 0.48f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(path3, color = ridge3Color.copy(alpha = 0.7f))

    // Middle Mountain
    val path2 = Path().apply {
        moveTo(0f, height * 0.72f)
        lineTo(width * 0.22f, height * 0.56f)
        lineTo(width * 0.55f, height * 0.66f)
        lineTo(width * 0.85f, height * 0.52f)
        lineTo(width, height * 0.60f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(path2, color = ridge2Color.copy(alpha = 0.85f))

    // Foreground Foothill with pine trees
    val path1 = Path().apply {
        moveTo(0f, height * 0.80f)
        quadraticTo(width * 0.4f, height * 0.75f, width * 0.7f, height * 0.82f)
        quadraticTo(width * 0.9f, height * 0.84f, width, height * 0.88f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(path1, color = ridge1Color)

    // Silhouetted Pine Trees
    val treeColor = if (isNight) Color(0xFF010409) else Color(0xFF090D16)
    for (i in 0 until 9) {
        val tx = width * (0.08f + i * 0.11f)
        val ty = height * 0.79f + (i % 3) * (height * 0.02f)
        val treeHeight = height * 0.06f
        val treeWidth = width * 0.045f

        val treePath = Path().apply {
            moveTo(tx, ty - treeHeight)
            lineTo(tx + treeWidth / 2f, ty)
            lineTo(tx - treeWidth / 2f, ty)
            close()
        }
        drawPath(treePath, color = treeColor)
    }
}

private fun DrawScope.drawStudioAnime(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    val isNight = timeOfDay == TimeOfDay.NIGHT

    // Rolling green meadow hills
    val hillBack = if (isNight) Color(0xFF064E3B) else Color(0xFF10B981)
    val hillFront = if (isNight) Color(0xFF022C22) else Color(0xFF059669)

    val pathBack = Path().apply {
        moveTo(0f, height * 0.68f)
        quadraticTo(width * 0.3f, height * 0.62f, width * 0.65f, height * 0.70f)
        quadraticTo(width * 0.85f, height * 0.74f, width, height * 0.66f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(pathBack, color = hillBack.copy(alpha = 0.9f))

    val pathFront = Path().apply {
        moveTo(0f, height * 0.82f)
        quadraticTo(width * 0.4f, height * 0.76f, width * 0.75f, height * 0.84f)
        quadraticTo(width * 0.9f, height * 0.86f, width, height * 0.80f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(pathFront, color = hillFront)

    // Animated wind ripples over meadow
    val sway = sin(timeMs * 0.003f) * 12f
    for (j in 0 until 12) {
        val gx = width * (0.05f + j * 0.08f)
        val gy = height * (0.83f + (j % 4) * 0.035f)
        drawLine(
            color = Color(0xFFA7F3D0).copy(alpha = 0.6f),
            start = Offset(gx, gy),
            end = Offset(gx + sway, gy - 16f),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawCyberpunkCity(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    val baseBuildingColor = Color(0xFF0F172A)
    val neonPink = Color(0xFFF43F5E)
    val neonCyan = Color(0xFF06B6D4)
    val neonYellow = Color(0xFFEAB308)

    // Skyline silhouettes
    val buildings = listOf(
        Triple(0.02f, 0.20f, 0.42f),
        Triple(0.24f, 0.16f, 0.35f),
        Triple(0.42f, 0.22f, 0.48f),
        Triple(0.66f, 0.18f, 0.38f),
        Triple(0.86f, 0.14f, 0.44f)
    )

    for ((leftFraction, widthFraction, heightFraction) in buildings) {
        val bx = width * leftFraction
        val bw = width * widthFraction
        val bh = height * heightFraction
        val by = height - bh

        drawRect(
            color = baseBuildingColor,
            topLeft = Offset(bx, by),
            size = Size(bw, bh)
        )

        // Rooftop antenna with blinking light
        val antennaX = bx + bw * 0.5f
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(antennaX, by),
            end = Offset(antennaX, by - 24f),
            strokeWidth = 2f
        )
        val blinkAlpha = if ((timeMs / 500) % 2 == 0L) 1f else 0.2f
        drawCircle(
            color = neonPink.copy(alpha = blinkAlpha),
            radius = 3.5f,
            center = Offset(antennaX, by - 24f)
        )

        // Windows
        val rows = 8
        val cols = 3
        for (r in 1 until rows) {
            for (c in 0 until cols) {
                if ((r + c + (timeMs / 3000).toInt()) % 3 == 0) {
                    val wx = bx + bw * (0.2f + c * 0.3f)
                    val wy = by + bh * (0.1f + r * 0.1f)
                    val winColor = if ((r + c) % 2 == 0) neonCyan else neonYellow
                    drawRect(
                        color = winColor.copy(alpha = 0.75f),
                        topLeft = Offset(wx, wy),
                        size = Size(bw * 0.18f, bh * 0.045f)
                    )
                }
            }
        }
    }

    // Wet reflective street at bottom
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x880F172A),
                Color(0xDD020617)
            )
        ),
        topLeft = Offset(0f, height * 0.88f),
        size = Size(width, height * 0.12f)
    )
}

private fun DrawScope.drawCinematicHorizon(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    val horizonY = height * 0.65f
    val isNight = timeOfDay == TimeOfDay.NIGHT

    val waterTop = if (isNight) Color(0xFF030712) else Color(0xFF075985)
    val waterBottom = if (isNight) Color(0xFF0B1329) else Color(0xFF0C4A6E)

    // Ocean body
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(waterTop, waterBottom),
            startY = horizonY,
            endY = height
        ),
        topLeft = Offset(0f, horizonY),
        size = Size(width, height - horizonY)
    )

    // Shimmering celestial reflection streak on water
    val reflectColor = if (isNight) Color(0xFFE2E8F0) else Color(0xFFFDE047)
    val reflectX = if (isNight) width * 0.78f else width * 0.5f

    for (k in 0 until 14) {
        val ry = horizonY + (k * (height - horizonY) / 14f)
        val waveW = (width * 0.05f) + (k * width * 0.02f) + sin((timeMs * 0.003f) + k) * 8f
        val alpha = (0.5f - (k * 0.03f)).coerceAtLeast(0.08f)

        drawLine(
            color = reflectColor.copy(alpha = alpha),
            start = Offset(reflectX - waveW / 2f, ry),
            end = Offset(reflectX + waveW / 2f, ry),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawDynamicAbstract(
    width: Float,
    height: Float,
    timeOfDay: TimeOfDay,
    condition: WeatherCondition,
    timeMs: Long
) {
    val isNight = timeOfDay == TimeOfDay.NIGHT
    val colors = if (isNight) {
        listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
    } else {
        listOf(Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFFFBBF24))
    }

    val waveCount = 3
    for (w in 0 until waveCount) {
        val color = colors[w % colors.size]
        val path = Path()
        val baseY = height * (0.6f + w * 0.12f)
        val phase = (timeMs * 0.002f) + w * 1.5f

        path.moveTo(0f, baseY)
        var x = 0f
        while (x <= width) {
            val y = baseY + sin(x / width * 2 * PI.toFloat() + phase) * 35f
            path.lineTo(x, y)
            x += width / 20f
        }
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        drawPath(path, color = color.copy(alpha = 0.35f - w * 0.08f))
    }
}

private fun DrawScope.drawWeatherParticles(
    width: Float,
    height: Float,
    condition: WeatherCondition,
    windSpeedKmh: Double,
    timeMs: Long
) {
    val windFactor = (windSpeedKmh / 30.0).toFloat().coerceIn(0.2f, 2.5f)

    when (condition) {
        WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.THUNDERSTORM -> {
            val dropCount = when (condition) {
                WeatherCondition.DRIZZLE -> 35
                WeatherCondition.RAIN -> 75
                WeatherCondition.THUNDERSTORM -> 110
                else -> 50
            }

            for (i in 0 until dropCount) {
                val seedX = ((i * 127 + 29) % 100) / 100f
                val seedSpeed = 1.2f + (i % 5) * 0.3f
                val yProgress = ((timeMs * 0.0018f * seedSpeed + i * 0.17f) % 1f)
                val xOffset = yProgress * (30f * windFactor)

                val x = (seedX * width + xOffset) % width
                val y = yProgress * height
                val length = if (condition == WeatherCondition.DRIZZLE) 14f else 28f

                drawLine(
                    color = Color(0xFF93C5FD).copy(alpha = 0.65f),
                    start = Offset(x, y),
                    end = Offset(x - 4f * windFactor, y + length),
                    strokeWidth = if (condition == WeatherCondition.DRIZZLE) 1.5f else 2.5f
                )
            }

            // Draw Lightning bolt during thunderstorm
            if (condition == WeatherCondition.THUNDERSTORM && ((timeMs % 2500) in 320..400)) {
                drawLightningBolt(width, height)
            }
        }
        WeatherCondition.SNOW -> {
            val flakeCount = 65
            for (i in 0 until flakeCount) {
                val seedX = ((i * 97 + 13) % 100) / 100f
                val seedSpeed = 0.4f + (i % 4) * 0.2f
                val yProgress = ((timeMs * 0.0007f * seedSpeed + i * 0.13f) % 1f)
                val sway = sin((timeMs * 0.002f) + i) * 18f
                val x = (seedX * width + sway + (timeMs * 0.015f * windFactor)) % width
                val y = yProgress * height
                val radius = 2.5f + (i % 3) * 1.5f

                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
        WeatherCondition.PARTLY_CLOUDY, WeatherCondition.OVERCAST -> {
            drawDriftingClouds(width, height, condition, windFactor, timeMs)
        }
        WeatherCondition.FOGGY -> {
            drawFogLayers(width, height, timeMs)
        }
        WeatherCondition.WINDY -> {
            drawWindGusts(width, height, windFactor, timeMs)
        }
        WeatherCondition.CLEAR -> {
            // Clear sky - subtle floating ambient dust/pollen motes in day
            for (i in 0 until 18) {
                val seedX = ((i * 83 + 19) % 100) / 100f
                val seedY = ((i * 61 + 37) % 100) / 100f
                val motesX = (seedX * width + sin((timeMs * 0.001f) + i) * 20f) % width
                val motesY = (seedY * height * 0.7f + cos((timeMs * 0.0012f) + i) * 15f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 2f,
                    center = Offset(motesX, motesY)
                )
            }
        }
    }
}

private fun DrawScope.drawDriftingClouds(
    width: Float,
    height: Float,
    condition: WeatherCondition,
    windFactor: Float,
    timeMs: Long
) {
    val cloudCount = if (condition == WeatherCondition.OVERCAST) 7 else 4
    val cloudColor = if (condition == WeatherCondition.OVERCAST) Color(0xFFCBD5E1).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.70f)

    for (c in 0 until cloudCount) {
        val speed = 0.02f + (c % 3) * 0.015f
        val startX = ((timeMs * 0.00005f * speed * windFactor * width + c * 260f) % (width + 300f)) - 150f
        val cloudY = height * (0.12f + (c % 4) * 0.08f)
        val cloudSize = width * 0.32f

        // Puff cluster
        drawCircle(color = cloudColor, radius = cloudSize * 0.38f, center = Offset(startX, cloudY))
        drawCircle(color = cloudColor, radius = cloudSize * 0.45f, center = Offset(startX + cloudSize * 0.35f, cloudY - cloudSize * 0.08f))
        drawCircle(color = cloudColor, radius = cloudSize * 0.36f, center = Offset(startX + cloudSize * 0.70f, cloudY))
    }
}

private fun DrawScope.drawFogLayers(width: Float, height: Float, timeMs: Long) {
    for (f in 0 until 4) {
        val fy = height * (0.50f + f * 0.12f)
        val offset = sin((timeMs * 0.001f) + f) * 30f
        drawRect(
            color = Color(0xFFCBD5E1).copy(alpha = 0.28f),
            topLeft = Offset(-50f + offset, fy),
            size = Size(width + 100f, height * 0.10f)
        )
    }
}

private fun DrawScope.drawWindGusts(width: Float, height: Float, windFactor: Float, timeMs: Long) {
    for (w in 0 until 6) {
        val progress = ((timeMs * 0.0008f * windFactor + w * 0.18f) % 1f)
        val wy = height * (0.25f + w * 0.10f)
        val startX = progress * (width + 200f) - 100f

        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(startX, wy),
            end = Offset(startX + 120f, wy),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawLightningBolt(width: Float, height: Float) {
    val boltPath = Path().apply {
        moveTo(width * 0.45f, 0f)
        lineTo(width * 0.42f, height * 0.18f)
        lineTo(width * 0.52f, height * 0.24f)
        lineTo(width * 0.47f, height * 0.42f)
        lineTo(width * 0.56f, height * 0.50f)
        lineTo(width * 0.50f, height * 0.68f)
    }
    // Glow
    drawPath(
        path = boltPath,
        color = Color(0xFF67E8F9).copy(alpha = 0.7f),
        style = Stroke(width = 12f)
    )
    // Core bolt
    drawPath(
        path = boltPath,
        color = Color.White,
        style = Stroke(width = 4f)
    )
}
