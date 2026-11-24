package com.netero.afrodite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.system.SystemActions
import com.netero.afrodite.ui.components.LocalIconLoader
import com.netero.afrodite.ui.drawer.AppDrawer
import com.netero.afrodite.ui.home.HomeScreen
import com.netero.afrodite.ui.onboarding.OnboardingScreen
import com.netero.afrodite.ui.settings.SettingsScreen
import com.netero.afrodite.ui.theme.AfroditeTheme
import com.netero.afrodite.ui.theme.Black

/**
 * Raiz da interface. Empilha as camadas:
 * tela inicial → gaveta → configurações → boas-vindas (primeira abertura).
 */
@Composable
fun AfroditeRoot(viewModel: LauncherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AfroditeTheme(tone = state.settings.goldTone) {
        CompositionLocalProvider(LocalIconLoader provides viewModel.iconLoader) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Black),
            ) {
                // Enquanto nada carregou (alguns milissegundos), fica tudo preto.
                if (state.ready) {
                    val launch: (AppInfo) -> Unit = { app -> SystemActions.launchApp(context, app) }

                    // Entrada suave da tela inicial quando o launcher inicia.
                    val homeVisible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = homeVisible,
                        enter = fadeIn(tween(700)) + slideInVertically(tween(700, easing = FastOutSlowInEasing)) { it / 30 },
                    ) {
                        HomeScreen(state = state, viewModel = viewModel, onLaunch = launch)
                    }

                    AnimatedVisibility(
                        visible = overlay == Overlay.DRAWER,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 8 },
                        exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 8 },
                    ) {
                        AppDrawer(
                            state = state,
                            query = query,
                            onQueryChange = viewModel::setQuery,
                            onLaunch = launch,
                            onClose = viewModel::closeOverlay,
                            onOpenSettings = viewModel::openSettings,
                            viewModel = viewModel,
                        )
                    }

                    AnimatedVisibility(
                        visible = overlay == Overlay.SETTINGS,
                        enter = fadeIn(tween(220)) + slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 10 },
                        exit = fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 10 },
                    ) {
                        SettingsScreen(state = state, viewModel = viewModel, onClose = viewModel::closeOverlay)
                    }

                    AnimatedVisibility(
                        visible = !state.settings.onboardingDone,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(450)),
                    ) {
                        OnboardingScreen(onFinish = viewModel::completeOnboarding)
                    }
                }
            }
        }
    }
}
