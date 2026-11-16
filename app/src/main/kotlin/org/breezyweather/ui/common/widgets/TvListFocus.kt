package org.breezyweather.ui.common.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged

/**
 * Remembers the option list of the screen currently shown, so the activity can send D-pad DOWN
 * from the top bar (Back / About) straight into the list. This is the Compose equivalent of
 * android:nextFocusDown: the list fills the screen behind the top bar, so geometric focus search
 * never sees it as "below" the bar buttons and DOWN used to do nothing.
 */
class TvListFocusTarget {
    internal var requester: FocusRequester? = null
    internal var listHasFocus = false

    /** Moves focus into the current list. False when there is no list or it already has focus. */
    fun enterList(): Boolean {
        if (listHasFocus) return false
        val target = requester ?: return false
        return runCatching { target.requestFocus() }.isSuccess
    }
}

val LocalTvListFocusTarget = staticCompositionLocalOf<TvListFocusTarget?> { null }

/**
 * Lleva el foco de mando (D-pad) al contenido de la lista al abrir la pantalla, igual que un diálogo.
 * focusRestorer recuerda el elemento enfocado y, al entrar desde la barra superior con DPAD_DOWN,
 * entrega el foco al primer elemento de la lista. Así las flechas arriba/abajo recorren todas las opciones.
 */
@Composable
fun Modifier.tvListFocus(): Modifier {
    val listFocus = remember { FocusRequester() }
    val target = LocalTvListFocusTarget.current
    DisposableEffect(target, listFocus) {
        target?.requester = listFocus
        target?.listHasFocus = false
        onDispose {
            if (target?.requester === listFocus) {
                target.requester = null
                target.listHasFocus = false
            }
        }
    }
    LaunchedEffect(Unit) {
        runCatching { listFocus.requestFocus() }
    }
    return this
        .onFocusChanged { state ->
            if (target?.requester === listFocus) target.listHasFocus = state.hasFocus
        }
        .focusRequester(listFocus)
        .focusRestorer()
}
