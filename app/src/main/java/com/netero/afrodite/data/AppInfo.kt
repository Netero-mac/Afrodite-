package com.netero.afrodite.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap
import com.netero.afrodite.util.TextUtils

/**
 * Um app que aparece na gaveta (uma atividade com ícone de launcher).
 *
 * O [key] ("pacote/atividade") é o identificador que salvamos nas configurações:
 * favoritos, dock, apps ocultos e apps dos gestos.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystem: Boolean,
) {
    val key: String = keyOf(packageName, activityName)

    /** Nome sem acentos e em minúsculas, usado na busca. */
    val searchKey: String = TextUtils.normalize(label)

    /** Letra da barra A–Z ('#' para números e símbolos). */
    val sectionLetter: Char = TextUtils.sectionLetter(label)

    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)

    companion object {
        fun keyOf(packageName: String, activityName: String) = "$packageName/$activityName"
    }
}

/**
 * Ícones já convertidos em bitmap, prontos para desenhar.
 *
 * @property monochrome camada monocromática do ícone adaptativo (Android 13+),
 *                      quando o app oferece uma. É a base dos ícones dourados.
 */
data class AppIcons(
    val original: ImageBitmap,
    val monochrome: ImageBitmap?,
)
