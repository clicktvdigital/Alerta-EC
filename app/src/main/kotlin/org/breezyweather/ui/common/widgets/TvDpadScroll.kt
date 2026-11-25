package org.breezyweather.ui.common.widgets

import android.content.Context
import android.content.pm.PackageManager
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import kotlinx.coroutines.launch

/** Neon cyan of the Alerta EC focus ring (same tone as [NeonButton]). */
val TvNeonCyan = Color(0xFF00E5FF)

/**
 * True on Android TV / Google TV and on TV boxes: leanback or television feature, or no touch screen at all
 * (cheap boxes often declare neither feature, but a phone or tablet always has a touch screen).
 */
fun Context.isTelevisionUi(): Boolean {
    val pm = packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
        !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
}

@Composable
fun rememberIsTelevisionUi(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isTelevisionUi() }
}

/**
 * Android TV remote on a scrolling list or column (settings, about, details, alerts):
 *
 * - Up/Down first move focus to the next focusable item; the list scrolls to show it (bring into view).
 * - When there is no focusable item further in that direction but the content can still scroll (text,
 *   charts, credits), the list itself takes the focus and scrolls by a step, so every part can be read.
 *   Taking the focus first matters: otherwise the focused item scrolls off screen, is disposed, the focus
 *   falls out of the screen and the remote seems stuck.
 * - If moving up or down would make the focus leave the list (for example to the top bar) while the list can
 *   still scroll that way, the focus stays in the list and it scrolls instead, so the top of the screen is
 *   shown before reaching the back button.
 * - Left/Right and OK keep their default behaviour (row controls, clickable items).
 *
 * No effect on phones and tablets.
 */
fun Modifier.tvDpadScroll(state: ScrollableState): Modifier = composed {
    if (!rememberIsTelevisionUi()) return@composed this
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 280.dp.toPx() }
    val self = remember { FocusRequester() }
    // Plain fields: onFocusChanged runs synchronously inside moveFocus, so they are current right after it.
    val focus = remember { TvListFocus() }
    val focusManager = LocalFocusManager.current

    this
        .focusRequester(self)
        .onFocusChanged {
            focus.inside = it.hasFocus
            focus.self = it.isFocused
        }
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val forward = when (event.key) {
                Key.DirectionDown -> true
                Key.DirectionUp -> false
                else -> return@onKeyEvent false
            }
            val canScroll = if (forward) state.canScrollForward else state.canScrollBackward
            if (focusManager.moveFocus(if (forward) FocusDirection.Down else FocusDirection.Up)) {
                if (focus.inside || !canScroll) return@onKeyEvent true
                // The focus left the list although it can still scroll: bring it back and scroll instead.
                runCatching { self.requestFocus() }
            } else if (!canScroll) {
                return@onKeyEvent false
            } else if (!focus.self) {
                runCatching { self.requestFocus() }
            }
            scope.launch { state.animateScrollBy(if (forward) step else -step) }
            true
        }
        .focusable()
}

/**
 * Android TV remote on a list of focusable rows (settings, about): unlike [tvDpadScroll], the list itself is
 * never a focus target, only a focus group, so coming from the top bar the focus enters the nearest row instead
 * of stopping on an invisible list (which left Up/Down without effect in 0.1.26).
 *
 * - Up/Down move the focus row by row; the focused row is brought into view (smooth scroll), so the focus never
 *   stays off screen.
 * - When there is no row further in that direction but the list can still scroll (titles, credits), it scrolls
 *   by a step so every part can be read; while the list can still scroll that way the focus does not leave it
 *   (the top of the screen is shown before reaching the back button).
 * - Left/Right and OK keep their default behaviour (top bar buttons, switches, clickable rows).
 *
 * No effect on phones and tablets.
 */
fun Modifier.tvDpadList(state: ScrollableState): Modifier = composed {
    if (!rememberIsTelevisionUi()) return@composed this
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 240.dp.toPx() }
    val focusManager = LocalFocusManager.current
    val group = remember { FocusRequester() }
    val focus = remember { TvListFocus() }
    val view = LocalView.current

    // On the TV, Compose's two-dimensional search does not cross from the top bar (back, about) to the rows below,
    // so DOWN from the top bar used to do nothing. That key then reaches the view system unhandled: catch it there
    // and enter the list (its first visible row).
    DisposableEffect(view) {
        val listener = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            if (event.action != AndroidKeyEvent.ACTION_DOWN ||
                event.keyCode != AndroidKeyEvent.KEYCODE_DPAD_DOWN ||
                focus.inside
            ) {
                return@OnUnhandledKeyEventListenerCompat false
            }
            runCatching { group.requestFocus() }.isSuccess && focus.inside
        }
        ViewCompat.addOnUnhandledKeyEventListener(view, listener)
        onDispose { ViewCompat.removeOnUnhandledKeyEventListener(view, listener) }
    }

    this
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val forward = when (event.key) {
                Key.DirectionDown -> true
                Key.DirectionUp -> false
                else -> return@onKeyEvent false
            }
            // Geometric search first, then the order of the rows (works even where the geometric one does not).
            if (focusManager.moveFocus(if (forward) FocusDirection.Down else FocusDirection.Up) ||
                focusManager.moveFocus(if (forward) FocusDirection.Next else FocusDirection.Previous)
            ) {
                return@onKeyEvent true
            }
            if (!(if (forward) state.canScrollForward else state.canScrollBackward)) return@onKeyEvent false
            scope.launch { state.animateScrollBy(if (forward) step else -step) }
            true
        }
        .focusRequester(group)
        .onFocusChanged { focus.inside = it.hasFocus }
        .focusProperties {
            onExit = {
                // Never leave downwards (nothing below a full screen list); leave upwards (top bar) only once the
                // top of the list is shown.
                val cancel = when (requestedFocusDirection) {
                    FocusDirection.Down, FocusDirection.Next -> true
                    FocusDirection.Up, FocusDirection.Previous -> state.canScrollBackward
                    else -> false
                }
                if (cancel) cancelFocusChange()
            }
        }
        .focusGroup()
}

private class TvListFocus {
    var inside = false
    var self = false
}
