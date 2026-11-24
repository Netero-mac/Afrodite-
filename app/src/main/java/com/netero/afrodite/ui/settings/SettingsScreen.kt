package com.netero.afrodite.ui.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.BuildConfig
import com.netero.afrodite.data.GoldTone
import com.netero.afrodite.data.LauncherSettings
import com.netero.afrodite.system.AccessibilityBridge
import com.netero.afrodite.system.DeviceLock
import com.netero.afrodite.system.SystemActions
import com.netero.afrodite.ui.LauncherUiState
import com.netero.afrodite.ui.LauncherViewModel
import com.netero.afrodite.ui.components.AppPicker
import com.netero.afrodite.ui.components.GoldConfirmDialog
import com.netero.afrodite.ui.components.GoldDivider
import com.netero.afrodite.ui.components.OnResume
import com.netero.afrodite.ui.components.PickerRequest
import com.netero.afrodite.ui.components.consumeTouches
import com.netero.afrodite.ui.components.rememberLastNonNull
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray
import com.netero.afrodite.ui.theme.palette

/** Tela de configurações. Tudo é salvo na hora (DataStore). */
@Composable
fun SettingsScreen(
    state: LauncherUiState,
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val gold = LocalGold.current
    val settings = state.settings

    var picker by remember { mutableStateOf<PickerRequest?>(null) }
    var showLockExplanation by remember { mutableStateOf(false) }
    var serviceActive by remember { mutableStateOf(AccessibilityBridge.isRunning) }
    var adminActive by remember { mutableStateOf(DeviceLock.isActive(context)) }
    var isDefault by remember { mutableStateOf(SystemActions.isDefaultLauncher(context)) }

    // Ao voltar das telas do Android (acessibilidade, administrador, app de início), atualiza o status.
    OnResume {
        serviceActive = AccessibilityBridge.isRunning
        adminActive = DeviceLock.isActive(context)
        isDefault = SystemActions.isDefaultLauncher(context)
    }

    // Versão "completo" no Android 9+: bloqueia pela acessibilidade (mantém a digital).
    // Versão "padrão": bloqueia pelo administrador do dispositivo.
    val useAccessibility = BuildConfig.HAS_ACCESSIBILITY && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val lockReady = serviceActive || adminActive

    fun appLabel(key: String): String = state.appsByKey[key]?.label ?: "nenhum"

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = gold.primary)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Configurações",
                    color = gold.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    letterSpacing = 1.sp,
                )
            }

            // ---------------- Tela inicial ----------------
            SectionTitle("Tela inicial")
            SettingRow(
                title = "Favoritos",
                value = "${state.favorites.size} de ${LauncherSettings.MAX_FAVORITES}",
                onClick = {
                    picker = PickerRequest(
                        title = "Favoritos",
                        selected = state.favorites.map { it.key },
                        maxSelection = LauncherSettings.MAX_FAVORITES,
                        onConfirm = viewModel::setFavorites,
                    )
                },
            )
            state.favorites.forEachIndexed { index, app ->
                ReorderRow(
                    label = "${index + 1}   ${app.label}",
                    canMoveUp = index > 0,
                    onMoveUp = { viewModel.moveFavoriteUp(app.key) },
                    onRemove = { viewModel.removeFavorite(app.key) },
                )
            }
            SettingSwitch(
                title = "Formato 24 horas",
                subtitle = if (settings.use24h) "ex.: 21:30" else "ex.: 9:30 PM",
                checked = settings.use24h,
                onCheckedChange = viewModel::setUse24h,
            )
            SettingSwitch(
                title = "Mostrar dock",
                subtitle = "4 ícones na parte de baixo",
                checked = settings.showDock,
                onCheckedChange = viewModel::setShowDock,
            )
            if (settings.showDock) {
                state.dock.forEachIndexed { index, app ->
                    SettingRow(
                        title = "Dock · posição ${index + 1}",
                        value = app?.label ?: "vazia",
                        indent = true,
                        onClick = {
                            picker = PickerRequest(
                                title = "Dock · posição ${index + 1}",
                                selected = listOfNotNull(app?.key),
                                maxSelection = 1,
                                allowNone = true,
                            ) { keys -> viewModel.setDockSlot(index, keys.firstOrNull().orEmpty()) }
                        },
                    )
                }
            }

            // ---------------- Gaveta ----------------
            SectionTitle("Gaveta de apps")
            SettingRow(
                title = "Apps ocultos",
                value = if (settings.hidden.isEmpty()) "nenhum" else "${settings.hidden.count { it in state.appsByKey }}",
                onClick = {
                    picker = PickerRequest(
                        title = "Apps ocultos",
                        selected = settings.hidden.filter { it in state.appsByKey },
                        maxSelection = Int.MAX_VALUE,
                    ) { keys -> viewModel.setHidden(keys.toSet()) }
                },
            )

            // ---------------- Aparência ----------------
            SectionTitle("Aparência")
            SettingSwitch(
                title = "Ícones dourados",
                subtitle = if (settings.goldenIcons) "ícones monocromáticos em dourado" else "ícones originais dos apps",
                checked = settings.goldenIcons,
                onCheckedChange = viewModel::setGoldenIcons,
            )
            Text(
                text = "Tom do dourado",
                color = TextGray,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 14.dp),
            )
            ToneSelector(selected = settings.goldTone, onSelect = viewModel::setGoldTone)

            // ---------------- Gestos ----------------
            SectionTitle("Gestos")
            InfoRow("Deslizar para cima", "gaveta de apps")
            InfoRow("Deslizar para baixo", "notificações")
            InfoRow("Toque longo", "configurações")
            SettingRow(
                title = "Deslizar para a direita",
                value = appLabel(settings.swipeRightApp),
                onClick = {
                    picker = PickerRequest(
                        title = "Deslizar para a direita",
                        selected = listOf(settings.swipeRightApp).filter { it.isNotEmpty() },
                        maxSelection = 1,
                        allowNone = true,
                    ) { keys -> viewModel.setSwipeRightApp(keys.firstOrNull().orEmpty()) }
                },
            )
            SettingRow(
                title = "Deslizar para a esquerda",
                value = appLabel(settings.swipeLeftApp),
                onClick = {
                    picker = PickerRequest(
                        title = "Deslizar para a esquerda",
                        selected = listOf(settings.swipeLeftApp).filter { it.isNotEmpty() },
                        maxSelection = 1,
                        allowNone = true,
                    ) { keys -> viewModel.setSwipeLeftApp(keys.firstOrNull().orEmpty()) }
                },
            )
            SettingSwitch(
                title = "Toque duplo para bloquear",
                subtitle = when {
                    !settings.doubleTapLock -> "toque duas vezes na tela inicial para bloquear"
                    serviceActive -> "ativo (serviço de acessibilidade)"
                    adminActive -> "ativo (o próximo desbloqueio pede o PIN)"
                    else -> "falta permitir o bloqueio"
                },
                checked = settings.doubleTapLock,
                onCheckedChange = { enable ->
                    if (!enable) {
                        viewModel.setDoubleTapLock(false)
                        // Tira a autorização de administrador: assim o app pode ser desinstalado normalmente.
                        DeviceLock.deactivate(context)
                        adminActive = false
                    } else if (lockReady) {
                        viewModel.setDoubleTapLock(true)
                    } else {
                        showLockExplanation = true
                    }
                },
            )
            if (settings.doubleTapLock && !lockReady) {
                SettingRow(
                    title = "Permitir o bloqueio",
                    value = "abrir",
                    indent = true,
                    onClick = { showLockExplanation = true },
                )
            }

            // ---------------- Sistema ----------------
            SectionTitle("Sistema")
            SettingRow(
                title = "Tela inicial padrão",
                value = if (isDefault) "Afrodite" else "definir",
                onClick = { SystemActions.openHomeSettings(context) },
            )
            SettingRow(title = "Versão", value = BuildConfig.VERSION_NAME, onClick = null)

            Spacer(Modifier.height(48.dp))
        }

        // Seletor de apps (favoritos, dock, ocultos, gestos)
        val shownPicker = rememberLastNonNull(picker)
        AnimatedVisibility(
            visible = picker != null,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 12 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 12 },
        ) {
            if (shownPicker != null) {
                AppPicker(
                    request = shownPicker,
                    apps = state.allApps,
                    goldenIcons = settings.goldenIcons,
                    onDismiss = { picker = null },
                )
            }
        }

        // Explicação antes de mandar o usuário para a tela do Android que dá a permissão
        if (showLockExplanation) {
            if (useAccessibility) {
                GoldConfirmDialog(
                    title = "Bloquear com toque duplo",
                    message = "O Afrodite bloqueia a tela por meio de um serviço de acessibilidade.\n\n" +
                        "Ele serve apenas para bloquear a tela e abrir as notificações. Não lê o que " +
                        "aparece na tela, não registra o que você digita e não coleta nenhum dado.\n\n" +
                        "Na próxima tela, toque em \"Afrodite · bloqueio de tela\" e ative.",
                    confirmText = "Abrir acessibilidade",
                    onConfirm = {
                        showLockExplanation = false
                        viewModel.setDoubleTapLock(true)
                        SystemActions.openAccessibilitySettings(context)
                    },
                    onDismiss = { showLockExplanation = false },
                )
            } else {
                GoldConfirmDialog(
                    title = "Bloquear com toque duplo",
                    message = "Para bloquear a tela, o Android pede que o Afrodite seja autorizado como " +
                        "\"app de administração do dispositivo\". Ele usa só a permissão de bloquear a " +
                        "tela: não apaga dados, não muda senhas e não vê nada.\n\n" +
                        "Limitação do Android: depois de bloquear pelo toque duplo, a maioria dos " +
                        "celulares pede o PIN ou a senha no desbloqueio seguinte, em vez da digital.\n\n" +
                        "Para desinstalar o Afrodite no futuro, desligue esta opção antes.",
                    confirmText = "Permitir",
                    onConfirm = {
                        showLockExplanation = false
                        viewModel.setDoubleTapLock(true)
                        DeviceLock.requestActivation(context)
                    },
                    onDismiss = { showLockExplanation = false },
                )
            }
        }
    }
}

