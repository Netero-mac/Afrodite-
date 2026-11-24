package com.netero.afrodite.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Serviço de acessibilidade mínimo.
 *
 * O Android só deixa um app comum bloquear a tela por meio de um serviço de
 * acessibilidade (ação global LOCK_SCREEN, Android 9+). Este serviço não lê a tela
 * (canRetrieveWindowContent = false) e ignora todos os eventos: ele só executa
 * as ações "bloquear tela" e "abrir notificações" quando o launcher pede.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: WeakReference<LockAccessibilityService>? = null

        /** `true` quando o usuário ativou o serviço nas configurações de acessibilidade. */
        val isRunning: Boolean
            get() = instance?.get() != null

        /** Bloqueia a tela. Devolve `false` se o serviço estiver desligado ou o Android for antigo. */
        fun lockScreen(): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return instance?.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
        }

        /** Abre a barra de notificações (jeito mais confiável, quando o serviço está ativo). */
        fun openNotifications(): Boolean =
            instance?.get()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
    }
}
