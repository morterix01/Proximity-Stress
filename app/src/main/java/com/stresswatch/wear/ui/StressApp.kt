package com.stresswatch.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import com.stresswatch.wear.ui.screens.ChartScreen
import com.stresswatch.wear.ui.screens.ParametersScreen
import com.stresswatch.wear.ui.screens.RadarScreen
import com.stresswatch.wear.ui.screens.PanicScreen
import com.stresswatch.wear.ui.theme.StressColors
import com.stresswatch.wear.viewmodel.StressViewModel
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────
//  App root con 4 schede:
//    0 → RadarScreen      (Radar stress colorato)
//    1 → ParametersScreen (Risultati + percentuali)
//    2 → ChartScreen      (Grafico andamento)
//    3 → PanicScreen      (Bottone anti-panico / SOS)
//
//  Navigazione:
//    • Swipe orizzontale
//    • Ghiera digitale (rotary) → scorre tra le pagine
// ──────────────────────────────────────────────────────────────

private const val PAGE_COUNT = 4

@Composable
fun StressApp(viewModel: StressViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    // Gestione ghiera digitale
    val focusRequester = remember { FocusRequester() }

    val pageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int get() = pagerState.currentPage
            override val pageCount: Int get() = PAGE_COUNT
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StressColors.Background)
            // Ghiera digitale: scroll rotary → cambia pagina
            .onRotaryScrollEvent { event ->
                coroutineScope.launch {
                    val delta = event.verticalScrollPixels
                    if (delta > 0f) {
                        // Rotazione verso destra → pagina successiva
                        val next = (pagerState.currentPage + 1).coerceAtMost(PAGE_COUNT - 1)
                        pagerState.animateScrollToPage(next)
                    } else if (delta < 0f) {
                        // Rotazione verso sinistra → pagina precedente
                        val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                        pagerState.animateScrollToPage(prev)
                    }
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> RadarScreen(uiState = uiState)
                1 -> ParametersScreen(uiState = uiState)
                2 -> ChartScreen(uiState = uiState)
                3 -> PanicScreen(uiState = uiState)
            }
        }

        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Richiedi il focus quando la composizione è pronta (per la ghiera)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
