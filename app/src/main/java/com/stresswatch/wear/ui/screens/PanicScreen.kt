package com.stresswatch.wear.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import com.stresswatch.wear.ui.theme.StressColors
import com.stresswatch.wear.viewmodel.StressUiState

@Composable
fun PanicScreen(uiState: StressUiState) {
    var isBreathing by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isBreathing) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StressColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (isBreathing) "RESPIRA..." else "ANTI-PANICO",
                color = if (isBreathing) StressColors.ChillingLight else StressColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(contentAlignment = Alignment.Center) {
                if (isBreathing) {
                    // Cerchi di espansione per il respiro
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .background(StressColors.Chilling.copy(alpha = 0.3f), CircleShape)
                    )
                }

                Button(
                    onClick = { isBreathing = !isBreathing },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isBreathing) StressColors.Chilling else StressColors.Stressed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isBreathing) "STOP" else "SOS",
                        fontSize = if (isBreathing) 16.sp else 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = if (isBreathing) "Segui il cerchio\nper calmarti" else "Premi per un\naiuto rapido",
                color = StressColors.OnSurfaceMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

