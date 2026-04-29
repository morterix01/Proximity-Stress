package com.stresswatch.wear.data

/**
 * Livello di stress calcolato dai parametri biometrici.
 * IDLE     = grigio   → non sta monitorando
 * CHILLING = blu      → super rilassato (score 0-24)
 * NORMAL   = verde    → normale (score 25-49)
 * LIGHT    = giallo   → inizio stress (score 50-74)
 * STRESSED = rosso    → stressato (score 75-100)
 */
enum class StressLevel(val label: String, val emoji: String) {
    IDLE("Non attivo", "⏸️"),
    CHILLING("Chilling", "😌"),
    NORMAL("Normale", "😊"),
    LIGHT_STRESS("Lieve stress", "😐"),
    STRESSED("Stressato", "😰")
}

/**
 * Snapshot dei dati biometrici in un dato momento.
 */
data class StressData(
    val heartRate: Float = 0f,          // bpm
    val heartRateVariability: Float = 0f, // HRV ms (RMSSD approssimato)
    val skinConductance: Float = 0f,    // GSR (simulato se non disponibile)
    val stressScore: Float = 0f,        // 0.0 – 100.0
    val stressLevel: StressLevel = StressLevel.IDLE,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Punto storico per il grafico (ultimi N campioni).
 */
data class HistoryPoint(
    val score: Float,
    val timestamp: Long
)
