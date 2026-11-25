package org.breezyweather.ui.common.widgets

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.unit.dp
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

private class TvListFocus {
    var inside = false
    var self = false
}
