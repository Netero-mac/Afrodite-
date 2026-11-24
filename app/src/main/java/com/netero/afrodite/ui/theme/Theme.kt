package com.netero.afrodite.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import com.netero.afrodite.data.GoldTone

/** Dourados atuais, disponíveis em qualquer tela via `LocalGold.current`. */
val LocalGold = compositionLocalOf { GoldTone.CLASSICO.palette() }

/**
 * Tema do Afrodite: fundo preto, texto cinza claro e dourado nos detalhes.
 * Ao trocar o tom do dourado, as cores mudam com uma transição suave.
 */
@Composable
fun AfroditeTheme(tone: GoldTone, content: @Composable () -> Unit) {
    val target = tone.palette()
    val primary by animateColorAsState(target.primary, animationSpec = tween(500), label = "ouro")
    val secondary by animateColorAsState(target.secondary, animationSpec = tween(500), label = "ouroEscuro")

    val colors = darkColorScheme(
        primary = primary,
        onPrimary = Black,
        primaryContainer = Black,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Black,
        background = Black,
        onBackground = TextGray,
        surface = Black,
        onSurface = TextGray,
        surfaceVariant = NearBlack,
        onSurfaceVariant = secondary,
        surfaceContainerLowest = Black,
        surfaceContainerLow = NearBlack,
        surfaceContainer = NearBlack,
        surfaceContainerHigh = NearBlack,
        surfaceContainerHighest = NearBlack,
        outline = secondary,
        outlineVariant = secondary.copy(alpha = 0.4f),
    )

    CompositionLocalProvider(LocalGold provides GoldPalette(primary, secondary)) {
        MaterialTheme(colorScheme = colors, typography = AfroditeTypography) {
            CompositionLocalProvider(LocalContentColor provides TextGray, content = content)
        }
    }
}
