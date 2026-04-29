package com.stresswatch.wear.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.stresswatch.wear.data.StressLevel
import com.stresswatch.wear.ui.theme.StressColors
import com.stresswatch.wear.viewmodel.StressUiState

// ──────────────────────────────────────────────────────────────
//  Scheda 2 – Risultati con percentuali e parametri biometrici
// ──────────────────────────────────────────────────────────────

@Composable
fun ParametersScreen(uiState: StressUiState) {
    val stress = uiState.current
    val isIdle = stress.stressLevel == StressLevel.IDLE || !uiState.isMonitoring

    val mainColor = stressToColorLight(stress.stressLevel)
    val accentColor = stressToColor(stress.stressLevel)

    val animScore by animateFloatAsState(
        targetValue = stress.stressScore / 100f,
        animationSpec = tween(600),
        label = "scoreArc"
    )
    val animHr by animateFloatAsState(
        targetValue = stress.heartRate,
        animationSpec = tween(600),
        label = "hr"
    )
    val animHrv by animateFloatAsState(
        targetValue = stress.heartRateVariability,
        animationSpec = tween(600),
        label = "hrv"
    )

    // Pulse per il cerchio score quando attivo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBeat by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isIdle) 1f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBeat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StressColors.Background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))

        // ── Titolo ──────────────────────────────────────────
        Text(
            text = "RISULTATI",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StressColors.OnSurfaceMuted,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(6.dp))

        // ── Score arc + percentuale ─────────────────────────
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(76.dp)) {
                drawScoreArc(if (isIdle) 0f else animScore, mainColor, accentColor)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    scaleX = pulseBeat
                    scaleY = pulseBeat
                }
            ) {
                if (isIdle) {
                    Text(
                        text = "--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = StressColors.IdleLight
                    )
                    Text(text = "⏸️", fontSize = 10.sp)
                } else {
                    Text(
                        text = "${stress.stressScore.toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = mainColor
                    )
                    Text(text = stress.stressLevel.emoji, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        // ── Etichetta livello ──────────────────────────────
        Text(
            text = if (isIdle) "Non attivo" else stress.stressLevel.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = mainColor
        )

        Spacer(Modifier.height(5.dp))

        // ── Parametri HR / HRV ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ParamBox(
                icon = "❤️",
                label = "BPM",
                value = if (isIdle) "--" else "${animHr.toInt()}",
                color = if (isIdle) StressColors.IdleLight else StressColors.StressedLight
            )
            ParamBox(
                icon = "📊",
                label = "HRV ms",
                value = if (isIdle) "--" else "${animHrv.toInt()}",
                color = if (isIdle) StressColors.IdleLight else StressColors.ChillingLight
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── Barre colorate per ogni zona ───────────────────
        StressZoneBars(currentScore = stress.stressScore, isIdle = isIdle)
    }
}

// ─────────────────────────────────────────────────────────────────
//  Componenti
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ParamBox(icon: String, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(StressColors.SurfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = icon, fontSize = 12.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 7.sp,
            color = StressColors.OnSurfaceMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun StressZoneBars(currentScore: Float, isIdle: Boolean) {
    data class Zone(val label: String, val range: ClosedFloatingPointRange<Float>, val color: Color, val emoji: String)
    val zones = listOf(
        Zone("Chill", 0f..24.9f, StressColors.ChillingLight, "😌"),
        Zone("Ok", 25f..49.9f, StressColors.NormalLight, "😊"),
        Zone("Lieve", 50f..74.9f, StressColors.LightStressLight, "😐"),
        Zone("Stress", 75f..100f, StressColors.StressedLight, "😰"),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        zones.forEach { zone ->
            val isActive = !isIdle && currentScore in zone.range
            val barFill = if (isIdle || !isActive) 0f else {
                ((currentScore - zone.range.start) / (zone.range.endInclusive - zone.range.start)).coerceIn(0f, 1f)
            }
            val animFill by animateFloatAsState(
                targetValue = barFill,
                animationSpec = tween(600),
                label = "bar${zone.label}"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = zone.emoji,
                    fontSize = 8.sp,
                    modifier = Modifier.width(14.dp)
                )
                // Barra
                Canvas(modifier = Modifier.weight(1f).height(5.dp)) {
                    // sfondo
                    drawRoundRect(
                        color = StressColors.SurfaceVariant,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                    // riempimento
                    if (isActive && animFill > 0f) {
                        drawRoundRect(
                            color = zone.color,
                            size = Size(size.width * animFill, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                        )
                    } else if (isActive) {
                        // Mostra la barra piena per la zona attiva
                        drawRoundRect(
                            color = zone.color.copy(alpha = 0.6f),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                        )
                    }
                }
                Text(
                    text = zone.label,
                    fontSize = 7.sp,
                    color = if (isActive) zone.color else StressColors.OnSurfaceMuted,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(26.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
//  Canvas helpers
// ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawScoreArc(fraction: Float, color: Color, accentColor: Color) {
    val stroke = 9f
    val inset = stroke / 2f

    // Traccia sfondo
    drawArc(
        color = Color.White.copy(alpha = 0.08f),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
    // Traccia score con gradiente da blu → verde → giallo → rosso
    if (fraction > 0f) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    StressColors.ChillingLight,
                    StressColors.NormalLight,
                    StressColors.LightStressLight,
                    StressColors.StressedLight,
                    StressColors.StressedLight,
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            ),
            startAngle = 135f,
            sweepAngle = 270f * fraction,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
