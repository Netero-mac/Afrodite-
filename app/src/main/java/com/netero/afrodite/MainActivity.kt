package com.netero.afrodite

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.netero.afrodite.ui.AfroditeRoot
import com.netero.afrodite.ui.LauncherViewModel

/**
 * Única Activity do launcher. Ela é a tela inicial (intent-filter HOME no manifesto).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels { LauncherViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Desenha por baixo das barras do sistema, com ícones claros sobre o preto.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // O botão "Voltar" nunca sai do launcher: no máximo fecha a gaveta ou as
        // configurações. (Telas menores, como o seletor de apps, têm o próprio
        // BackHandler, que tem prioridade sobre este.)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.closeOverlay()
            }
        })

        setContent { AfroditeRoot(viewModel) }
    }

    /** Apertar "Início" com o launcher já aberto volta para a tela inicial. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) viewModel.closeOverlay()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onLauncherHidden()
    }
}
