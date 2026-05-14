package com.stresswatch.wear.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.stresswatch.wear.data.StressLevel
import com.stresswatch.wear.viewmodel.StressMonitorController
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import android.graphics.SweepGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

class WatchFaceRenderer(
    context: Context,
    surfaceHolder: SurfaceHolder,
    private val styleRepository: CurrentUserStyleRepository,
    private val complicationSlotsManager: ComplicationSlotsManager,
    private val watchState: WatchState,
    canvasType: Int
) : Renderer.CanvasRenderer2<WatchFaceRenderer.StressWatchFaceSharedAssets>(
    surfaceHolder,
    styleRepository,
    watchState,
    canvasType,
    16L, // 60 fps approx
    clearWithBackgroundTintBeforeRenderingHighlightLayer = true
) {

    class StressWatchFaceSharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    private val timePaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = context.resources.displayMetrics.density * 65
        typeface = android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL)
        letterSpacing = 0.05f
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = context.resources.displayMetrics.density * 65
        typeface = android.graphics.Typeface.create("sans-serif-bold", android.graphics.Typeface.NORMAL)
        style = Paint.Style.FILL
        letterSpacing = 0.05f
        maskFilter = android.graphics.BlurMaskFilter(25f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private var lastSweepColor: Int? = null
    private var cachedSweepGradient: SweepGradient? = null

    private var lastScoreColor: Int? = null
    private var lastScoreFraction: Float? = null
    private var cachedRadialGradient: RadialGradient? = null

    private val ringPaint = Paint().apply {
        color = Color.parseColor("#22FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#11FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val zonePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val scorePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val armPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val sweepPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun getColorWithAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    override suspend fun createSharedAssets(): StressWatchFaceSharedAssets {
        return StressWatchFaceSharedAssets()
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: StressWatchFaceSharedAssets) {
        val uiState = StressMonitorController.uiState.value
        val stressLevel = uiState.current.stressLevel
        val isAmbient = watchState.isAmbient.value == true
        
        // Read Style
        val isDynamicEnabled = (styleRepository.userStyle.value[androidx.wear.watchface.style.UserStyleSetting.Id("dynamic_colors")] 
            as? androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption)?.value ?: true

        // Background - Deep Dark
        canvas.drawColor(Color.BLACK)

        // Draw Complications
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }

        // Colors
        val (mainColor, glowColor) = if (isDynamicEnabled && !isAmbient) {
            when (stressLevel) {
                StressLevel.CHILLING -> Pair(Color.parseColor("#00E5FF"), Color.parseColor("#00B8D4")) // Cyan Glow
                StressLevel.NORMAL -> Pair(Color.parseColor("#00E676"), Color.parseColor("#00C853"))  // Spring Green
                StressLevel.LIGHT_STRESS -> Pair(Color.parseColor("#FFEA00"), Color.parseColor("#FFD600")) // Yellow Glow
                StressLevel.STRESSED -> Pair(Color.parseColor("#FF1744"), Color.parseColor("#D50000"))    // Vivid Red
                StressLevel.IDLE -> Pair(Color.GRAY, Color.DKGRAY)
            }
        } else {
            Pair(Color.WHITE, Color.LTGRAY)
        }

        if (isAmbient) {
            // Minimalist Ambient Mode: White text, no glow, no special colors
            timePaint.color = Color.WHITE
            timePaint.clearShadowLayer()
        } else {
            // Premium Active Mode: Colors + Glow
            // Calculate luminance to determine contrast text color
            val r = Color.red(mainColor)
            val g = Color.green(mainColor)
            val b = Color.blue(mainColor)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            
            if (luminance > 0.5) {
                timePaint.color = Color.BLACK
                timePaint.setShadowLayer(8f, 0f, 0f, getColorWithAlpha(Color.WHITE, 180))
            } else {
                timePaint.color = Color.WHITE
                timePaint.setShadowLayer(8f, 0f, 0f, getColorWithAlpha(Color.BLACK, 180))
            }
            glowPaint.color = glowColor
        }

        val cx = bounds.centerX().toFloat()
        val cy = bounds.centerY().toFloat()
        val maxR = minOf(cx, cy) * 0.88f

        if (!isAmbient) {
            // Update radar ring and grid color to be unicolor based on mainColor
            ringPaint.color = getColorWithAlpha(mainColor, 50) // ~20% opacity
            gridPaint.color = getColorWithAlpha(mainColor, 25) // ~10% opacity
            
            // Draw Radar Background (Concentric Rings)
            for (i in 1..4) {
                canvas.drawCircle(cx, cy, maxR * (i / 4f), ringPaint)
            }

            // Draw Grid Lines (every 45 degrees)
            for (angle in 0 until 360 step 45) {
                val rad = Math.toRadians(angle.toDouble())
                canvas.drawLine(
                    cx, cy,
                    cx + maxR * cos(rad).toFloat(),
                    cy + maxR * sin(rad).toFloat(),
                    gridPaint
                )
            }

            // Radar is now unicolor, so we removed the multi-color zones.

            // Draw filled area proportional to score
            val scoreFraction = uiState.current.stressScore.coerceIn(0f, 100f) / 100f
            if (scoreFraction > 0f) {
                val sweepAngle = scoreFraction * 360f
                val radius = maxR * scoreFraction.coerceAtLeast(0.01f)
                if (lastScoreColor != mainColor || lastScoreFraction != scoreFraction || cachedRadialGradient == null) {
                    cachedRadialGradient = RadialGradient(
                        cx, cy, radius,
                        intArrayOf(getColorWithAlpha(mainColor, 90), getColorWithAlpha(mainColor, 10)),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    lastScoreColor = mainColor
                    lastScoreFraction = scoreFraction
                    scorePaint.shader = cachedRadialGradient
                }
                val arcBounds = RectF(cx - maxR, cy - maxR, cx + maxR, cy + maxR)
                canvas.drawArc(arcBounds, -90f, sweepAngle, true, scorePaint)
            }

            // Draw Radar Arm and Sweep Trail
            val radarSpeed = if (stressLevel == StressLevel.IDLE) 8000L else 3000L
            val radarAngle = ((System.currentTimeMillis() % radarSpeed) / radarSpeed.toFloat()) * 360f

            canvas.save()
            canvas.rotate(radarAngle, cx, cy)

            // Sweep Trail
            if (lastSweepColor != mainColor || cachedSweepGradient == null) {
                val c0 = Color.TRANSPARENT
                val c1 = getColorWithAlpha(mainColor, 30)
                val c2 = getColorWithAlpha(mainColor, 80)
                cachedSweepGradient = SweepGradient(
                    cx, cy,
                    intArrayOf(c0, c0, c1, c2),
                    floatArrayOf(0f, 300f / 360f, 330f / 360f, 1f)
                )
                lastSweepColor = mainColor
                sweepPaint.shader = cachedSweepGradient
            }
            val sweepBounds = RectF(cx - maxR, cy - maxR, cx + maxR, cy + maxR)
            canvas.drawArc(sweepBounds, 300f, 60f, true, sweepPaint)

            // Radar Arm
            armPaint.color = mainColor
            armPaint.alpha = if (stressLevel == StressLevel.IDLE) 76 else 255 // 30% opacity if idle
            canvas.drawLine(cx, cy, cx + maxR, cy, armPaint)

            canvas.restore()

            // Draw Center Dot
            dotPaint.color = mainColor
            canvas.drawCircle(cx, cy, 8f, dotPaint)
            dotPaint.color = Color.WHITE
            canvas.drawCircle(cx, cy, 3f, dotPaint)
            
            // Draw time glow
            if (isDynamicEnabled) {
                val timeText = zonedDateTime.format(timeFormatter)
                val x = bounds.centerX().toFloat()
                val y = bounds.centerY().toFloat() - ((timePaint.descent() + timePaint.ascent()) / 2)
                canvas.drawText(timeText, x, y, glowPaint)
            }
        }

        // Draw Time
        val timeText = zonedDateTime.format(timeFormatter)
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat() - ((timePaint.descent() + timePaint.ascent()) / 2)
        
        if (!uiState.hasPermission && !isAmbient) {
            val warningPaint = Paint().apply {
                color = Color.RED
                textSize = 30f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Permessi mancanti", x, bounds.bottom.toFloat() - 40f, warningPaint)
        }

        // Draw main text
        canvas.drawText(timeText, x, y, timePaint)

        // Request next frame for fluid 60fps animation
        if (!isAmbient) {
            invalidate()
        }
    }






    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime, sharedAssets: StressWatchFaceSharedAssets) {
        // Optional: highlight for editing mode
    }
}
