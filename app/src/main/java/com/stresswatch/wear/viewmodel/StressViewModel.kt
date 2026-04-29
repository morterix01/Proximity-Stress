package com.stresswatch.wear.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.stresswatch.wear.StressMonitorService
import com.stresswatch.wear.data.HistoryPoint
import com.stresswatch.wear.data.SensorRepository
import com.stresswatch.wear.data.StressData
import com.stresswatch.wear.data.StressLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StressUiState(
    val current: StressData = StressData(),
    val history: List<HistoryPoint> = emptyList(),
    val isMonitoring: Boolean = false,
    val hasPermission: Boolean = false
)

object StressMonitorController {
    private val _uiState = MutableStateFlow(StressUiState())
    val uiState: StateFlow<StressUiState> = _uiState.asStateFlow()

    private var repository: SensorRepository? = null
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private const val maxHistoryPoints = 60

    fun startMonitoring(context: Context) {
        if (monitorJob?.isActive == true) return

        // Avvia il Foreground Service per mantenere vivo il processo
        val intent = Intent(context, StressMonitorService::class.java)
        ContextCompat.startForegroundService(context, intent)

        val repo = SensorRepository(context)
        repository = repo

        _uiState.value = _uiState.value.copy(isMonitoring = true)

        monitorJob = scope.launch {
            repo.sensorDataFlow().collect { data ->
                val newPoint = HistoryPoint(score = data.stressScore, timestamp = data.timestamp)
                val history = (_uiState.value.history + newPoint).takeLast(maxHistoryPoints)

                _uiState.value = _uiState.value.copy(
                    current = data,
                    history = history
                )
            }
        }
    }

    fun stopMonitoring(context: Context) {
        monitorJob?.cancel()
        monitorJob = null
        repository?.stopDemo()
        _uiState.value = _uiState.value.copy(isMonitoring = false)

        val intent = Intent(context, StressMonitorService::class.java)
        context.stopService(intent)
    }
}

class StressViewModel : ViewModel() {
    val uiState: StateFlow<StressUiState> = StressMonitorController.uiState

    fun startMonitoring(context: Context) {
        StressMonitorController.startMonitoring(context.applicationContext)
    }

    fun stopMonitoring(context: Context) {
        StressMonitorController.stopMonitoring(context.applicationContext)
    }
}
