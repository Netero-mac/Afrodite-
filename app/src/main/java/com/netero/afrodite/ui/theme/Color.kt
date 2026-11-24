package com.netero.afrodite.ui.theme

import androidx.compose.ui.graphics.Color
import com.netero.afrodite.data.GoldTone

/** Preto puro: em telas AMOLED o pixel fica apagado e não gasta bateria. */
val Black = Color(0xFF000000)

/** Quase preto, só para superfícies que precisam se destacar um tiquinho. */
val NearBlack = Color(0xFF0A0A0A)

/** Cor do texto principal (nomes dos apps). */
val TextGray = Color(0xFFE0E0E0)

/** Par de dourados: [primary] para destaques e [secondary] para itens secundários. */
data class GoldPalette(
    val primary: Color,
    val secondary: Color,
)

/** Converte o tom escolhido nas configurações no par de cores. */
fun GoldTone.palette(): GoldPalette = when (this) {
    GoldTone.CLARO -> GoldPalette(primary = Color(0xFFE9CF7F), secondary = Color(0xFFA08A50))
    GoldTone.CLASSICO -> GoldPalette(primary = Color(0xFFD4AF37), secondary = Color(0xFF8C7330))
    GoldTone.ENVELHECIDO -> GoldPalette(primary = Color(0xFFB59559), secondary = Color(0xFF6F5C37))
}
