package com.netero.afrodite.system

import android.accessibilityservice.AccessibilityService
import android.os.Build
import java.lang.ref.WeakReference

/**
 * Ponte entre o launcher e o serviço de acessibilidade.
 *
 * O serviço em si só existe na versão "completo" (src/completo). Na versão "padrão"
 * ninguém se registra aqui, e todas as funções simplesmente devolvem `false`.
 */
object AccessibilityBridge {

    @Volatile
    private var service: WeakReference<AccessibilityService>? = null

    fun attach(accessibilityService: AccessibilityService) {
        service = WeakReference(accessibilityService)
    }

    fun detach() {
        service = null
    }

    /** `true` quando o usuário ligou o serviço nas configurações de acessibilidade. */
    val isRunning: Boolean
        get() = service?.get() != null

    /** Bloqueia a tela mantendo a digital funcionando (Android 9+). */
    fun lockScreen(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return service?.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
    }

    /** Abre a barra de notificações. */
    fun openNotifications(): Boolean =
        service?.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
}
