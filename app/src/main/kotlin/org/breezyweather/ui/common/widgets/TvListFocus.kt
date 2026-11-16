package org.breezyweather.ui.common.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer

/**
 * Lleva el foco de mando (D-pad) al contenido de la lista al abrir la pantalla, igual que un diálogo.
 * focusRestorer recuerda el elemento enfocado y, al entrar desde la barra superior con DPAD_DOWN,
 * entrega el foco al primer elemento de la lista. Así las flechas arriba/abajo recorren todas las opciones.
 */
@Composable
fun Modifier.tvListFocus(): Modifier {
    val listFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { listFocus.requestFocus() }
    }
    return this.focusRequester(listFocus).focusRestorer()
}
