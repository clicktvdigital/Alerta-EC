package org.breezyweather.ui.common.widgets

import android.content.pm.PackageManager
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
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

/**
 * Android TV remote on a scrolling list whose content is mostly non-focusable (charts, text cards):
 * Down/Up first move focus to the next focusable item, and only when there is none they scroll the list by a
 * step, so every part of the screen can be reached and read without a touch screen. The list itself can hold
 * focus, so it is reachable even when it has no focusable item. No effect outside Android TV.
 */
fun Modifier.tvDpadScroll(state: ScrollableState): Modifier = composed {
    val context = LocalContext.current
    val television = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    if (!television) return@composed this
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 320.dp.toPx() }
    this
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val sign = when (event.key) {
                Key.DirectionDown -> 1
                Key.DirectionUp -> -1
                else -> return@onKeyEvent false
            }
            if (focusManager.moveFocus(if (sign > 0) FocusDirection.Down else FocusDirection.Up)) {
                return@onKeyEvent true
            }
            val canScroll = if (sign > 0) state.canScrollForward else state.canScrollBackward
            if (!canScroll) return@onKeyEvent false
            scope.launch { state.animateScrollBy(sign * step) }
            true
        }
        .focusable()
}
