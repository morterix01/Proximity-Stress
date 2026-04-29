package com.stresswatch.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
//  Palette StressWatch
//  IDLE        = grigio  (no monitoraggio)
//  CHILLING    = blu     (score 0-24)
//  NORMAL      = verde   (score 25-49)
//  LIGHT_STRESS= giallo  (score 50-74)
//  STRESSED    = rosso   (score 75-100)
// ──────────────────────────────────────────────────────────────
object StressColors {
    // Idle / no data
    val Idle      = Color(0xFF424242)
    val IdleLight = Color(0xFF9E9E9E)

    // Chilling (blu)
    val Chilling      = Color(0xFF1565C0)
    val ChillingLight = Color(0xFF42A5F5)

    // Normale (verde)
    val Normal      = Color(0xFF2E7D32)
    val NormalLight = Color(0xFF66BB6A)

    // Lieve stress (giallo)
    val LightStress      = Color(0xFFF9A825)
    val LightStressLight = Color(0xFFFFEE58)

    // Stressato (rosso)
    val Stressed      = Color(0xFFC62828)
    val StressedLight = Color(0xFFEF5350)

    // Superficie / sfondo
    val Background       = Color(0xFF0A0A0F)
    val Surface          = Color(0xFF12121A)
    val SurfaceVariant   = Color(0xFF1C1C28)
    val OnSurface        = Color(0xFFE8E8F0)
    val OnSurfaceMuted   = Color(0xFF8888A0)

    // Griglia radar
    val GridLine  = Color(0x22FFFFFF)
    val RadarRing = Color(0x33FFFFFF)
}

@Composable
fun StressWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
