package com.netero.afrodite.ui.drawer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netero.afrodite.ui.theme.LocalGold

/** Letras da barra lateral: '#' (números e símbolos) e A–Z. */
val ALPHABET: List<Char> = listOf('#') + ('A'..'Z').toList()

/**
 * Barra A–Z dourada. Tocar ou arrastar o dedo sobre ela pula para a letra.
 * Letras sem nenhum app ficam apagadas.
 */
@Composable
fun AlphabetBar(
    available: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGold.current
    val haptics = LocalHapticFeedback.current
    var active by remember { mutableStateOf<Char?>(null) }
    val latestOnSelected by rememberUpdatedState(onLetterSelected)
    val latestOnRelease by rememberUpdatedState(onRelease)

    Column(
        modifier
            .width(24.dp)
            .pointerInput(Unit) {
                fun select(y: Float) {
                    if (size.height == 0) return
                    val index = ((y / size.height) * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
                    val letter = ALPHABET[index]
                    if (letter != active) {
                        active = letter
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        latestOnSelected(letter)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    select(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        select(change.position.y)
                    }
                    active = null
                    latestOnRelease()
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ALPHABET.forEach { letter ->
            val color = when {
                letter == active -> gold.primary
                letter in available -> gold.primary.copy(alpha = 0.8f)
                else -> gold.secondary.copy(alpha = 0.35f)
            }
            Text(
                text = letter.toString(),
                color = color,
                fontSize = 10.sp,
                fontWeight = if (letter == active) FontWeight.Normal else FontWeight.Light,
            )
        }
    }
}

/** Primeiro índice da lista para cada letra. */
fun firstIndexByLetter(letters: List<Char>): Map<Char, Int> {
    val map = HashMap<Char, Int>()
    letters.forEachIndexed { index, letter -> if (letter !in map) map[letter] = index }
    return map
}

/** Índice para onde pular: a letra pedida ou, se não houver app com ela, a próxima que houver. */
fun indexForLetter(letter: Char, index: Map<Char, Int>): Int? {
    val start = ALPHABET.indexOf(letter).coerceAtLeast(0)
    for (i in start until ALPHABET.size) {
        index[ALPHABET[i]]?.let { return it }
    }
    for (i in start - 1 downTo 0) {
        index[ALPHABET[i]]?.let { return it }
    }
    return null
}
