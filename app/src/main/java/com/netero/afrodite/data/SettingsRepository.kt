package com.netero.afrodite.data

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Arquivo único do DataStore com as preferências do launcher. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "afrodite_settings")

/**
 * Lê e grava as configurações do Afrodite usando o DataStore (Preferences).
 * Listas ordenadas (favoritos e dock) são guardadas como texto separado por quebra de linha.
 */
class SettingsRepository(private val context: Context) {

    /** Fluxo com as configurações atuais; emite de novo a cada alteração. */
    val settings: Flow<LauncherSettings> = context.dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs -> prefs.toSettings() }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = this[FAVORITES].toKeyList().take(LauncherSettings.MAX_FAVORITES),
        dock = toDockSlots(this[DOCK]),
        hidden = this[HIDDEN] ?: emptySet(),
        // Na primeira vez, segue o formato de hora do próprio celular.
        use24h = this[USE_24H] ?: DateFormat.is24HourFormat(context),
        showDock = this[SHOW_DOCK] ?: true,
        goldenIcons = this[GOLDEN_ICONS] ?: true,
        goldTone = GoldTone.fromName(this[GOLD_TONE]),
        swipeLeftApp = this[SWIPE_LEFT].orEmpty(),
        swipeRightApp = this[SWIPE_RIGHT].orEmpty(),
        doubleTapLock = this[DOUBLE_TAP_LOCK] ?: false,
        onboardingDone = this[ONBOARDING_DONE] ?: false,
    )

    suspend fun setFavorites(keys: List<String>) = edit {
        it[FAVORITES] = keys.distinct().take(LauncherSettings.MAX_FAVORITES).joinToString(SEPARATOR)
    }

    suspend fun setDock(slots: List<String>) = edit {
        it[DOCK] = normalizeDock(slots).joinToString(SEPARATOR)
    }

    suspend fun setHidden(keys: Set<String>) = edit { it[HIDDEN] = keys }

    suspend fun setUse24h(value: Boolean) = edit { it[USE_24H] = value }

    suspend fun setShowDock(value: Boolean) = edit { it[SHOW_DOCK] = value }

    suspend fun setGoldenIcons(value: Boolean) = edit { it[GOLDEN_ICONS] = value }

    suspend fun setGoldTone(tone: GoldTone) = edit { it[GOLD_TONE] = tone.name }

    suspend fun setSwipeLeftApp(key: String) = edit { it[SWIPE_LEFT] = key }

    suspend fun setSwipeRightApp(key: String) = edit { it[SWIPE_RIGHT] = key }

    suspend fun setDoubleTapLock(value: Boolean) = edit { it[DOUBLE_TAP_LOCK] = value }

    suspend fun setOnboardingDone(value: Boolean) = edit { it[ONBOARDING_DONE] = value }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }

    private companion object {
        const val SEPARATOR = "\n"

        val FAVORITES = stringPreferencesKey("favorites")
        val DOCK = stringPreferencesKey("dock")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val USE_24H = booleanPreferencesKey("use_24h")
        val SHOW_DOCK = booleanPreferencesKey("show_dock")
        val GOLDEN_ICONS = booleanPreferencesKey("golden_icons")
        val GOLD_TONE = stringPreferencesKey("gold_tone")
        val SWIPE_LEFT = stringPreferencesKey("swipe_left_app")
        val SWIPE_RIGHT = stringPreferencesKey("swipe_right_app")
        val DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        fun String?.toKeyList(): List<String> =
            this?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

        fun toDockSlots(raw: String?): List<String> =
            normalizeDock(raw?.split(SEPARATOR) ?: emptyList())

        /** Garante exatamente [LauncherSettings.DOCK_SIZE] posições. */
        fun normalizeDock(slots: List<String>): List<String> =
            List(LauncherSettings.DOCK_SIZE) { index -> slots.getOrNull(index).orEmpty() }
    }
}
