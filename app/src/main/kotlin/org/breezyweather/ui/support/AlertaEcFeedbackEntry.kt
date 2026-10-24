package org.breezyweather.ui.support

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import org.breezyweather.ui.map.MapActivity

/** Entradas de Home sin campos editables dentro del RecyclerView. */
@Composable
fun AlertaEcFeedbackEntry(context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🗺️ Meteorología de Ecuador", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Lluvia, temperatura y capas INAMHI", style = MaterialTheme.typography.bodyMedium)
                Text("Las capas se consultan por separado; activadas no significa que haya datos recientes.",
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = { context.startActivity(Intent(context, MapActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()) { Text("Abrir mapa meteorológico") }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ Reseñas de ALERTA EC", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { showFeedback(context, false) },
                    modifier = Modifier.fillMaxWidth()) { Text("Escribir reseña") }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📢 Reporte meteorológico", style = MaterialTheme.typography.titleLarge)
                Text("Incluye texto, foto, video o audio y GPS voluntario.",
                    style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { showFeedback(context, true) },
                    modifier = Modifier.fillMaxWidth()) { Text("Redactar reporte y adjuntar evidencia") }
                Text("El envío requiere elegir una aplicación externa. No equivale a un reporte recibido por ECU 911.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun findHost(context: Context): FragmentActivity? {
    var current: Context = context
    repeat(10) {
        val candidate = current
        if (candidate is FragmentActivity) return candidate
        current = (candidate as? ContextWrapper)?.baseContext ?: return null
    }
    return null
}

private fun showFeedback(context: Context, isReport: Boolean) {
    val host = findHost(context)
    if (host == null || host.supportFragmentManager.isStateSaved) {
        Toast.makeText(context, "No se puede abrir el formulario ahora.", Toast.LENGTH_LONG).show()
        return
    }
    val tag = if (isReport) "ALERTA_EC_FORM_REPORTE" else "ALERTA_EC_FORM_RESENA"
    if (host.supportFragmentManager.findFragmentByTag(tag) == null) {
        AlertaEcFeedbackDialog.create(isReport).show(host.supportFragmentManager, tag)
    }
}