// ======================================================================================
// Peças da tela
// ======================================================================================

@Composable
private fun SectionTitle(text: String) {
    val gold = LocalGold.current
    Column(Modifier.padding(top = 32.dp, bottom = 6.dp)) {
        Text(
            text = text.uppercase(),
            color = gold.primary,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(10.dp))
        GoldDivider(Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String?,
    onClick: (() -> Unit)?,
    indent: Boolean = false,
) {
    val gold = LocalGold.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = if (indent) 16.dp else 0.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = if (indent) TextGray.copy(alpha = 0.8f) else TextGray,
            style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        )
        if (value != null) {
            Text(
                text = value,
                modifier = Modifier.widthIn(max = 170.dp),
                color = gold.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) = SettingRow(title = title, value = value, onClick = null)

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val gold = LocalGold.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextGray, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = gold.secondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Black,
                checkedTrackColor = gold.primary,
                checkedBorderColor = gold.primary,
                uncheckedThumbColor = gold.secondary,
                uncheckedTrackColor = Black,
                uncheckedBorderColor = gold.secondary,
            ),
        )
    }
}

/** Favorito na lista de configurações, com "subir" e "remover". */
@Composable
private fun ReorderRow(
    label: String,
    canMoveUp: Boolean,
    onMoveUp: () -> Unit,
    onRemove: () -> Unit,
) {
    val gold = LocalGold.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = TextGray.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Subir",
                tint = if (canMoveUp) gold.primary else gold.secondary.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remover", tint = gold.secondary, modifier = Modifier.size(18.dp))
        }
    }
}

/** Três opções de tom, cada uma com uma amostra da cor. */
@Composable
private fun ToneSelector(selected: GoldTone, onSelect: (GoldTone) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GoldTone.entries.forEach { tone ->
            val palette = tone.palette()
            val isSelected = tone == selected
            val shape = RoundedCornerShape(4.dp)
            Column(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .border(0.5.dp, if (isSelected) palette.primary else Color(0xFF262626), shape)
                    .clickable { onSelect(tone) }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(palette.primary, palette.secondary))),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tone.label,
                    color = if (isSelected) palette.primary else TextGray.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
