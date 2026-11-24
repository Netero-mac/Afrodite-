package com.netero.afrodite.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.ui.components.AppIcon
import com.netero.afrodite.ui.theme.LocalGold

/**
 * Dock com 4 posições. Posição vazia aparece como um anel dourado discreto;
 * tocar nela (ou segurar um ícone) abre o seletor de apps para aquela posição.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    slots: List<AppInfo?>,
    goldenIcons: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onPickSlot: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEachIndexed { index, app ->
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = { onPickSlot(index) },
                        onClick = { if (app != null) onAppClick(app) else onPickSlot(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (app != null) {
                    AppIcon(app = app, golden = goldenIcons, size = 40.dp)
                } else {
                    Box(
                        Modifier
                            .size(10.dp)
                            .border(0.5.dp, gold.secondary, CircleShape),
                    )
                }
            }
        }
    }
}
