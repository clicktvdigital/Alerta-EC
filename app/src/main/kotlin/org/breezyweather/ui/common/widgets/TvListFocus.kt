package org.breezyweather.ui.common.widgets

import android.view.KeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Holds the focus requester of the option list on the screen currently shown. It stays registered
 * for as long as that screen is composed, so DOWN can re-enter the list any number of times.
 *
 * This is the Compose equivalent of android:nextFocusDown: the list sits behind the top bar, so
 * geometric focus search never sees it as "below" the bar buttons.
 */
class TvListFocusTarget {
    internal var requester: FocusRequester? = null

    /** True while the focus is on one of the list's items (kept by [tvListFocus]). */
    internal var inList: Boolean = false

    /** Moves focus into the current list. False when no list is registered. */
    fun enterList(): Boolean {
        val target = requester ?: return false
        return runCatching { target.requestFocus() }.isSuccess
    }
}

val LocalTvListFocusTarget = staticCompositionLocalOf<TvListFocusTarget?> { null }

/**
 * D-pad state of one activity whose screens are Compose. Route every key through [handleKey] from
 * dispatchKeyEvent and wrap the content in [TvScreenFocusHost].
 */
class TvScreenFocus {
    internal val list = TvListFocusTarget()
    internal var focusManager: FocusManager? = null

    /** True when the key was consumed. */
    fun handleKey(decor: View, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
            KeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
            KeyEvent.KEYCODE_DPAD_LEFT -> FocusDirection.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> FocusDirection.Right
            else -> return false
        }
        val manager = focusManager ?: return false
        val composeView = findComposeView(decor)

        if (composeView?.hasFocus() != true) {
            // Focus is on a native view (top bar Back / About). DOWN enters the list, like nextFocusDown.
            if (direction != FocusDirection.Down) return false
            if (list.enterList()) return true
            if (composeView == null || !composeView.requestFocus()) return false
            return manager.moveFocus(FocusDirection.Next)
        }

        if (manager.moveFocus(direction)) return true
        // Already on the last item of the list: stay there. Re-entering the list here made focusRestorer jump
        // back to a remembered item («Complementos…») at the end of Settings (TV test 2026-11-20).
        if (direction == FocusDirection.Down && list.inList) return true
        // A Compose top bar sits over the list, so geometric search cannot reach it: DOWN enters the list.
        return direction == FocusDirection.Down && list.enterList()
    }

    private fun findComposeView(root: View): AbstractComposeView? {
        if (root is AbstractComposeView) return root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findComposeView(root.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}

/** Provides [focus] to the Compose tree and keeps its FocusManager for [TvScreenFocus.handleKey]. */
@Composable
fun TvScreenFocusHost(focus: TvScreenFocus, content: @Composable () -> Unit) {
    val manager = LocalFocusManager.current
    SideEffect { focus.focusManager = manager }
    CompositionLocalProvider(LocalTvListFocusTarget provides focus.list, content = content)
}

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
        onDispose {
            target?.let { if (it.requester === listFocus) it.requester = null }
        }
    }
    LaunchedEffect(Unit) {
        runCatching { listFocus.requestFocus() }
    }
    return this
        .onFocusChanged { state -> target?.inList = state.hasFocus }
        .focusRequester(listFocus)
        .focusRestorer()
}

/**
 * Visible focus ring for a clickable card on TV: a 3 dp primary border while the D-pad focus is on it.
 * It only animates the border width at focus change, so it stays cheap on 1 GB devices.
 */
fun Modifier.tvFocusIndicator(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.isFocused }
        .border(
            width = if (focused) 3.dp else 0.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = shape,
        )
}
