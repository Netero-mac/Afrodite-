package com.netero.afrodite.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.netero.afrodite.R

/**
 * Montserrat em pesos finos (licença SIL Open Font, veja /licenses).
 * Os arquivos ficam em res/font.
 */
val Montserrat = FontFamily(
    Font(R.font.montserrat_extralight, FontWeight.ExtraLight),
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_regular, FontWeight.Normal),
)

private fun TextStyle.elegant() = copy(fontFamily = Montserrat, fontWeight = FontWeight.Light)

private val Base = Typography()

/** Toda a tipografia do Material 3 trocada pela Montserrat Light. */
val AfroditeTypography = Typography(
    displayLarge = Base.displayLarge.elegant(),
    displayMedium = Base.displayMedium.elegant(),
    displaySmall = Base.displaySmall.elegant(),
    headlineLarge = Base.headlineLarge.elegant(),
    headlineMedium = Base.headlineMedium.elegant(),
    headlineSmall = Base.headlineSmall.elegant(),
    titleLarge = Base.titleLarge.elegant(),
    titleMedium = Base.titleMedium.elegant(),
    titleSmall = Base.titleSmall.elegant(),
    bodyLarge = Base.bodyLarge.elegant(),
    bodyMedium = Base.bodyMedium.elegant(),
    bodySmall = Base.bodySmall.elegant(),
    labelLarge = Base.labelLarge.elegant(),
    labelMedium = Base.labelMedium.elegant(),
    labelSmall = Base.labelSmall.elegant(),
)
