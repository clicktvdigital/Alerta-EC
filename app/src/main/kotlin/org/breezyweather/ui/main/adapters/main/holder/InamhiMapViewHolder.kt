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
import org.breezyweather.ui.support.TvCardActions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import breezyweather.domain.location.model.Location
import java.io.File
import org.breezyweather.ui.common.widgets.tvFocusIndicator
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

/**
 * «Meteorología de Ecuador», «Reseñas» and «Reporte» as three separate views: on TV each one is its own
 * Home target and OK runs its button directly. As one ComposeView they were selected together and OK
 * opened an extra list of actions (TV test 2026-11-20).
 */
class InamhiMapViewHolder(
    private val container: android.widget.LinearLayout,
) : AbstractMainViewHolder(container) {
    private val cards = listOf(TvCardActions.TAG_MAP, TvCardActions.TAG_REVIEW, TvCardActions.TAG_REPORT)
        .map { tag ->
            ComposeView(container.context).also { view ->
                view.tag = tag
                container.addView(
                    view,
                    android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                )
            }
        }

    init {
        container.orientation = android.widget.LinearLayout.VERTICAL
        val padding = (6 * container.resources.displayMetrics.density).toInt()
        container.setPadding(padding * 2, padding, padding * 2, padding)
    }

    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean
    ) {
        super.onBindView(context, location, provider, listAnimationEnabled, itemAnimationEnabled)
        val dark = !ThemeManager.isLightTheme(context, location)
        cards.forEach { view ->
            view.setContent {
                BreezyWeatherTheme(dark) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.padding(vertical = 6.dp)
                    ) {
                        when (view.tag) {
                            TvCardActions.TAG_MAP -> org.breezyweather.ui.support.AlertaEcMapEntryCard(context)
                            TvCardActions.TAG_REVIEW -> org.breezyweather.ui.support.AlertaEcReviewEntryCard(context)
                            else -> org.breezyweather.ui.support.AlertaEcReportEntryCard(context)
                        }
                    }
                }
            }
        }
    }
}
