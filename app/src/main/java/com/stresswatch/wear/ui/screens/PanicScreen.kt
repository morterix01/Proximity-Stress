package com.stresswatch.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                text = "ANTI-PANICO",
                color = StressColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = { /* TODO: Azione Anti-Panico / SOS */ },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = StressColors.Stressed,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            ) {
                Text(
                    text = "SOS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Premi per un\naiuto rapido",
                color = StressColors.OnSurfaceMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
