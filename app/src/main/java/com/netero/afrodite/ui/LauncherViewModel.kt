package com.netero.afrodite.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.netero.afrodite.AfroditeApp
import com.netero.afrodite.data.AppIcons
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.data.AppsRepository
import com.netero.afrodite.data.GoldTone
import com.netero.afrodite.data.LauncherSettings
import com.netero.afrodite.data.SettingsRepository
import com.netero.afrodite.ui.components.IconLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Qual camada está aberta por cima da tela inicial. */
enum class Overlay { NONE, DRAWER, SETTINGS }

/** Resultado de "Adicionar ao dock". */
enum class DockResult { ADDED, ALREADY_THERE, FULL }

/** Tudo que as telas precisam para se desenhar. */
data class LauncherUiState(
    /** `false` até a primeira lista de apps e as configurações carregarem. */
    val ready: Boolean = false,
    val settings: LauncherSettings = LauncherSettings(),
    /** Todos os apps (inclusive ocultos), em ordem alfabética. */
    val allApps: List<AppInfo> = emptyList(),
    /** Apps da gaveta (sem os ocultos). */
    val drawerApps: List<AppInfo> = emptyList(),
    val favorites: List<AppInfo> = emptyList(),
    /** Sempre 4 posições; `null` = posição vazia. */
    val dock: List<AppInfo?> = List(LauncherSettings.DOCK_SIZE) { null },
    val appsByKey: Map<String, AppInfo> = emptyMap(),
) {
    fun isFavorite(app: AppInfo) = favorites.any { it.key == app.key }
}

/**
 * ViewModel único do launcher (MVVM): junta a lista de apps com as configurações,
 * guarda qual tela está aberta e recebe as ações do usuário.
 */
class LauncherViewModel(
    private val appsRepository: AppsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<LauncherUiState> =
        combine(appsRepository.apps, settingsRepository.settings) { apps, settings -> buildState(apps, settings) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    private val _overlay = MutableStateFlow(Overlay.NONE)
    val overlay: StateFlow<Overlay> = _overlay.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Entregue às telas para carregar ícones sob demanda. */
    val iconLoader: IconLoader = object : IconLoader {
        override fun cached(app: AppInfo): AppIcons? = appsRepository.cachedIcons(app.key)
        override suspend fun load(app: AppInfo): AppIcons? = appsRepository.loadIcons(app)
    }

    // ---------------- Navegação ----------------

    fun openDrawer() {
        _overlay.value = Overlay.DRAWER
    }

    fun openSettings() {
        _query.value = ""
        _overlay.value = Overlay.SETTINGS
    }

    /** Volta para a tela inicial (botão Voltar, botão Início). */
    fun closeOverlay() {
        _overlay.value = Overlay.NONE
        _query.value = ""
    }

    /** O launcher saiu da tela (abriu um app, apagou a tela): fecha a gaveta e limpa a busca. */
    fun onLauncherHidden() {
        if (_overlay.value == Overlay.DRAWER) closeOverlay()
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    // ---------------- Favoritos ----------------

    /** Adiciona ou remove dos favoritos. Devolve `false` se o limite de 6 já foi atingido. */
    fun toggleFavorite(app: AppInfo): Boolean {
        val keys = favoriteKeys()
        val newKeys = when {
            app.key in keys -> keys - app.key
            keys.size >= LauncherSettings.MAX_FAVORITES -> return false
            else -> keys + app.key
        }
        persist { it.setFavorites(newKeys) }
        return true
    }

    fun setFavorites(keys: List<String>) = persist { it.setFavorites(keys) }

    fun moveFavoriteUp(key: String) {
        val keys = favoriteKeys().toMutableList()
        val index = keys.indexOf(key)
        if (index <= 0) return
        keys[index] = keys[index - 1]
        keys[index - 1] = key
        persist { it.setFavorites(keys) }
    }

    fun removeFavorite(key: String) {
        val keys = favoriteKeys() - key
        persist { it.setFavorites(keys) }
    }

    // ---------------- Dock ----------------

    /** Coloca um app numa posição do dock ("" esvazia). O mesmo app não fica repetido. */
    fun setDockSlot(index: Int, key: String) {
        val slots = current().settings.dock.toMutableList()
        if (index !in slots.indices) return
        if (key.isNotEmpty()) {
            val existing = slots.indexOf(key)
            if (existing >= 0) slots[existing] = ""
        }
        slots[index] = key
        persist { it.setDock(slots) }
    }

    fun addToDock(app: AppInfo): DockResult {
        val state = current()
        if (state.dock.any { it?.key == app.key }) return DockResult.ALREADY_THERE
        val free = state.dock.indexOfFirst { it == null }
        if (free < 0) return DockResult.FULL
        setDockSlot(free, app.key)
        if (!state.settings.showDock) persist { it.setShowDock(true) }
        return DockResult.ADDED
    }

    // ---------------- Apps ocultos ----------------

    fun hideApp(app: AppInfo) {
        val hidden = current().settings.hidden + app.key
        persist { it.setHidden(hidden) }
    }

    fun setHidden(keys: Set<String>) = persist { it.setHidden(keys) }

    // ---------------- Preferências simples ----------------

    fun setUse24h(value: Boolean) = persist { it.setUse24h(value) }
    fun setShowDock(value: Boolean) = persist { it.setShowDock(value) }
    fun setGoldenIcons(value: Boolean) = persist { it.setGoldenIcons(value) }
    fun setGoldTone(tone: GoldTone) = persist { it.setGoldTone(tone) }
    fun setSwipeLeftApp(key: String) = persist { it.setSwipeLeftApp(key) }
    fun setSwipeRightApp(key: String) = persist { it.setSwipeRightApp(key) }
    fun setDoubleTapLock(value: Boolean) = persist { it.setDoubleTapLock(value) }
    fun completeOnboarding() = persist { it.setOnboardingDone(true) }

    // ---------------- Internos ----------------

    private fun current() = uiState.value

    /** Favoritos que ainda existem (apps desinstalados saem da lista na próxima gravação). */
    private fun favoriteKeys() = current().favorites.map { it.key }

    private fun persist(block: suspend (SettingsRepository) -> Unit) {
        viewModelScope.launch { block(settingsRepository) }
    }

    private fun buildState(apps: List<AppInfo>?, settings: LauncherSettings): LauncherUiState {
        val list = apps.orEmpty()
        val byKey = list.associateBy { it.key }
        return LauncherUiState(
            ready = apps != null,
            settings = settings,
            allApps = list,
            drawerApps = if (settings.hidden.isEmpty()) list else list.filter { it.key !in settings.hidden },
            favorites = settings.favorites.mapNotNull { byKey[it] },
            dock = settings.dock.map { byKey[it] },
            appsByKey = byKey,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AfroditeApp
                LauncherViewModel(app.appsRepository, app.settingsRepository)
            }
        }
    }
}
