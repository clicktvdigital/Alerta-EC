package org.breezyweather.ui.support

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import org.breezyweather.ui.map.MapActivity
import org.breezyweather.ui.common.widgets.NeonButton
import org.breezyweather.alertaec.community.AlertaEcCommunity
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

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
                NeonButton(onClick = { context.startActivity(Intent(context, MapActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()) { Text("Abrir mapa meteorológico") }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ Reseñas de ALERTA EC", style = MaterialTheme.typography.titleLarge)
                NeonButton(onClick = { showFeedback(context, false) },
                    modifier = Modifier.fillMaxWidth()) { Text("Escribir reseña") }
            }
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📢 Reporte meteorológico", style = MaterialTheme.typography.titleLarge)
                Text("Incluye texto, foto, video o audio y GPS voluntario.",
                    style = MaterialTheme.typography.bodyMedium)
                NeonButton(onClick = { showFeedback(context, true) },
                    modifier = Modifier.fillMaxWidth()) { Text("Redactar reporte y adjuntar evidencia") }
                NeonButton(onClick = { showCommunityReports(context) },
                    modifier = Modifier.fillMaxWidth()) { Text("👥 Ver reportes de la comunidad") }
                Text("Los reportes comunitarios son voluntarios y no equivalen a un aviso recibido por ECU 911.",
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

/** Recent community reports near the latest position (or all Ecuador), read on IO; errors shown plainly. */
private fun showCommunityReports(context: Context) {
    val host = findHost(context) ?: return
    if (!AlertaEcCommunity.isConfigured) {
        Toast.makeText(context, AlertaEcCommunity.NOT_CONFIGURED, Toast.LENGTH_LONG).show()
        return
    }
    val title = "👥 Reportes de la comunidad (últimas 12 h)"
    val loading = android.app.AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage("Consultando…")
        .setNegativeButton("Cerrar", null)
        .show()
    val position = org.breezyweather.background.location.ActiveLocationController.getLatest(context)
    host.lifecycleScope.launch {
        val result = AlertaEcCommunity.recentReports(position?.latitude, position?.longitude)
        if (!loading.isShowing) return@launch
        val reports = (result as? AlertaEcCommunity.Result.Ok)?.value
        if (reports.isNullOrEmpty()) {
            loading.setMessage(
                (result as? AlertaEcCommunity.Result.Error)?.message
                    ?: ("No hay reportes recientes " + if (position != null) "cerca de tu zona." else "en Ecuador.")
            )
            return@launch
        }
        loading.dismiss()
        val items = reports.map { report ->
            val hora = runCatching {
                java.time.OffsetDateTime.parse(report.createdAt)
                    .atZoneSameInstant(java.time.ZoneId.of("America/Guayaquil"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            }.getOrDefault("")
            "${severityMark(report.severity)} ${report.category} · ${report.severity} · $hora\n${report.description}" +
                if (report.latitude != null && report.longitude != null) {
                    "\n(zona ~1 km: %.2f, %.2f)".format(java.util.Locale.US, report.latitude, report.longitude)
                } else ""
        }.toTypedArray()
        android.app.AlertDialog.Builder(context)
            .setTitle("$title · toca uno para denunciarlo")
            .setItems(items) { _, index -> flagReport(context, host, reports[index]) }
            .setNegativeButton("Cerrar", null)
            .show()
    }
}

private fun severityMark(severity: String) = when (severity) {
    "Crítica" -> "🔴"
    "Alta" -> "🟠"
    "Moderada" -> "🟡"
    else -> "🟢"
}

/** Flag a community report: three flags hide it until it is reviewed (database trigger). */
private fun flagReport(context: Context, host: FragmentActivity, report: AlertaEcCommunity.Report) {
    val reasons = AlertaEcCommunity.FLAG_REASONS.toTypedArray()
    android.app.AlertDialog.Builder(context)
        .setTitle("Denunciar este reporte")
        .setItems(reasons) { _, index ->
            host.lifecycleScope.launch {
                val message = when (val result = AlertaEcCommunity.flagReport(report.id, reasons[index])) {
                    is AlertaEcCommunity.Result.Ok -> "Gracias. Con varias denuncias el reporte se oculta y se revisa."
                    is AlertaEcCommunity.Result.Error -> result.message
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
        .setNegativeButton("Cancelar", null)
        .show()
}
