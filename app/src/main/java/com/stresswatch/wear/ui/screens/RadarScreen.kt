package com.stresswatch.wear.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.stresswatch.wear.data.StressLevel
import com.stresswatch.wear.ui.theme.StressColors
import com.stresswatch.wear.viewmodel.StressUiState
import kotlin.math.*

// ──────────────────────────────────────────────────────────────
//  Scheda 1 – Radar stress con colore dinamico
//  Blu    = CHILLING
//  Verde  = NORMAL
//  Giallo = LIGHT_STRESS
//  Rosso  = STRESSED
//  Grigio = IDLE (non monitora)
// ──────────────────────────────────────────────────────────────

@Composable
fun RadarScreen(uiState: StressUiState) {
    val stress = uiState.current
    val isIdle = stress.stressLevel == StressLevel.IDLE || !uiState.isMonitoring

    // Colore dominante basato sul livello stress
    val targetColor = stressToColor(stress.stressLevel)
    val targetColorLight = stressToColorLight(stress.stressLevel)

    val animColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "stressColor"
    )
    val animColorLight by animateColorAsState(
        targetValue = targetColorLight,
        animationSpec = tween(durationMillis = 800),
        label = "stressColorLight"
    )

    // Rotazione radar: lenta e spenta quando idle
    val radarSpeed = if (isIdle) 8000 else 3000
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = radarSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    // Pulsazione anello esterno (nessuna se idle)
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isIdle) 0.85f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Score animato
    val animScore by animateFloatAsState(
        targetValue = stress.stressScore,
        animationSpec = tween(durationMillis = 600),
        label = "score"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StressColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Canvas radar
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = minOf(cx, cy) * 0.88f

            drawRadarBackground(cx, cy, maxR, animColor, animColorLight,
                if (isIdle) 0f else animScore / 100f)
            drawColorZones(cx, cy, maxR)
            drawRadarArm(cx, cy, maxR, radarAngle, animColorLight.copy(alpha = if (isIdle) 0.3f else 1f))
            if (!isIdle) drawPulseRing(cx, cy, maxR, pulseAnim, animColor)
            drawCenterDot(cx, cy, animColorLight)
        }

        // Testo centrale
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 30.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stress.stressLevel.emoji,
                fontSize = 20.sp
            )
            if (isIdle) {
                Text(
                    text = "---",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = StressColors.IdleLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "NON ATTIVO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = StressColors.Idle,
                    letterSpacing = 2.sp
                )
            } else {
                Text(
                    text = "${animScore.toInt()}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = animColorLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stress.stressLevel.label.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = animColor,
                    letterSpacing = 2.sp
                )
            }
        }

        // HR badge in basso (solo se attivo)
        if (stress.heartRate > 0f && !isIdle) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "❤️", fontSize = 10.sp)
                Text(
                    text = "${stress.heartRate.toInt()} bpm",
                    fontSize = 10.sp,
                    color = StressColors.OnSurfaceMuted
                )
            }
        }

        // Indicatore "MONITORAGGIO IN CORSO" se attivo ma senza dati
        if (uiState.isMonitoring && isIdle) {
            Text(
                text = "In attesa sensori…",
                fontSize = 8.sp,
                color = StressColors.IdleLight,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  Helpers colori
// ─────────────────────────────────────────────────────────────────

fun stressToColor(level: StressLevel): Color = when (level) {
    StressLevel.IDLE         -> StressColors.Idle
    StressLevel.CHILLING     -> StressColors.Chilling
    StressLevel.NORMAL       -> StressColors.Normal
    StressLevel.LIGHT_STRESS -> StressColors.LightStress
    StressLevel.STRESSED     -> StressColors.Stressed
}

fun stressToColorLight(level: StressLevel): Color = when (level) {
    StressLevel.IDLE         -> StressColors.IdleLight
    StressLevel.CHILLING     -> StressColors.ChillingLight
    StressLevel.NORMAL       -> StressColors.NormalLight
    StressLevel.LIGHT_STRESS -> StressColors.LightStressLight
    StressLevel.STRESSED     -> StressColors.StressedLight
}

// ─────────────────────────────────────────────────────────────────
//  Funzioni di disegno Canvas
// ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawRadarBackground(
    cx: Float, cy: Float, maxR: Float,
    color: Color, colorLight: Color, scoreFraction: Float
) {
    // Anelli concentrici (sfondo)
    val rings = 4
    for (i in 1..rings) {
        val r = maxR * (i.toFloat() / rings)
        drawCircle(
            color = StressColors.RadarRing,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f)
        )
    }

    // Linee a raggi (ogni 45°)
    for (angle in 0 until 360 step 45) {
        val rad = Math.toRadians(angle.toDouble())
        drawLine(
            color = StressColors.GridLine,
            start = Offset(cx, cy),
            end = Offset(
                cx + maxR * cos(rad).toFloat(),
                cy + maxR * sin(rad).toFloat()
            ),
            strokeWidth = 1f
        )
    }

    // Area riempita proporzionale allo score (arco completo)
    if (scoreFraction > 0f) {
        val sweepAngle = scoreFraction * 360f
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.05f)),
                center = Offset(cx, cy),
                radius = maxR * scoreFraction.coerceAtLeast(0.01f)
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(cx - maxR, cy - maxR),
            size = Size(maxR * 2, maxR * 2)
        )
    }
}

private fun DrawScope.drawColorZones(cx: Float, cy: Float, maxR: Float) {
    // Quattro zone colorate come anelli sottili sull'esterno
    // dall'interno: blu, verde, giallo, rosso
    data class Zone(val outerFrac: Float, val innerFrac: Float, val color: Color)
    val zones = listOf(
        Zone(1.00f, 0.75f, StressColors.Stressed.copy(alpha = 0.18f)),
        Zone(0.75f, 0.50f, StressColors.LightStress.copy(alpha = 0.18f)),
        Zone(0.50f, 0.25f, StressColors.Normal.copy(alpha = 0.18f)),
        Zone(0.25f, 0.00f, StressColors.Chilling.copy(alpha = 0.22f)),
    )
    zones.forEach { z ->
        val outerR = maxR * z.outerFrac
        drawCircle(color = z.color, radius = outerR, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawRadarArm(
    cx: Float, cy: Float, maxR: Float, angleDeg: Float, color: Color
) {
    rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(color.copy(alpha = 0f), color.copy(alpha = 0.9f)),
                start = Offset(cx, cy),
                end = Offset(cx + maxR, cy)
            ),
            start = Offset(cx, cy),
            end = Offset(cx + maxR, cy),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        // Scia del braccio (sweep trail)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    color.copy(alpha = 0f),
                    color.copy(alpha = 0f),
                    color.copy(alpha = 0.12f),
                    color.copy(alpha = 0.25f),
                ),
                center = Offset(cx, cy)
            ),
            startAngle = -60f,
            sweepAngle = 60f,
            useCenter = true,
            topLeft = Offset(cx - maxR, cy - maxR),
            size = Size(maxR * 2, maxR * 2)
        )
    }
}

private fun DrawScope.drawPulseRing(
    cx: Float, cy: Float, maxR: Float, scale: Float, color: Color
) {
    drawCircle(
        color = color.copy(alpha = 0.5f * (scale - 0.85f) / 0.15f),
        radius = maxR * scale,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawCenterDot(cx: Float, cy: Float, color: Color) {
    drawCircle(color = color, radius = 5f, center = Offset(cx, cy))
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = 2f,
        center = Offset(cx, cy)
    )
}
