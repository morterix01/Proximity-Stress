package com.stresswatch.wear.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import com.stresswatch.wear.data.HistoryPoint
import com.stresswatch.wear.ui.theme.StressColors
import com.stresswatch.wear.viewmodel.StressUiState
import kotlin.math.*

// ──────────────────────────────────────────────────────────────
//  Scheda 3 – Grafico andamento stress nel tempo
// ──────────────────────────────────────────────────────────────

@Composable
fun ChartScreen(uiState: StressUiState) {
    val history = uiState.history
    val stress = uiState.current
    val mainColor = stressToColorLight(stress.stressLevel)

    // Animazione entrata del grafico
    val animProgress by animateFloatAsState(
        targetValue = if (history.size > 1) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "chartIn"
    )

    // Stats calcolate
    val avgScore = if (history.isEmpty()) 0f else history.map { it.score }.average().toFloat()
    val maxScore = history.maxOfOrNull { it.score } ?: 0f
    val minScore = history.minOfOrNull { it.score } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StressColors.Background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(5.dp))

        // ── Titolo ──────────────────────────────────────────
        Text(
            text = "ANDAMENTO",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = StressColors.OnSurfaceMuted,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(4.dp))

        // ── Grafico principale ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(StressColors.SurfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            if (history.size > 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawStressChart(history, animProgress)
                    drawChartGrid()
                }
            } else {
                // Placeholder
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📈",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "In raccolta dati…",
                        fontSize = 8.sp,
                        color = StressColors.OnSurfaceMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Statistiche ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                label = "MEDIA",
                value = if (history.isEmpty()) "--" else "${avgScore.toInt()}%",
                color = StressColors.ChillingLight
            )
            StatCard(
                label = "MAX",
                value = if (history.isEmpty()) "--" else "${maxScore.toInt()}%",
                color = StressColors.StressedLight
            )
            StatCard(
                label = "MIN",
                value = if (history.isEmpty()) "--" else "${minScore.toInt()}%",
                color = StressColors.NormalLight
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── Legenda colori ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = StressColors.ChillingLight, label = "Chill")
            LegendItem(color = StressColors.NormalLight, label = "Ok")
            LegendItem(color = StressColors.LightStressLight, label = "Lieve")
            LegendItem(color = StressColors.StressedLight, label = "Stress")
        }

        Spacer(Modifier.height(3.dp))

        // ── Campioni disponibili ───────────────────────────
        Text(
            text = "${history.size} campioni",
            fontSize = 7.sp,
            color = StressColors.OnSurfaceMuted.copy(alpha = 0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────
//  Componenti piccoli
// ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(StressColors.Surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 6.sp,
            color = StressColors.OnSurfaceMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(Modifier.width(2.dp))
        Text(text = label, fontSize = 6.sp, color = StressColors.OnSurfaceMuted)
    }
}

// ─────────────────────────────────────────────────────────────────
//  Canvas: disegno del grafico
// ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawStressChart(history: List<HistoryPoint>, progress: Float) {
    val w = size.width
    val h = size.height
    val maxScore = 100f
    val pointCount = history.size
    val visibleCount = (pointCount * progress).toInt().coerceAtLeast(2)
    val visible = history.takeLast(visibleCount)

    if (visible.size < 2) return

    // ── Area riempita sotto la curva ──────────────────────
    val pathFill = Path()
    val pathLine = Path()

    visible.forEachIndexed { i, point ->
        val x = w * i / (visible.size - 1).toFloat()
        val y = h - (point.score / maxScore * h)
        if (i == 0) {
            pathFill.moveTo(x, h)
            pathFill.lineTo(x, y)
            pathLine.moveTo(x, y)
        } else {
            pathFill.lineTo(x, y)
            pathLine.lineTo(x, y)
        }
    }
    pathFill.lineTo(w, h)
    pathFill.close()

    drawPath(
        path = pathFill,
        brush = Brush.verticalGradient(
            0f to StressColors.Stressed.copy(alpha = 0.3f),
            0.4f to StressColors.LightStress.copy(alpha = 0.2f),
            0.7f to StressColors.Normal.copy(alpha = 0.15f),
            1f to StressColors.Chilling.copy(alpha = 0.05f)
        )
    )

    // ── Linea colorata per zona ───────────────────────────
    val segments = visible.windowed(2)
    segments.forEachIndexed { i, (a, b) ->
        val x1 = w * i / (visible.size - 1).toFloat()
        val x2 = w * (i + 1) / (visible.size - 1).toFloat()
        val y1 = h - (a.score / maxScore * h)
        val y2 = h - (b.score / maxScore * h)
        val avgScore = (a.score + b.score) / 2f
        val segColor = scoreToLineColor(avgScore)
        drawLine(
            color = segColor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }

    // ── Punto corrente evidenziato ────────────────────────
    val lastX = w
    val lastScore = visible.last().score
    val lastY = h - (lastScore / maxScore * h)
    val dotColor = scoreToLineColor(lastScore)
    drawCircle(color = dotColor.copy(alpha = 0.3f), radius = 6f, center = Offset(lastX, lastY))
    drawCircle(color = dotColor, radius = 3.5f, center = Offset(lastX, lastY))
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 1.5f, center = Offset(lastX, lastY))
}

private fun DrawScope.drawChartGrid() {
    val levels = listOf(0.25f, 0.50f, 0.75f)
    levels.forEach { frac ->
        val y = size.height * (1f - frac)
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))
        )
    }
}

private fun scoreToLineColor(score: Float): Color = when {
    score < 25f -> StressColors.ChillingLight
    score < 50f -> StressColors.NormalLight
    score < 75f -> StressColors.LightStressLight
    else        -> StressColors.StressedLight
}
