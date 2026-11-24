package com.netero.afrodite.util

import java.text.Normalizer

/**
 * Funções de texto usadas na busca e na barra A–Z.
 * São Kotlin puro (sem Android), por isso têm testes de unidade.
 */
object TextUtils {

    private val COMBINING_MARKS = "\\p{Mn}+".toRegex()

    /** "  Câmera " -> "camera": sem acentos, minúsculo e sem espaços nas pontas. */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase()
            .trim()

    /** Letra da barra A–Z para um nome. O que não começa com A–Z vai para '#'. */
    fun sectionLetter(label: String): Char {
        val first = normalize(label).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (first in 'A'..'Z') first else '#'
    }

    /**
     * Filtra uma lista pela busca, mantendo a ordem alfabética dentro de cada grupo:
     * 1º os nomes que começam com o texto, 2º os que têm uma palavra começando com ele,
     * 3º os que só contêm o texto em algum lugar.
     *
     * @param key função que devolve o texto já normalizado de cada item.
     */
    fun <T> filter(items: List<T>, query: String, key: (T) -> String): List<T> {
        val q = normalize(query)
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item ->
                val k = key(item)
                val rank = when {
                    k.startsWith(q) -> 0
                    k.split(' ').any { it.startsWith(q) } -> 1
                    k.contains(q) -> 2
                    else -> null
                }
                rank?.let { item to it }
            }
            .sortedBy { it.second } // sortedBy é estável: a ordem alfabética se mantém
            .map { it.first }
    }
}
