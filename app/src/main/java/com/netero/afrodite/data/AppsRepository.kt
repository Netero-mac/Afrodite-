package com.netero.afrodite.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Fonte da lista de apps instalados.
 *
 * - Usa a API [LauncherApps], feita para launchers.
 * - Guarda a última lista em disco: na próxima abertura ela aparece na hora,
 *   enquanto a lista atualizada é carregada em segundo plano.
 * - Escuta instalações, remoções e atualizações e se atualiza sozinha.
 * - Converte os ícones em bitmap fora da thread principal e guarda num cache em memória.
 */
class AppsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val user: UserHandle = Process.myUserHandle()
    private val cacheFile = File(context.filesDir, CACHE_FILE)
    private val refreshMutex = Mutex()

    private val _apps = MutableStateFlow<List<AppInfo>?>(null)

    /** Lista de apps em ordem alfabética; `null` enquanto ainda não carregou nada. */
    val apps: StateFlow<List<AppInfo>?> = _apps.asStateFlow()

    /** Informações do sistema da última consulta, usadas para pegar os ícones. */
    @Volatile
    private var activityInfos: Map<String, LauncherActivityInfo> = emptyMap()

    private val iconSizePx = (ICON_SIZE_DP * context.resources.displayMetrics.density).roundToInt()

    /** Cache de ícones limitado por memória (bytes), não por quantidade. */
    private val iconCache = object : LruCache<String, AppIcons>(cacheSizeBytes()) {
        override fun sizeOf(key: String, value: AppIcons): Int =
            value.original.byteCount() + (value.monochrome?.byteCount() ?: 0)
    }

    private var prewarmJob: Job? = null

    /** Avisos do sistema sobre apps instalados, removidos ou atualizados. */
    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, profile: UserHandle) =
            onPackagesChanged(packageName)

        override fun onPackageAdded(packageName: String, profile: UserHandle) =
            onPackagesChanged(packageName)

        override fun onPackageChanged(packageName: String, profile: UserHandle) =
            onPackagesChanged(packageName)

        override fun onPackagesAvailable(packageNames: Array<out String>, profile: UserHandle, replacing: Boolean) =
            onPackagesChanged(*packageNames)

        override fun onPackagesUnavailable(packageNames: Array<out String>, profile: UserHandle, replacing: Boolean) =
            onPackagesChanged(*packageNames)
    }

    /** Chamado uma vez pela Application. */
    fun start() {
        launcherApps.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        scope.launch(Dispatchers.IO) {
            // 1) Mostra a lista salva (instantâneo)…
            val cached = readCache()
            if (cached != null && _apps.value == null) _apps.value = cached
            // 2) …e busca a lista real em seguida.
            refresh()
        }
    }

    /** Consulta o sistema de novo e atualiza a lista e o cache em disco. */
    suspend fun refresh() {
        refreshMutex.withLock {
            val fresh = withContext(Dispatchers.IO) {
                runCatching { queryApps() }
                    .onFailure { Log.w(TAG, "Falha ao listar os apps", it) }
                    .getOrNull()
            } ?: return
            _apps.value = fresh
            withContext(Dispatchers.IO) { writeCache(fresh) }
        }
        prewarmIcons()
    }

    fun cachedIcons(key: String): AppIcons? = iconCache.get(key)

    /** Devolve os ícones do app, carregando em segundo plano se ainda não estiverem no cache. */
    suspend fun loadIcons(app: AppInfo): AppIcons? {
        iconCache.get(app.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val icons = runCatching { renderIcons(app) }
                .onFailure { Log.w(TAG, "Sem ícone para ${app.key}", it) }
                .getOrNull()
            if (icons != null) iconCache.put(app.key, icons)
            icons
        }
    }

    // ----------------------------------------------------------------------------------
    // Consulta ao sistema
    // ----------------------------------------------------------------------------------

    private fun queryApps(): List<AppInfo> {
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        val infos = HashMap<String, LauncherActivityInfo>()
        val list = launcherApps.getActivityList(null, user).mapNotNull { info ->
            val component = info.componentName
            // O próprio Afrodite não aparece na gaveta.
            if (component.packageName == context.packageName) return@mapNotNull null
            val label = info.label.toString().trim().ifEmpty { component.packageName }
            val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val app = AppInfo(component.packageName, component.className, label, isSystem)
            infos[app.key] = info
            app
        }
        activityInfos = infos
        return list
            .distinctBy { it.key }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    private fun onPackagesChanged(vararg packageNames: String) {
        // Descarta os ícones antigos desses pacotes (o ícone pode ter mudado numa atualização).
        val keys = iconCache.snapshot().keys
        packageNames.forEach { pkg ->
            keys.filter { it.startsWith("$pkg/") }.forEach { iconCache.remove(it) }
        }
        scope.launch { refresh() }
    }

    // ----------------------------------------------------------------------------------
    // Ícones
    // ----------------------------------------------------------------------------------

    private fun renderIcons(app: AppInfo): AppIcons {
        val densityDpi = context.resources.displayMetrics.densityDpi
        val drawable: Drawable = activityInfos[app.key]?.getIcon(densityDpi)
            ?: context.packageManager.getActivityIcon(app.componentName)

        val original = drawable.toBitmap(iconSizePx, iconSizePx).asImageBitmap()

        // Android 13+: ícones adaptativos podem ter uma camada monocromática (a mesma dos
        // "ícones temáticos"). Ela fica perfeita pintada de dourado.
        val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
            drawable.monochrome?.let { layer ->
                val copy = layer.constantState?.newDrawable()?.mutate() ?: layer
                AdaptiveIconDrawable(ColorDrawable(Color.TRANSPARENT), copy)
                    .toBitmap(iconSizePx, iconSizePx)
                    .asImageBitmap()
            }
        } else {
            null
        }
        return AppIcons(original, monochrome)
    }

    /** Carrega os ícones de todos os apps aos poucos, para a gaveta abrir já com tudo pronto. */
    private fun prewarmIcons() {
        prewarmJob?.cancel()
        prewarmJob = scope.launch(Dispatchers.IO) {
            for (app in _apps.value.orEmpty()) {
                if (iconCache.size() > iconCache.maxSize() * 0.9) break
                if (iconCache.get(app.key) == null) {
                    loadIcons(app)
                    yield()
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // Cache em disco (JSON simples)
    // ----------------------------------------------------------------------------------

    private fun readCache(): List<AppInfo>? {
        if (!cacheFile.exists()) return null
        return runCatching {
            val array = JSONArray(cacheFile.readText())
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                AppInfo(
                    packageName = item.getString("p"),
                    activityName = item.getString("a"),
                    label = item.getString("l"),
                    isSystem = item.optBoolean("s"),
                )
            }
        }.getOrNull()
    }

    private fun writeCache(apps: List<AppInfo>) {
        runCatching {
            val array = JSONArray()
            apps.forEach { app ->
                array.put(
                    JSONObject()
                        .put("p", app.packageName)
                        .put("a", app.activityName)
                        .put("l", app.label)
                        .put("s", app.isSystem),
                )
            }
            // Grava num arquivo temporário e renomeia: nunca deixa um cache pela metade.
            val tmp = File(context.filesDir, "$CACHE_FILE.tmp")
            tmp.writeText(array.toString())
            tmp.renameTo(cacheFile)
        }.onFailure { Log.w(TAG, "Falha ao salvar o cache de apps", it) }
    }

    private companion object {
        const val TAG = "AppsRepository"
        const val CACHE_FILE = "apps_cache.json"
        const val ICON_SIZE_DP = 44

        /** Até 1/8 da memória do app, no máximo 48 MB. */
        fun cacheSizeBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(48L * 1024 * 1024).toInt()

        fun ImageBitmap.byteCount(): Int = asAndroidBitmap().byteCount
    }
}
