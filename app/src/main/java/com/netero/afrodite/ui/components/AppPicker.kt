package com.netero.afrodite.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray
import com.netero.afrodite.util.TextUtils
import com.netero.afrodite.util.toast

/**
 * Pedido para abrir o seletor de apps.
 *
 * @param maxSelection 1 = escolha única (fecha ao tocar); mais que 1 = várias, com marcação.
 * @param allowNone mostra a opção "Nenhum" (só na escolha única).
 * @param onConfirm recebe as chaves escolhidas, na ordem em que foram marcadas.
 */
class PickerRequest(
    val title: String,
    val selected: List<String>,
    val maxSelection: Int,
    val allowNone: Boolean = false,
    val onConfirm: (List<String>) -> Unit,
)

/** Tela cheia para escolher um ou vários apps, com busca. */
@Composable
fun AppPicker(
    request: PickerRequest,
    apps: List<AppInfo>,
    goldenIcons: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val gold = LocalGold.current
    val single = request.maxSelection == 1
    var query by remember(request) { mutableStateOf("") }
    var selection by remember(request) { mutableStateOf(request.selected) }
    val results = remember(apps, query) { TextUtils.filter(apps, query) { it.searchKey } }

    fun finish(keys: List<String>) {
        request.onConfirm(keys)
        onDismiss()
    }

    // Na escolha múltipla, "Voltar" salva o que foi marcado.
    BackHandler { if (single) onDismiss() else finish(selection) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .consumeTouches()
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (single) onDismiss() else finish(selection) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = gold.primary)
            }
            Text(
                text = request.title,
                modifier = Modifier.weight(1f),
                color = gold.primary,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!single) {
                val counter = if (request.maxSelection == Int.MAX_VALUE) {
                    "${selection.size}"
                } else {
                    "${selection.size}/${request.maxSelection}"
                }
                Text(counter, color = gold.secondary, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "OK",
                    color = gold.primary,
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { finish(selection) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }

        GoldSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "buscar",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
        ) {
            if (single && request.allowNone && query.isBlank()) {
                item(key = "__nenhum__") {
                    PickerRow(
                        label = "Nenhum",
                        checked = request.selected.isEmpty(),
                        icon = null,
                        onClick = { finish(emptyList()) },
                    )
                }
            }
            items(results, key = { it.key }) { app ->
                val checked = app.key in selection
                PickerRow(
                    label = app.label,
                    checked = checked,
                    icon = { AppIcon(app, goldenIcons, 28.dp) },
                    onClick = {
                        when {
                            single -> finish(listOf(app.key))
                            checked -> selection = selection - app.key
                            selection.size >= request.maxSelection ->
                                context.toast("Limite de ${request.maxSelection} apps")
                            else -> selection = selection + app.key
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    checked: Boolean,
    icon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val gold = LocalGold.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(16.dp))
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (checked) gold.primary else TextGray,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = "Selecionado", tint = gold.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
