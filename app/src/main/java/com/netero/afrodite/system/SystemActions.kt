package com.netero.afrodite.system

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import com.netero.afrodite.data.AppInfo
import com.netero.afrodite.util.toast

/**
 * Tudo que o launcher pede ao sistema Android: abrir apps, telas de configuração,
 * notificações, bloqueio de tela etc. Fica separado da interface para manter as
 * telas simples.
 */
object SystemActions {

    private const val TAG = "SystemActions"

    /** Abre um app. Devolve `false` se não conseguiu (ex.: foi desinstalado agora). */
    fun launchApp(context: Context, app: AppInfo): Boolean = try {
        context.getSystemService(LauncherApps::class.java)
            .startMainActivity(app.componentName, Process.myUserHandle(), null, null)
        true
    } catch (e: Exception) {
        Log.w(TAG, "Não foi possível abrir ${app.key}", e)
        context.toast("Não foi possível abrir ${app.label}")
        false
    }

    /** Tela "Informações do app" do sistema. */
    fun openAppInfo(context: Context, app: AppInfo) {
        try {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(app.componentName, Process.myUserHandle(), null, null)
        } catch (e: Exception) {
            start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null)))
        }
    }

    /** Pede ao sistema para desinstalar (o Android mostra a confirmação). */
    fun uninstall(context: Context, app: AppInfo) {
        start(context, Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
    }

    /**
     * Abre a barra de notificações.
     * 1º tenta pelo serviço de acessibilidade (se estiver ativo);
     * 2º usa o StatusBarManager por reflexão, como a maioria dos launchers faz.
     */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotifications(context: Context) {
        if (LockAccessibilityService.openNotifications()) return
        try {
            val service = context.getSystemService("statusbar") ?: return
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível abrir as notificações", e)
        }
    }

    /** Bloqueia a tela pelo serviço de acessibilidade, avisando o usuário se não der. */
    fun lockScreen(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            context.toast("O bloqueio por toque duplo precisa do Android 9 ou mais novo")
            return false
        }
        if (LockAccessibilityService.lockScreen()) return true
        context.toast("Ative o serviço de acessibilidade do Afrodite nas configurações")
        return false
    }

    /** Toque no relógio: abre os alarmes. */
    fun openAlarms(context: Context) {
        start(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    /** Toque na data: abre a agenda no dia de hoje. */
    fun openCalendar(context: Context) {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        ContentUris.appendId(builder, System.currentTimeMillis())
        start(context, Intent(Intent.ACTION_VIEW, builder.build()))
    }

    /** O Afrodite é a tela inicial padrão? */
    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /**
     * Intent da janelinha "Usar o Afrodite como app de início?" (Android 10+).
     * Devolve `null` em versões antigas; nesse caso use [openHomeSettings].
     */
    fun requestDefaultLauncherIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return null
    }

    /** Tela do sistema onde se escolhe o app de início. */
    fun openHomeSettings(context: Context) {
        if (start(context, Intent(Settings.ACTION_HOME_SETTINGS), showError = false)) return
        if (start(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), showError = false)) return
        start(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openAccessibilitySettings(context: Context) {
        start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun start(context: Context, intent: Intent, showError: Boolean = true): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        if (showError) context.toast("Nenhum app disponível para isso")
        false
    } catch (e: SecurityException) {
        if (showError) context.toast("O Android não permitiu abrir essa tela")
        false
    }
}
