package com.netero.afrodite.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.ui.components.AppIcon
import com.netero.afrodite.ui.components.GoldDivider
import com.netero.afrodite.ui.components.consumeTouches
import com.netero.afrodite.ui.components.quietClickable
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray

/** Menu do toque longo num app da gaveta. */
@Composable
fun AppActionsMenu(
    app: AppInfo,
    isFavorite: Boolean,
    goldenIcons: Boolean,
    onDismiss: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToDock: () -> Unit,
    onHide: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val gold = LocalGold.current
    val shape = RoundedCornerShape(4.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.86f))
            .quietClickable(onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .border(0.5.dp, gold.secondary, shape)
                .background(Black, shape)
                .consumeTouches()
                .padding(vertical = 20.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, golden = goldenIcons, size = 36.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    text = app.label,
                    color = gold.primary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            GoldDivider(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            MenuItem("Informações do app", onInfo)
            if (!app.isSystem) MenuItem("Desinstalar", onUninstall)
            MenuItem(if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos", onToggleFavorite)
            MenuItem("Adicionar ao dock", onAddToDock)
            MenuItem("Ocultar da gaveta", onHide)
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        color = TextGray,
        style = MaterialTheme.typography.bodyLarge,
        letterSpacing = 0.5.sp,
    )
}
