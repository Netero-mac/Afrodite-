package com.netero.afrodite.data

/** Os três tons de dourado que o usuário pode escolher. */
enum class GoldTone(val label: String) {
    CLARO("Claro"),
    CLASSICO("Clássico"),
    ENVELHECIDO("Envelhecido");

    companion object {
        fun fromName(name: String?): GoldTone =
            entries.firstOrNull { it.name == name } ?: CLASSICO
    }
}

/**
 * Todas as preferências do launcher, como são lidas do DataStore.
 * Os apps são guardados pela chave "pacote/atividade" (veja [AppInfo.key]).
 */
data class LauncherSettings(
    val favorites: List<String> = emptyList(),
    /** Sempre [DOCK_SIZE] posições; posição vazia = "". */
    val dock: List<String> = List(DOCK_SIZE) { "" },
    val hidden: Set<String> = emptySet(),
    val use24h: Boolean = true,
    val showDock: Boolean = true,
    val goldenIcons: Boolean = true,
    val goldTone: GoldTone = GoldTone.CLASSICO,
    val swipeLeftApp: String = "",
    val swipeRightApp: String = "",
    val doubleTapLock: Boolean = false,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val MAX_FAVORITES = 6
        const val DOCK_SIZE = 4
    }
}
