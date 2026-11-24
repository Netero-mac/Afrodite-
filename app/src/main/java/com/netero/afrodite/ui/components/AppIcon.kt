package com.netero.afrodite.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.Dp
import com.netero.afrodite.data.AppIcons
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.ui.theme.LocalGold

/** Quem sabe carregar ícones (o ViewModel entrega uma implementação). */
interface IconLoader {
    fun cached(app: AppInfo): AppIcons?
    suspend fun load(app: AppInfo): AppIcons?
}

val LocalIconLoader = staticCompositionLocalOf<IconLoader?> { null }

/**
 * Ícone de um app.
 *
 * Modo dourado:
 * - se o app tem ícone monocromático (Android 13+), ele é pintado de dourado;
 * - senão, o ícone colorido vira "duotone": a luminosidade de cada pixel é
 *   multiplicada pelo dourado, preservando os detalhes do desenho.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    golden: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val loader = LocalIconLoader.current
    val icons by produceState(initialValue = loader?.cached(app), app.key, loader) {
        value = loader?.let { it.cached(app) ?: it.load(app) }
    }
    val gold = LocalGold.current.primary
    val duotone = remember(gold) { goldDuotone(gold) }

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val current = icons
        val fill = Modifier.fillMaxSize()
        when {
            current == null -> Unit
            !golden -> Image(
                bitmap = current.original,
                contentDescription = null,
                modifier = fill,
                filterQuality = FilterQuality.Medium,
            )
            current.monochrome != null -> Image(
                bitmap = current.monochrome,
                contentDescription = null,
                modifier = fill,
                colorFilter = ColorFilter.tint(gold),
                filterQuality = FilterQuality.Medium,
            )
            else -> Image(
                bitmap = current.original,
                contentDescription = null,
                modifier = fill,
                colorFilter = duotone,
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** Matriz de cor: luminância (Rec. 709) × dourado, mantendo a transparência. */
private fun goldDuotone(gold: Color): ColorFilter {
    val gain = 1.15f // um pouco mais de brilho para não ficar apagado no preto
    val lr = 0.2126f * gain
    val lg = 0.7152f * gain
    val lb = 0.0722f * gain
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                gold.red * lr, gold.red * lg, gold.red * lb, 0f, 0f,
                gold.green * lr, gold.green * lg, gold.green * lb, 0f, 0f,
                gold.blue * lr, gold.blue * lg, gold.blue * lb, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
