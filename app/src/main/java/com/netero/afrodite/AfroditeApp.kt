package com.netero.afrodite

import android.app.Application
import com.netero.afrodite.data.AppsRepository
import com.netero.afrodite.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Classe Application do Afrodite.
 *
 * Cria os repositórios uma única vez (injeção de dependência manual, sem bibliotecas)
 * e começa a carregar a lista de apps assim que o processo nasce, antes mesmo da
 * tela aparecer.
 */
class AfroditeApp : Application() {

    /** Escopo de corrotinas que vive enquanto o processo do launcher estiver vivo. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appsRepository: AppsRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        appsRepository = AppsRepository(this, appScope).also { it.start() }
    }
}
