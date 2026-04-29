package com.stresswatch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.stresswatch.wear.ui.StressApp
import com.stresswatch.wear.ui.theme.StressWatchTheme
import com.stresswatch.wear.viewmodel.StressViewModel

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: StressViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startMonitoring(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            StressWatchTheme {
                StressApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startMonitoring(this)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    override fun onPause() {
        super.onPause()
        // Non fermiamo il monitoraggio qui, così continua in background tramite il Foreground Service
    }
}
