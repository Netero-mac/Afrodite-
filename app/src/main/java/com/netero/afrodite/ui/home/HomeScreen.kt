package com.netero.afrodite.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.system.SystemActions
import com.netero.afrodite.ui.LauncherUiState
import com.netero.afrodite.ui.LauncherViewModel
import com.netero.afrodite.ui.components.AppPicker
import com.netero.afrodite.ui.components.GoldDivider
import com.netero.afrodite.ui.components.PickerRequest
import com.netero.afrodite.ui.components.quietClickable
import com.netero.afrodite.ui.components.rememberLastNonNull
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray
import com.netero.afrodite.util.toast
import kotlin.math.abs

/** Distância mínima do dedo para contar como "deslizar". */
private val SWIPE_THRESHOLD = 64.dp

/**
 * Tela inicial: relógio, data, até 6 favoritos em texto e o dock.
 *
 * Gestos (em qualquer lugar da tela):
 * - deslizar para cima → gaveta de apps
 * - deslizar para baixo → notificações
 * - deslizar para a direita/esquerda → app escolhido nas configurações
 * - toque duplo → bloquear a tela (se ativado)
 * - toque longo → configurações
 */
@Composable
fun HomeScreen(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    onLaunch: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val settings = state.settings

    // Os gestos são configurados uma vez só (pointerInput(Unit)); por isso leem
    // sempre a versão mais nova do estado através de rememberUpdatedState.
    val latestState by rememberUpdatedState(state)
    val latestOnLaunch by rememberUpdatedState(onLaunch)

    var dockSlotToPick by remember { mutableStateOf<Int?>(null) }

    fun openGestureApp(key: String) {
        val app = latestState.appsByKey[key]
        if (app != null) {
            latestOnLaunch(app)
        } else {
            context.toast("Escolha um app para este gesto em Configurações › Gestos")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val threshold = SWIPE_THRESHOLD.toPx()
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = { total = Offset.Zero },
                        onDragCancel = { total = Offset.Zero },
                        onDragEnd = {
                            val dx = total.x
                            val dy = total.y
                            val vertical = abs(dy) >= abs(dx)
                            when {
                                vertical && dy < -threshold -> viewModel.openDrawer()
                                vertical && dy > threshold -> SystemActions.expandNotifications(context)
                                !vertical && dx > threshold -> openGestureApp(latestState.settings.swipeRightApp)
                                !vertical && dx < -threshold -> openGestureApp(latestState.settings.swipeLeftApp)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            total += dragAmount
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (latestState.settings.doubleTapLock) SystemActions.lockScreen(context)
                        },
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openSettings()
                        },
                    )
                }
                .systemBarsPadding(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Clock(
                    use24h = settings.use24h,
                    onTimeClick = { SystemActions.openAlarms(context) },
                    onDateClick = { SystemActions.openCalendar(context) },
                )
                Spacer(Modifier.height(36.dp))
                GoldDivider(Modifier.width(120.dp))
                Spacer(Modifier.height(32.dp))
                Favorites(favorites = state.favorites, onClick = onLaunch)
                Spacer(Modifier.weight(1.2f))
                if (settings.showDock) {
                    Dock(
                        slots = state.dock,
                        goldenIcons = settings.goldenIcons,
                        onAppClick = onLaunch,
                        onPickSlot = { dockSlotToPick = it },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Seletor de app para uma posição do dock (fica por cima, fora da área dos gestos).
        val slot = rememberLastNonNull(dockSlotToPick)
        AnimatedVisibility(
            visible = dockSlotToPick != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 12 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 12 },
        ) {
            if (slot != null) {
                val request = remember(slot) {
                    PickerRequest(
                        title = "Dock · posição ${slot + 1}",
                        selected = listOfNotNull(state.dock.getOrNull(slot)?.key),
                        maxSelection = 1,
                        allowNone = true,
                    ) { keys -> viewModel.setDockSlot(slot, keys.firstOrNull().orEmpty()) }
                }
                AppPicker(
                    request = request,
                    apps = state.allApps,
                    goldenIcons = settings.goldenIcons,
                    onDismiss = { dockSlotToPick = null },
                )
            }
        }
    }
}

/** Lista de favoritos: só o nome, centralizado, em cinza claro. */
@Composable
private fun Favorites(favorites: List<AppInfo>, onClick: (AppInfo) -> Unit) {
    val gold = LocalGold.current
    if (favorites.isEmpty()) {
        Text(
            text = "toque longo na tela para escolher favoritos",
            color = gold.secondary,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 1.5.sp,
        )
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        favorites.forEach { app ->
            Text(
                text = app.label,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .quietClickable { onClick(app) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                color = TextGray,
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
