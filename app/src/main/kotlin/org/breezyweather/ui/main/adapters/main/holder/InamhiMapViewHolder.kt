package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import breezyweather.domain.location.model.Location
import java.io.File
import org.breezyweather.ui.map.MapActivity
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.resource.providers.ResourceProvider

private fun createReportMediaUri(context: Context, extension: String): Uri? = runCatching {
    // El FileProvider existente solo autoriza external-cache-path.
    val folder = context.externalCacheDir ?: error("Almacenamiento temporal no disponible")
    val file = File.createTempFile("alerta_ec_report_", ".$extension", folder)
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()

private fun startReviewEmail(context: Context, stars: Int, comment: String) {
    val body = "Valoración: $stars/5\nComentario: ${comment.trim()}"
    val uri = Uri.parse(
        "mailto:clicktvdigital@gmail.com?subject=" + Uri.encode("Reseña de ALERTA EC") +
            "&body=" + Uri.encode(body)
    )
    try {
        context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
    } catch (_: Exception) {
        Toast.makeText(context, "No hay una aplicación de correo disponible", Toast.LENGTH_LONG).show()
    }
}

private fun shareReport(context: Context, kind: String, message: String, gps: Boolean, attachment: Uri?) {
    val locationLine = if (gps) {
        val position = org.breezyweather.background.location.ActiveLocationController.getLatest(context)
        if (position != null && System.currentTimeMillis() - position.timestamp in 0L..120000L &&
            position.latitude.isFinite() && position.longitude.isFinite()) {
            "GPS reciente: ${position.latitude}, ${position.longitude}; " +
                "precisión horizontal: ${position.accuracy ?: "no informada"} m"
        } else {
            "GPS: sin medición reciente verificable"
        }
    } else {
        "GPS: no compartido por elección del usuario"
    }
    val body = "ALERTA EC - Reporte voluntario\nCategoría: $kind\n" +
        "Descripción: ${message.trim()}\n$locationLine\n" +
        "Fecha del dispositivo: ${java.util.Date()}\n" +
        "Este reporte no se envía automáticamente; el usuario elige cómo compartirlo."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (attachment == null) "text/plain"
            else context.contentResolver.getType(attachment) ?: "application/octet-stream"
        putExtra(Intent.EXTRA_SUBJECT, "Reporte ALERTA EC - $kind")
        putExtra(Intent.EXTRA_TEXT, body)
        if (attachment != null) {
            putExtra(Intent.EXTRA_STREAM, attachment)
            clipData = android.content.ClipData.newRawUri("Evidencia ALERTA EC", attachment)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Elegir aplicación para enviar el reporte"))
    } catch (_: Exception) {
        Toast.makeText(context, "No se encontró aplicación para compartir", Toast.LENGTH_LONG).show()
    }
}

class InamhiMapViewHolder(private val composeView: ComposeView) : AbstractMainViewHolder(composeView) {
    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean
    ) {
        super.onBindView(context, location, provider, listAnimationEnabled, itemAnimationEnabled)
        composeView.setContent {
            BreezyWeatherTheme(!ThemeManager.isLightTheme(context, location)) {
                org.breezyweather.ui.support.AlertaEcFeedbackEntry(context)
            }
        }
    }
}

