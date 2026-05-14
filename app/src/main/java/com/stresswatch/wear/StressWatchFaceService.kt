package com.stresswatch.wear

import android.graphics.RectF
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.complications.ComplicationSlotBounds



import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState

import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.TapEvent
import androidx.wear.watchface.TapType
import com.stresswatch.wear.renderer.WatchFaceRenderer

import com.stresswatch.wear.viewmodel.StressMonitorController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StressWatchFaceService : WatchFaceService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        StressMonitorController.startMonitoring(applicationContext)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {

        val renderer = WatchFaceRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            styleRepository = currentUserStyleRepository,
            complicationSlotsManager = complicationSlotsManager,
            watchState = watchState,
            canvasType = CanvasType.HARDWARE
        )


        // Observe stress changes → redraw the watch face
        serviceScope.launch {
            StressMonitorController.uiState.collect {
                renderer.invalidate()
            }
        }

        // Keep monitoring alive when the face becomes visible
        serviceScope.launch {
            watchState.isVisible.collect { isVisible ->
                if (isVisible == true) {
                    StressMonitorController.startMonitoring(applicationContext)
                }
            }
        }

        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        ).setTapListener(object : WatchFace.TapListener {
            override fun onTapEvent(tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?) {
                if (tapType == TapType.UP && complicationSlot == null) {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    if (intent != null) {
                        startActivity(intent)
                    }
                }
            }
        })


    }

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {

        // CanvasComplicationFactory lambda receives watchState + invalidateCallback from the
        // framework at the right time — avoids the chicken-and-egg problem with WatchState.
        fun makeSlot(
            id: Int,
            bounds: RectF,
            types: List<ComplicationType>
        ): ComplicationSlot {
            return ComplicationSlot.createRoundRectComplicationSlotBuilder(
                id = id,
                canvasComplicationFactory = CanvasComplicationFactory { watchState, invalidateCallback ->
                    CanvasComplicationDrawable(
                        ComplicationDrawable(this@StressWatchFaceService),
                        watchState,
                        invalidateCallback
                    )
                },
                supportedTypes = types,
                defaultDataSourcePolicy = androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy(),
                bounds = ComplicationSlotBounds(bounds)
            ).build()
        }

        val shortTypes = listOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
        val longTypes = listOf(
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE,
            ComplicationType.LONG_TEXT
        )

        return ComplicationSlotsManager(
            listOf(
                makeSlot(100, RectF(0.10f, 0.40f, 0.35f, 0.60f), shortTypes), // Left
                makeSlot(101, RectF(0.65f, 0.40f, 0.90f, 0.60f), shortTypes), // Right
                makeSlot(102, RectF(0.30f, 0.08f, 0.70f, 0.33f), longTypes),  // Top
                makeSlot(103, RectF(0.30f, 0.67f, 0.70f, 0.92f), longTypes)   // Bottom
            ),
            currentUserStyleRepository
        )
    }

    override fun createUserStyleSchema(): androidx.wear.watchface.style.UserStyleSchema {
        return androidx.wear.watchface.style.UserStyleSchema(
            listOf(
                androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting(
                    androidx.wear.watchface.style.UserStyleSetting.Id("dynamic_colors"),
                    resources,
                    com.stresswatch.wear.R.string.style_dynamic_colors_title,
                    com.stresswatch.wear.R.string.style_dynamic_colors_desc,
                    null,
                    listOf(androidx.wear.watchface.style.WatchFaceLayer.BASE),
                    true
                )

            )
        )
    }



    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
