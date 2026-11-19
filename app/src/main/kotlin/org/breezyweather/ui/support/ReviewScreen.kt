package org.breezyweather.ui.support

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.breezyweather.ui.common.widgets.Material3Scaffold
import org.breezyweather.ui.common.widgets.insets.FitStatusBarTopAppBar

@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var estrellas by rememberSaveable { mutableIntStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }

    Material3Scaffold(
        topBar = {
            FitStatusBarTopAppBar(
                title = "Calificar ALERTA EC",
                onBackPressed = onBack,
            )
        }
    ) { paddings ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddings)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Tu opinion nos ayuda a mejorar ALERTA EC", style = MaterialTheme.typography.titleLarge)
            Text("Selecciona de 1 a 5 estrellas. No necesitas realizar ninguna donacion.")
            androidx.compose.foundation.layout.Row {
                (1..5).forEach { numero ->
                    TextButton(onClick = { estrellas = numero }) {
                        Text(if (numero <= estrellas) "★" else "☆")
                    }
                }
            }
            Text("Valoracion: $estrellas de 5 estrellas")
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it.take(2000) },
                label = { Text("Escribe tu opinion (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 7,
            )
            Text("Tu valoracion no se publica automaticamente. Al continuar, se abrira tu aplicacion de correo para que decidas si deseas enviarla.")
            Button(
                enabled = estrellas > 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:clicktvdigital@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Reseña de ALERTA EC")
                        putExtra(Intent.EXTRA_TEXT, "Valoracion: $estrellas/5" + System.lineSeparator() + System.lineSeparator() + "Comentario: $comentario")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No hay una aplicacion de correo disponible.", Toast.LENGTH_LONG).show()
                    }
                },
            ) {
                Text("Preparar reseña por correo")
            }
        }
    }
}
