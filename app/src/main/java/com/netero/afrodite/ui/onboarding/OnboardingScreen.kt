package com.netero.afrodite.ui.onboarding

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.system.SystemActions
import com.netero.afrodite.ui.components.GoldButton
import com.netero.afrodite.ui.components.GoldDivider
import com.netero.afrodite.ui.components.OnResume
import com.netero.afrodite.ui.components.consumeTouches
import com.netero.afrodite.ui.components.quietClickable
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray

/**
 * Primeira abertura: convida o usuário a definir o Afrodite como tela inicial padrão.
 * Some sozinha quando o Afrodite vira o padrão (ou quando o usuário toca em "agora não").
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val gold = LocalGold.current
    val latestOnFinish by rememberUpdatedState(onFinish)
    var isDefault by remember { mutableStateOf(SystemActions.isDefaultLauncher(context)) }

    OnResume { isDefault = SystemActions.isDefaultLauncher(context) }
    LaunchedEffect(isDefault) { if (isDefault) latestOnFinish() }

    // Janelinha do sistema "Usar o Afrodite como app de início?" (Android 10+)
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = SystemActions.isDefaultLauncher(context)
        if (!isDefault) SystemActions.openHomeSettings(context)
    }

    fun askToBeDefault() {
        val intent = SystemActions.requestDefaultLauncherIntent(context)
        if (intent == null) {
            SystemActions.openHomeSettings(context)
            return
        }
        try {
            roleRequest.launch(intent)
        } catch (e: ActivityNotFoundException) {
            SystemActions.openHomeSettings(context)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .consumeTouches()
            .systemBarsPadding()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "AFRODITE",
            color = gold.primary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 10.sp,
        )
        Spacer(Modifier.height(20.dp))
        GoldDivider(Modifier.width(140.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            text = "minimalismo em preto e ouro",
            color = gold.secondary,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.weight(0.7f))

        Text(
            text = "Para usar o Afrodite como sua tela inicial, defina-o como app de início padrão. " +
                "Dá para mudar quando quiser nas configurações do Android.",
            color = TextGray,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        GoldButton(text = "Definir como padrão", onClick = { askToBeDefault() })
        Spacer(Modifier.height(8.dp))
        Text(
            text = "agora não",
            color = gold.secondary,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp,
            modifier = Modifier
                .quietClickable(onFinish)
                .padding(16.dp),
        )

        Spacer(Modifier.weight(1f))

        // Resumo dos gestos
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GestureHint("↑", "deslize para cima: todos os apps")
            GestureHint("↓", "deslize para baixo: notificações")
            GestureHint("•", "toque longo: configurações")
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun GestureHint(symbol: String, text: String) {
    val gold = LocalGold.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = gold.primary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(12.dp))
        Text(text, color = gold.secondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
    }
}
