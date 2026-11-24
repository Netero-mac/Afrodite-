package com.netero.afrodite.system

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.netero.afrodite.util.toast

/**
 * Receptor de "administrador do dispositivo". A única política declarada é
 * force-lock (bloquear a tela), em res/xml/device_admin.xml.
 */
class LockAdminReceiver : DeviceAdminReceiver()

/**
 * Bloqueio de tela pelo "administrador do dispositivo".
 *
 * É o jeito que funciona na versão "padrão", que não pode ter serviço de acessibilidade.
 * Limitação do Android: depois de um bloqueio feito assim, a maioria dos celulares pede
 * o PIN/senha no desbloqueio seguinte (a digital volta a funcionar depois disso).
 */
object DeviceLock {

    private fun component(context: Context) = ComponentName(context, LockAdminReceiver::class.java)

    private fun policyManager(context: Context): DevicePolicyManager? =
        context.getSystemService(DevicePolicyManager::class.java)

    /** O usuário já autorizou o Afrodite a bloquear a tela? */
    fun isActive(context: Context): Boolean =
        policyManager(context)?.isAdminActive(component(context)) == true

    /** Bloqueia a tela. Devolve `false` se ainda não tiver autorização. */
    fun lock(context: Context): Boolean {
        if (!isActive(context)) return false
        return runCatching { policyManager(context)?.lockNow() }.isSuccess
    }

    /** Abre a tela do Android que pede a autorização. */
    fun requestActivation(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "O Afrodite usa só a permissão de bloquear a tela, para o toque duplo.",
            )
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.toast("Este celular não permite essa autorização")
        }
    }

    /**
     * Remove a autorização. Importante: o Android não deixa desinstalar um app que é
     * administrador, então desligar o toque duplo já libera a desinstalação.
     */
    fun deactivate(context: Context) {
        runCatching {
            if (isActive(context)) policyManager(context)?.removeActiveAdmin(component(context))
        }
    }
}
