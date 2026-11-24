package com.netero.afrodite.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.netero.afrodite.ui.components.quietClickable
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.Montserrat
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Relógio grande em fonte fina dourada, com a data em letras pequenas embaixo.
 * Toque na hora abre os alarmes; toque na data abre a agenda.
 */
@Composable
fun Clock(
    use24h: Boolean,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val now by rememberCurrentMinute()
    val locale: Locale = LocalConfiguration.current.locales[0]

    val time = remember(now, use24h, locale) {
        now.format(DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm", locale))
    }
    val period = remember(now, use24h, locale) {
        if (use24h) null else now.format(DateTimeFormatter.ofPattern("a", locale))
    }
    val date = remember(now.toLocalDate(), locale) { formatDate(now, locale) }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.quietClickable(onTimeClick)) {
            Text(
                text = time,
                modifier = Modifier.alignByBaseline(),
                style = TextStyle(
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.ExtraLight,
                    fontSize = 92.sp,
                    letterSpacing = 2.sp,
                    // Números com a mesma largura: o relógio não "pula" quando os dígitos mudam.
                    fontFeatureSettings = "tnum",
                    color = gold.primary,
                ),
            )
            if (period != null) {
                Text(
                    text = period,
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = gold.secondary,
                )
            }
        }
        Text(
            text = date,
            modifier = Modifier
                .quietClickable(onDateClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 3.sp,
            color = gold.secondary,
        )
    }
}

/** Data no idioma do celular, sem o ano. Ex.: "SEGUNDA-FEIRA, 23 DE NOVEMBRO". */
private fun formatDate(now: LocalDateTime, locale: Locale): String {
    val text = runCatching {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        now.format(DateTimeFormatter.ofPattern(pattern, locale))
    }.getOrElse {
        now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    return text.uppercase(locale)
}

/**
 * Hora atual, atualizada na virada de cada minuto.
 * Só roda enquanto a tela está visível (economiza bateria).
 */
@Composable
private fun rememberCurrentMinute(): State<LocalDateTime> {
    val state = remember { mutableStateOf(LocalDateTime.now()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                state.value = LocalDateTime.now()
                val millis = System.currentTimeMillis()
                delay(60_000L - (millis % 60_000L) + 25L)
            }
        }
    }
    return state
}
