package com.netero.afrodite.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.system.SystemActions
import com.netero.afrodite.ui.DockResult
import com.netero.afrodite.ui.LauncherUiState
import com.netero.afrodite.ui.LauncherViewModel
import com.netero.afrodite.ui.components.AppIcon
import com.netero.afrodite.ui.components.GoldSearchField
import com.netero.afrodite.ui.components.consumeTouches
import com.netero.afrodite.ui.components.rememberLastNonNull
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray
import com.netero.afrodite.util.TextUtils
import com.netero.afrodite.util.toast
import kotlinx.coroutines.launch

/**
 * Gaveta de apps: busca no topo, lista alfabética e barra A–Z na lateral.
 * Puxar a lista para baixo (quando já está no topo) fecha a gaveta.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    state: LauncherUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LauncherViewModel,
) {
    val context = LocalContext.current
    val gold = LocalGold.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val golden = state.settings.goldenIcons

    val results = remember(state.drawerApps, query) {
        TextUtils.filter(state.drawerApps, query) { it.searchKey }
    }
    val letterIndex = remember(results) { firstIndexByLetter(results.map { it.sectionLetter }) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    // Se só um app corresponde à busca, abre direto (uma vez por texto digitado).
    var autoLaunchedFor by remember { mutableStateOf("") }
    LaunchedEffect(query, results) {
        if (query.isBlank()) {
            autoLaunchedFor = ""
        } else if (results.size == 1 && query != autoLaunchedFor) {
            autoLaunchedFor = query
            onLaunch(results.first())
        }
    }

    // Cada nova busca começa do topo da lista.
    LaunchedEffect(query) { listState.scrollToItem(0) }

    // Puxar para baixo no topo da lista fecha a gaveta.
    val latestOnClose by rememberUpdatedState(onClose)
    val closeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember(closeThreshold) {
        object : NestedScrollConnection {
            private var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > closeThreshold) {
                        pulled = 0f
                        latestOnClose()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Black)
            .consumeTouches(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            // Barra de busca + atalho para as configurações
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoldSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "buscar",
                    modifier = Modifier.weight(1f),
                    onGo = { results.firstOrNull()?.let(onLaunch) },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpar busca", tint = gold.secondary, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configurações", tint = gold.secondary, modifier = Modifier.size(20.dp))
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToClose),
                    contentPadding = PaddingValues(start = 28.dp, end = 48.dp, top = 12.dp, bottom = 32.dp),
                ) {
                    items(results, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            golden = golden,
                            onClick = { onLaunch(app) },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuApp = app
                            },
                        )
                    }
                }

                if (results.isEmpty()) {
                    Text(
                        text = "nenhum app encontrado",
                        modifier = Modifier.align(Alignment.Center),
                        color = gold.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 1.sp,
                    )
                }

                if (query.isBlank() && results.isNotEmpty()) {
                    AlphabetBar(
                        available = letterIndex.keys,
                        onLetterSelected = { letter ->
                            activeLetter = letter
                            indexForLetter(letter, letterIndex)?.let { target ->
                                scope.launch { listState.scrollToItem(target) }
                            }
                        },
                        onRelease = { activeLetter = null },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 8.dp, top = 12.dp, bottom = 24.dp),
                    )
                }
            }
        }

        // Letra grande no centro enquanto o dedo corre pela barra A–Z.
        val shownLetter = rememberLastNonNull(activeLetter)
        AnimatedVisibility(
            visible = activeLetter != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(300)),
        ) {
            Text(
                text = shownLetter?.toString().orEmpty(),
                color = gold.primary,
                fontSize = 80.sp,
                fontWeight = FontWeight.ExtraLight,
            )
        }

        // Menu do toque longo.
        val shownMenuApp = rememberLastNonNull(menuApp)
        AnimatedVisibility(
            visible = menuApp != null,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
        ) {
            val app = shownMenuApp
            if (app != null) {
                AppActionsMenu(
                    app = app,
                    isFavorite = state.isFavorite(app),
                    goldenIcons = golden,
                    onDismiss = { menuApp = null },
                    onInfo = {
                        menuApp = null
                        SystemActions.openAppInfo(context, app)
                    },
                    onUninstall = {
                        menuApp = null
                        SystemActions.uninstall(context, app)
                    },
                    onToggleFavorite = {
                        menuApp = null
                        val wasFavorite = state.isFavorite(app)
                        if (viewModel.toggleFavorite(app)) {
                            context.toast(if (wasFavorite) "Removido dos favoritos" else "Adicionado aos favoritos")
                        } else {
                            context.toast("Você já tem 6 favoritos")
                        }
                    },
                    onAddToDock = {
                        menuApp = null
                        val message = when (viewModel.addToDock(app)) {
                            DockResult.ADDED -> "Adicionado ao dock"
                            DockResult.ALREADY_THERE -> "Esse app já está no dock"
                            DockResult.FULL -> "O dock está cheio (4 apps)"
                        }
                        context.toast(message)
                    },
                    onHide = {
                        menuApp = null
                        viewModel.hideApp(app)
                        context.toast("${app.label} foi ocultado. Reexiba em Configurações.")
                    },
                )
            }
        }
    }
}

/** Uma linha da gaveta: ícone pequeno + nome. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppInfo,
    golden: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, golden = golden, size = 30.dp)
        Spacer(Modifier.width(18.dp))
        Text(
            text = app.label,
            color = TextGray,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 17.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