@Composable
private fun HomeMapAndFeedback(context: Context) {
    val teclado = LocalSoftwareKeyboardController.current
    val focoOpinion = remember { FocusRequester() }
    val focoReporte = remember { FocusRequester() }
    var reviewExpanded by rememberSaveable { mutableStateOf(false) }
    var reportExpanded by rememberSaveable { mutableStateOf(false) }
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var opinion by rememberSaveable { mutableStateOf("") }
    var reportText by rememberSaveable { mutableStateOf("") }
    var reportKind by rememberSaveable { mutableStateOf("Fenómeno meteorológico") }
    var includeGps by rememberSaveable { mutableStateOf(false) }
    var attachment by remember { mutableStateOf<Uri?>(null) }
    var attachmentLabel by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(reviewExpanded) {
        if (reviewExpanded) {
            delay(200)
            runCatching { focoOpinion.requestFocus() }
            teclado?.show()
        }
    }
    LaunchedEffect(reportExpanded) {
        if (reportExpanded) {
            delay(200)
            runCatching { focoReporte.requestFocus() }
            teclado?.show()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { attachment = uri; attachmentLabel = "Fotografía seleccionada" }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { attachment = uri; attachmentLabel = "Video seleccionado" }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { attachment = uri; attachmentLabel = "Audio seleccionado" }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { successful ->
        if (successful && pendingCameraUri != null) {
            attachment = pendingCameraUri; attachmentLabel = "Foto tomada con cámara"
        }
        pendingCameraUri = null
    }
    val takeVideo = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { successful ->
        if (successful && pendingVideoUri != null) {
            attachment = pendingVideoUri; attachmentLabel = "Video grabado con cámara"
        }
        pendingVideoUri = null
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🗺️ Meteorología de Ecuador", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Lluvias · Temperatura · Capas INAMHI", style = MaterialTheme.typography.bodyLarge)
                Text("Mapas y capas de pronóstico. La activación de una capa no asegura que existan " +
                    "datos recientes para todos los puntos del país.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { context.startActivity(Intent(context, MapActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Explorar mapa meteorológico") }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⭐ Reseñas y opiniones", style = MaterialTheme.typography.titleLarge)
                Text("Cuéntanos qué mejorar, sin salir de Inicio.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { reviewExpanded = !reviewExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (reviewExpanded) "Ocultar reseña" else "Escribir reseña")
                }
                if (reviewExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        (1..5).forEach { index ->
                            TextButton(onClick = { rating = index }, modifier = Modifier.weight(1f)) {
                                Text(if (index <= rating) "★" else "☆",
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text("Valoración: $rating de 5 estrellas", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = opinion, onValueChange = { opinion = it.take(2000) },
                        label = { Text("Tu opinión (opcional)") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focoOpinion),
                        minLines = 2, maxLines = 5
                    )
                    Button(onClick = { startReviewEmail(context, rating, opinion) },
                        enabled = rating > 0, modifier = Modifier.fillMaxWidth()) {
                        Text("Preparar reseña por correo")
                    }
                    Text("Se abrirá el correo. No se publica ni se envía hasta que lo confirmes.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("📣 Reportar clima o fallos", style = MaterialTheme.typography.titleLarge)
                Text("Comunica condiciones del tiempo o fallos de la aplicación. " +
                    "No reemplaza una llamada al ECU 911.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { reportExpanded = !reportExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (reportExpanded) "Ocultar reporte" else "Preparar reporte")
                }
                if (reportExpanded) {
                    Text("¿Qué quieres reportar?", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { reportKind = "Fenómeno meteorológico" }, modifier = Modifier.weight(1f)
                        ) { Text(if (reportKind == "Fenómeno meteorológico") "✓ Clima" else "Clima") }
                        OutlinedButton(
                            onClick = { reportKind = "Error o sugerencia de la aplicación" },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (reportKind != "Fenómeno meteorológico") "✓ Aplicación" else "Aplicación") }
                    }
                    OutlinedTextField(
                        value = reportText, onValueChange = { reportText = it.take(3000) },
                        label = { Text("¿Qué ocurrió? ¿Dónde y cuándo?") },
                        modifier = Modifier.fillMaxWidth().focusRequester(focoReporte),
                        minLines = 3, maxLines = 6
                    )
                    Text("Adjuntar evidencia (opcional, un archivo por reporte)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { photoPicker.launch("image/*") },
                            modifier = Modifier.weight(1f)) { Text("🖼️ Foto") }
                        OutlinedButton(onClick = { videoPicker.launch("video/*") },
                            modifier = Modifier.weight(1f)) { Text("🎞️ Video") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { audioPicker.launch("audio/*") },
                            modifier = Modifier.weight(1f)) { Text("🎵 Audio") }
                        OutlinedButton(onClick = {
                            val uri = createReportMediaUri(context, "jpg")
                            if (uri == null) {
                                Toast.makeText(context, "La cámara no está disponible", Toast.LENGTH_SHORT).show()
                            } else { pendingCameraUri = uri; takePhoto.launch(uri) }
                        }, modifier = Modifier.weight(1f)) { Text("📷 Cámara") }
                    }
                    OutlinedButton(onClick = {
                        val uri = createReportMediaUri(context, "mp4")
                        if (uri == null) {
                            Toast.makeText(context, "La videocámara no está disponible", Toast.LENGTH_SHORT).show()
                        } else { pendingVideoUri = uri; takeVideo.launch(uri) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("🎥 Grabar video") }
                    if (attachment != null) {
                        Text("Adjunto: $attachmentLabel", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { attachment = null; attachmentLabel = "" }) { Text("Quitar archivo") }
                    }
                    Row {
                        Checkbox(checked = includeGps, onCheckedChange = { includeGps = it })
                        Text("Compartir mi ubicación GPS reciente, solo con mi autorización",
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = {
                        shareReport(context, reportKind, reportText, includeGps, attachment)
                    }, enabled = reportText.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Elegir aplicación para enviar")
                    }
                    Text("ALERTA EC prepara el contenido localmente. No hay recepción automática " +
                        "ni seguimiento remoto habilitado para este formulario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}