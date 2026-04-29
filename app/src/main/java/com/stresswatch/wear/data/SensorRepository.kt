package com.stresswatch.wear.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

/**
 * Repository che legge i sensori reali del Galaxy Watch 4:
 *  - TYPE_HEART_RATE  → frequenza cardiaca in bpm
 *  - Fallback: calcola HRV dal jitter dei battiti heart-rate
 *
 *  Score 0–100:
 *    0-24   → CHILLING  (blu)
 *    25-49  → NORMAL    (verde)
 *    50-74  → LIGHT_STRESS (giallo)
 *    75-100 → STRESSED  (rosso)
 *    IDLE   → grigio (nessun dato)
 */
class SensorRepository(private val context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Storico degli ultimi intervalli RR per calcolare HRV (RMSSD)
    private val rrIntervals = ArrayDeque<Float>()
    private var lastHrTimestamp = 0L
    private var lastHrBpm = 0f

    // -------------------------------------------------------
    //  Flow continuo dei dati biometrici
    // -------------------------------------------------------
    fun sensorDataFlow(): Flow<StressData> = callbackFlow {

        var currentHr = 0f
        var currentHrv = 50f
        var hasData = false

        val hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_HEART_RATE -> {
                        val bpm = event.values[0]
                        if (bpm > 0f) {
                            currentHr = bpm
                            hasData = true

                            // Calcola intervallo RR in ms per HRV
                            val now = event.timestamp / 1_000_000L
                            if (lastHrTimestamp > 0L && lastHrBpm > 0f) {
                                val rrMs = 60_000f / bpm
                                rrIntervals.addLast(rrMs)
                                if (rrIntervals.size > 20) rrIntervals.removeFirst()
                                currentHrv = calculateRmssd(rrIntervals)
                            }
                            lastHrTimestamp = now
                            lastHrBpm = bpm

                            val stress = computeStressScore(currentHr, currentHrv)
                            trySend(buildStressData(currentHr, currentHrv, stress, hasData))
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        hrSensor?.also {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            // Nessun sensore fisico → modalità demo con dati simulati realistici
            startDemoMode { data -> trySend(data) }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    // -------------------------------------------------------
    //  Calcolo RMSSD (misura HRV standard clinica)
    // -------------------------------------------------------
    private fun calculateRmssd(intervals: List<Float>): Float {
        if (intervals.size < 2) return 50f
        var sumSq = 0.0
        for (i in 1 until intervals.size) {
            val diff = (intervals[i] - intervals[i - 1]).toDouble()
            sumSq += diff * diff
        }
        val rmssd = sqrt(sumSq / (intervals.size - 1)).toFloat()
        // Clamp 10–100 ms → range tipico
        return rmssd.coerceIn(10f, 100f)
    }

    // -------------------------------------------------------
    //  Score 0–100 da HR + HRV
    //    HR normale riposo: 60-80 bpm → score basso
    //    HRV alta (>60ms) → rilassato, score basso
    // -------------------------------------------------------
    private fun computeStressScore(hr: Float, hrv: Float): Float {
        // HR contribuisce per il 60%
        val hrScore = ((hr - 55f) / 85f * 100f).coerceIn(0f, 100f)
        // HRV: più bassa = più stress (invertiamo)
        val hrvScore = (1f - (hrv / 100f)) * 100f

        return (hrScore * 0.6f + hrvScore * 0.4f).coerceIn(0f, 100f)
    }

    private fun buildStressData(hr: Float, hrv: Float, score: Float, hasData: Boolean): StressData {
        val level = if (!hasData) {
            StressLevel.IDLE
        } else {
            when {
                score < 25f -> StressLevel.CHILLING
                score < 50f -> StressLevel.NORMAL
                score < 75f -> StressLevel.LIGHT_STRESS
                else        -> StressLevel.STRESSED
            }
        }
        return StressData(
            heartRate = hr,
            heartRateVariability = hrv,
            stressScore = score,
            stressLevel = level
        )
    }

    // -------------------------------------------------------
    //  Demo mode – simula variazioni realistiche
    // -------------------------------------------------------
    private var demoThread: Thread? = null

    private fun startDemoMode(emit: (StressData) -> Unit) {
        var hr = 72f
        var trend = 0.3f
        var trendTimer = 0

        demoThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                trendTimer++
                if (trendTimer > 15) {
                    trend = (-1..1).random() * 0.5f
                    trendTimer = 0
                }

                hr = (hr + trend + (Math.random() * 2 - 1).toFloat()).coerceIn(55f, 140f)
                val hrv = ((-0.5f * hr + 92f) + (Math.random() * 10 - 5).toFloat()).coerceIn(10f, 100f)
                val score = computeStressScore(hr, hrv)
                emit(buildStressData(hr, hrv, score, hasData = true))

                Thread.sleep(1000)
            }
        }.also { it.start() }
    }

    fun stopDemo() {
        demoThread?.interrupt()
        demoThread = null
    }
}
