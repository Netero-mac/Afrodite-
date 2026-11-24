package com.netero.afrodite.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netero.afrodite.ui.theme.Black
import com.netero.afrodite.ui.theme.LocalGold
import com.netero.afrodite.ui.theme.TextGray

// ======================================================================================
// Modificadores
// ======================================================================================

/**
 * Faz uma camada "segurar" os toques. Sem isso, toques em áreas vazias de uma camada
 * (gaveta, configurações) atravessariam e acionariam os gestos da tela inicial.
 */
fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/** Clique sem o efeito de onda (ripple), para manter o visual limpo. */
fun Modifier.quietClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

// ======================================================================================
// Efeitos
// ======================================================================================

/** Executa [action] sempre que a tela volta para o primeiro plano. */
@Composable
fun OnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val currentAction by rememberUpdatedState(action)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentAction()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Lembra o último valor não nulo. Útil para animações de saída: o conteúdo continua
 * aparecendo enquanto some, mesmo depois que o estado virou `null`.
 */
@Composable
fun <T : Any> rememberLastNonNull(value: T?): T? {
    val holder = remember { LastValueHolder<T>() }
    if (value != null) holder.value = value
    return holder.value
}

private class LastValueHolder<T : Any> {
    var value: T? = null
}

// ======================================================================================
// Elementos visuais
// ======================================================================================

/** Linha dourada bem fina, que desaparece nas pontas. */
@Composable
fun GoldDivider(modifier: Modifier = Modifier, thickness: Dp = 0.5.dp) {
    val gold = LocalGold.current.primary
    Box(
        modifier
            .height(thickness)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, gold, Color.Transparent))),
    )
}

/** Botão com contorno dourado fino e texto espaçado. */
@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val gold = LocalGold.current
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .clip(shape)
            .border(0.5.dp, gold.primary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = gold.primary,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 3.sp,
        )
    }
}

/** Campo de busca minimalista: texto cinza, cursor e linha dourados. */
@Composable
fun GoldSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onGo: (() -> Unit)? = null,
) {
    val gold = LocalGold.current
    val textStyle = MaterialTheme.typography.titleMedium.copy(color = TextGray, letterSpacing = 1.sp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(gold.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = if (onGo != null) ImeAction.Go else ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onGo = { onGo?.invoke() }),
        decorationBox = { innerTextField ->
            Column {
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = textStyle, color = gold.secondary)
                    }
                    innerTextField()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(gold.secondary),
                )
            }
        },
    )
}

/** Diálogo de confirmação no estilo do launcher (preto com borda dourada). */
@Composable
fun GoldConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val gold = LocalGold.current
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.88f))
            .quietClickable(onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .border(0.5.dp, gold.secondary, shape)
                .background(Black, shape)
                .consumeTouches()
                .padding(24.dp),
        ) {
            Text(title, color = gold.primary, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            Text(message, color = TextGray, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                DialogAction("Cancelar", gold.secondary, onDismiss)
                Spacer(Modifier.width(8.dp))
                DialogAction(confirmText, gold.primary, onConfirm)
            }
        }
    }
}

@Composable
private fun DialogAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text.uppercase(),
        color = color,
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 2.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
