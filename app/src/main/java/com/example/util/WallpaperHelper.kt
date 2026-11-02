package com.example.util

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.data.model.TimeOfDay
import com.example.data.model.WallpaperArtStyle
import com.example.data.model.WeatherCondition
import com.example.service.WeatherLiveWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

enum class WallpaperDestination(val label: String) {
    HOME_SCREEN("Home Screen"),
    LOCK_SCREEN("Lock Screen"),
    BOTH("Home & Lock Screen")
}

object WallpaperHelper {

    suspend fun applyWallpaper(
        context: Context,
        condition: WeatherCondition,
        artStyle: WallpaperArtStyle,
        timeOfDay: TimeOfDay,
        isDay: Boolean,
        temperatureC: Double,
        windSpeedKmh: Double,
        destination: WallpaperDestination
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
            val height = if (metrics.heightPixels > 0) metrics.heightPixels else 2400

            val bitmap = renderToBitmap(
                width = width,
                height = height,
                condition = condition,
                artStyle = artStyle,
                timeOfDay = timeOfDay,
                isDay = isDay,
                temperatureC = temperatureC,
                windSpeedKmh = windSpeedKmh
            )

            val wallpaperManager = WallpaperManager.getInstance(context)

            val flags = when (destination) {
                WallpaperDestination.HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                WallpaperDestination.LOCK_SCREEN -> WallpaperManager.FLAG_LOCK
                WallpaperDestination.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, flags)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }

            Result.success("Wallpaper applied to ${destination.label}!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openLiveWallpaperChooser(context: Context): Boolean {
        return try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, WeatherLiveWallpaperService::class.java)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    fun renderToBitmap(
        width: Int,
        height: Int,
        condition: WeatherCondition,
        artStyle: WallpaperArtStyle,
        timeOfDay: TimeOfDay,
        isDay: Boolean,
        temperatureC: Double,
        windSpeedKmh: Double,
        timeMs: Long = 2000L
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawSceneOnCanvas(
            canvas = canvas,
            width = width.toFloat(),
            height = height.toFloat(),
            condition = condition,
            artStyle = artStyle,
            timeOfDay = timeOfDay,
            isDay = isDay,
            windSpeedKmh = windSpeedKmh,
            timeMs = timeMs
        )

        return bitmap
    }

