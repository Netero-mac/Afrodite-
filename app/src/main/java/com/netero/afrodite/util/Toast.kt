package com.netero.afrodite.util

import android.content.Context
import android.widget.Toast

/** Atalho para mostrar uma mensagem curta na tela. */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
