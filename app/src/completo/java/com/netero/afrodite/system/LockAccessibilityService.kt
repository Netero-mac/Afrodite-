package com.netero.afrodite.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço de acessibilidade mínimo. Só existe na versão "completo".
 *
 * Ele não lê a tela (canRetrieveWindowContent = false) e ignora todos os eventos.
 * Ao ser ligado, se registra no [AccessibilityBridge], que o launcher usa para
 * "bloquear a tela" (Android 9+) e "abrir notificações".
 *
 * Fica fora da versão "padrão" porque o Google Play Protect bloqueia, no Brasil,
 * APKs baixados pelo navegador que declaram um serviço de acessibilidade.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityBridge.detach()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityBridge.detach()
        super.onDestroy()
    }
}