    fun drawSceneOnCanvas(
        canvas: Canvas,
        width: Float,
        height: Float,
        condition: WeatherCondition,
        artStyle: WallpaperArtStyle,
        timeOfDay: TimeOfDay,
        isDay: Boolean,
        windSpeedKmh: Double,
        timeMs: Long,
        parallaxOffset: Float = 0f,
        touchRippleX: Float = -1f,
        touchRippleY: Float = -1f,
        touchProgress: Float = 0f
    ) {
        val effectiveTime = resolveEffectiveTimeOfDay(timeOfDay, isDay)
        val isNight = effectiveTime == TimeOfDay.NIGHT

        // 1. Sky Gradient
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val skyColors = when {
            condition == WeatherCondition.THUNDERSTORM -> intArrayOf(
                0xFF0F172A.toInt(),
                0xFF1E293B.toInt(),
                0xFF334155.toInt()
            )
            condition == WeatherCondition.OVERCAST || condition == WeatherCondition.RAIN -> intArrayOf(
                0xFF334155.toInt(),
                0xFF475569.toInt(),
                0xFF64748B.toInt()
            )
            condition == WeatherCondition.FOGGY -> intArrayOf(
                0xFF64748B.toInt(),
                0xFF94A3B8.toInt(),
                0xFFCBD5E1.toInt()
            )
            effectiveTime == TimeOfDay.DAWN -> intArrayOf(
                0xFF4C1D95.toInt(),
                0xFFBE185D.toInt(),
                0xFFF97316.toInt(),
                0xFFFDE047.toInt()
            )
            effectiveTime == TimeOfDay.DUSK -> intArrayOf(
                0xFF1E1B4B.toInt(),
                0xFF831843.toInt(),
                0xFFEA580C.toInt(),
                0xFFFBBF24.toInt()
            )
            effectiveTime == TimeOfDay.NIGHT -> intArrayOf(
                0xFF030712.toInt(),
                0xFF0B1329.toInt(),
                0xFF172554.toInt()
            )
            else -> intArrayOf(
                0xFF0284C7.toInt(),
                0xFF38BDF8.toInt(),
                0xFFBAE6FD.toInt()
            )
        }
        skyPaint.shader = LinearGradient(0f, 0f, 0f, height, skyColors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width, height, skyPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 2. Celestial (Sun or Stars & Moon)
        if (condition != WeatherCondition.OVERCAST && condition != WeatherCondition.THUNDERSTORM) {
            if (isNight) {
                // Stars
                paint.style = Paint.Style.FILL
                for (i in 0 until 50) {
                    val seedX = ((i * 73 + 17) % 100) / 100f
                    val seedY = ((i * 37 + 43) % 100) / 100f * 0.55f
                    val shimmer = (sin((timeMs * 0.003f) + i) + 1f) * 0.5f
                    paint.color = 0xFFFFFFFF.toInt()
                    paint.alpha = (shimmer * 200 + 55).toInt().coerceIn(40, 255)
                    val r = if (i % 7 == 0) 4f else 2.5f
                    canvas.drawCircle(seedX * width, seedY * height, r, paint)
                }

                // Moon
                val moonCx = width * 0.78f
                val moonCy = height * 0.18f
                val moonR = width * 0.08f

                // Glow
                paint.color = 0x2693C5FD
                canvas.drawCircle(moonCx, moonCy, moonR * 1.8f, paint)

                // Moon
                paint.color = 0xFFF8FAFC.toInt()
                canvas.drawCircle(moonCx, moonCy, moonR, paint)

                // Crescent shadow
                paint.color = 0xE60B1329.toInt()
                canvas.drawCircle(moonCx - moonR * 0.35f, moonCy - moonR * 0.2f, moonR * 0.88f, paint)
            } else {
                // Sun
                val (sunCx, sunCy) = when (effectiveTime) {
                    TimeOfDay.DAWN -> width * 0.5f to height * 0.45f
                    TimeOfDay.DUSK -> width * 0.5f to height * 0.52f
                    else -> width * 0.75f to height * 0.16f
                }
                val sunR = width * 0.12f

                val glowColor = if (effectiveTime == TimeOfDay.DAY) 0x4DFDE047.toInt() else 0x4DF97316.toInt()
                paint.color = glowColor
                canvas.drawCircle(sunCx, sunCy, sunR * 1.6f, paint)

                paint.color = if (effectiveTime == TimeOfDay.DAY) 0xFFFFFBEB.toInt() else 0xFFFED7AA.toInt()
                canvas.drawCircle(sunCx, sunCy, sunR, paint)
            }
        }

        // 3. Scenery by Art Style
        when (artStyle) {
            WallpaperArtStyle.MINIMAL_LANDSCAPE -> drawMinimalLandscape(canvas, width, height, isNight)
            WallpaperArtStyle.STUDIO_ANIME -> drawStudioAnime(canvas, width, height, isNight)
            WallpaperArtStyle.CYBERPUNK_NEON -> drawCyberpunkCity(canvas, width, height, isNight, timeMs)
            WallpaperArtStyle.HORIZON_ATMOSPHERE -> drawCinematicHorizon(canvas, width, height, isNight, timeMs)
            WallpaperArtStyle.DYNAMIC_ABSTRACT -> drawDynamicAbstract(canvas, width, height, isNight, timeMs)
        }

        // 4. Weather Particles
        drawWeatherParticles(canvas, width, height, condition, windSpeedKmh, timeMs)

        // 5. Interactive Touch Ripple Effect
        if (touchProgress in 0f..1f && touchRippleX >= 0) {
            val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f * (1f - touchProgress) + 1f
                color = 0xFFFFFFFF.toInt()
                alpha = ((1f - touchProgress) * 160).toInt().coerceIn(0, 255)
            }
            val radius = touchProgress * (width * 0.35f)
            canvas.drawCircle(touchRippleX, touchRippleY, radius, ripplePaint)
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

    private fun drawMinimalLandscape(canvas: Canvas, width: Float, height: Float, isNight: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // Back Ridge
        paint.color = if (isNight) 0xB31E293B.toInt() else 0xB3475569.toInt()
        val path3 = Path().apply {
            moveTo(0f, height * 0.65f)
            lineTo(width * 0.35f, height * 0.45f)
            lineTo(width * 0.7f, height * 0.58f)
            lineTo(width, height * 0.48f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(path3, paint)

        // Middle Ridge
        paint.color = if (isNight) 0xD90F172A.toInt() else 0xD91E293B.toInt()
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
        canvas.drawPath(path2, paint)

        // Front Foothill
        paint.color = if (isNight) 0xFF020617.toInt() else 0xFF0F172A.toInt()
        val path1 = Path().apply {
            moveTo(0f, height * 0.80f)
            quadTo(width * 0.4f, height * 0.75f, width * 0.7f, height * 0.82f)
            quadTo(width * 0.9f, height * 0.84f, width, height * 0.88f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(path1, paint)

        // Pine Trees
        paint.color = if (isNight) 0xFF010409.toInt() else 0xFF090D16.toInt()
        for (i in 0 until 9) {
            val tx = width * (0.08f + i * 0.11f)
            val ty = height * 0.79f + (i % 3) * (height * 0.02f)
            val th = height * 0.06f
            val tw = width * 0.045f
            val tree = Path().apply {
                moveTo(tx, ty - th)
                lineTo(tx + tw / 2f, ty)
                lineTo(tx - tw / 2f, ty)
                close()
            }
            canvas.drawPath(tree, paint)
        }
    }

    private fun drawStudioAnime(canvas: Canvas, width: Float, height: Float, isNight: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        paint.color = if (isNight) 0xE6064E3B.toInt() else 0xE610B981.toInt()
        val pathBack = Path().apply {
            moveTo(0f, height * 0.68f)
            quadTo(width * 0.3f, height * 0.62f, width * 0.65f, height * 0.70f)
            quadTo(width * 0.85f, height * 0.74f, width, height * 0.66f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(pathBack, paint)

        paint.color = if (isNight) 0xFF022C22.toInt() else 0xFF059669.toInt()
        val pathFront = Path().apply {
            moveTo(0f, height * 0.82f)
            quadTo(width * 0.4f, height * 0.76f, width * 0.75f, height * 0.84f)
            quadTo(width * 0.9f, height * 0.86f, width, height * 0.80f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(pathFront, paint)
    }

    private fun drawCyberpunkCity(canvas: Canvas, width: Float, height: Float, isNight: Boolean, timeMs: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val buildings = listOf(
            Triple(0.02f, 0.20f, 0.42f),
            Triple(0.24f, 0.16f, 0.35f),
            Triple(0.42f, 0.22f, 0.48f),
            Triple(0.66f, 0.18f, 0.38f),
            Triple(0.86f, 0.14f, 0.44f)
        )

        for ((leftF, widthF, heightF) in buildings) {
            val bx = width * leftF
            val bw = width * widthF
            val bh = height * heightF
            val by = height - bh

            paint.color = 0xFF0F172A.toInt()
            canvas.drawRect(bx, by, bx + bw, height, paint)

            // Windows
            val rows = 8
            val cols = 3
            for (r in 1 until rows) {
                for (c in 0 until cols) {
                    if ((r + c + (timeMs / 3000).toInt()) % 3 == 0) {
                        val wx = bx + bw * (0.2f + c * 0.3f)
                        val wy = by + bh * (0.1f + r * 0.1f)
                        paint.color = if ((r + c) % 2 == 0) 0xBF06B6D4.toInt() else 0xBFEAB308.toInt()
                        canvas.drawRect(wx, wy, wx + bw * 0.18f, wy + bh * 0.045f, paint)
                    }
                }
            }
        }
    }

    private fun drawCinematicHorizon(canvas: Canvas, width: Float, height: Float, isNight: Boolean, timeMs: Long) {
        val horizonY = height * 0.65f
        val oceanPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val waterColors = if (isNight) intArrayOf(0xFF030712.toInt(), 0xFF0B1329.toInt()) else intArrayOf(0xFF075985.toInt(), 0xFF0C4A6E.toInt())
        oceanPaint.shader = LinearGradient(0f, horizonY, 0f, height, waterColors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, horizonY, width, height, oceanPaint)

        // Shimmering streak
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4f
            color = if (isNight) 0x66E2E8F0.toInt() else 0x66FDE047.toInt()
        }
        val rx = if (isNight) width * 0.78f else width * 0.5f
        for (k in 0 until 12) {
            val ry = horizonY + (k * (height - horizonY) / 12f)
            val ww = width * 0.06f + k * width * 0.02f
            canvas.drawLine(rx - ww / 2f, ry, rx + ww / 2f, ry, linePaint)
        }
    }

    private fun drawDynamicAbstract(canvas: Canvas, width: Float, height: Float, isNight: Boolean, timeMs: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val colors = if (isNight) {
            intArrayOf(0x596366F1.toInt(), 0x4CA855F7.toInt(), 0x40EC4899.toInt())
        } else {
            intArrayOf(0x5938BDF8.toInt(), 0x4C34D399.toInt(), 0x40FBBF24.toInt())
        }

        for (w in 0 until 3) {
            paint.color = colors[w % colors.size]
            val path = Path()
            val baseY = height * (0.6f + w * 0.12f)
            val phase = (timeMs * 0.002f) + w * 1.5f
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= width) {
                val y = baseY + sin(x / width * 2 * PI.toFloat() + phase) * 45f
                path.lineTo(x, y)
                x += width / 20f
            }
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    private fun drawWeatherParticles(
        canvas: Canvas,
        width: Float,
        height: Float,
        condition: WeatherCondition,
        windSpeedKmh: Double,
        timeMs: Long
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (condition) {
            WeatherCondition.RAIN, WeatherCondition.DRIZZLE, WeatherCondition.THUNDERSTORM -> {
                paint.color = 0xA693C5FD.toInt()
                paint.strokeWidth = if (condition == WeatherCondition.DRIZZLE) 2f else 3.5f
                val count = if (condition == WeatherCondition.THUNDERSTORM) 90 else 60

                for (i in 0 until count) {
                    val sx = ((i * 127 + 29) % 100) / 100f
                    val yProgress = ((timeMs * 0.0018f * 1.5f + i * 0.17f) % 1f)
                    val x = (sx * width + yProgress * 40f) % width
                    val y = yProgress * height
                    val len = if (condition == WeatherCondition.DRIZZLE) 18f else 32f
                    canvas.drawLine(x, y, x - 5f, y + len, paint)
                }
            }
            WeatherCondition.SNOW -> {
                paint.color = 0xDEFFFFFF.toInt()
                paint.style = Paint.Style.FILL
                for (i in 0 until 60) {
                    val sx = ((i * 97 + 13) % 100) / 100f
                    val yProgress = ((timeMs * 0.0007f + i * 0.13f) % 1f)
                    val x = (sx * width + sin((timeMs * 0.002f) + i) * 20f) % width
                    val y = yProgress * height
                    val r = 3f + (i % 3) * 2f
                    canvas.drawCircle(x, y, r, paint)
                }
            }
            WeatherCondition.PARTLY_CLOUDY, WeatherCondition.OVERCAST -> {
                paint.style = Paint.Style.FILL
                paint.color = if (condition == WeatherCondition.OVERCAST) 0xD9CBD5E1.toInt() else 0xB3FFFFFF.toInt()
                for (c in 0 until 4) {
                    val startX = ((timeMs * 0.00005f * 0.02f * width + c * 280f) % (width + 300f)) - 150f
                    val cy = height * (0.14f + (c % 3) * 0.08f)
                    val cSize = width * 0.35f
                    canvas.drawCircle(startX, cy, cSize * 0.38f, paint)
                    canvas.drawCircle(startX + cSize * 0.35f, cy - cSize * 0.08f, cSize * 0.45f, paint)
                    canvas.drawCircle(startX + cSize * 0.70f, cy, cSize * 0.36f, paint)
                }
            }
            else -> {}
        }
    }
}
