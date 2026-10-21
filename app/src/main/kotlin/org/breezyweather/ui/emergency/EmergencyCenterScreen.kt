package org.breezyweather.ui.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.breezyweather.alertaec.info.OfficialInfoCachedItem
import org.breezyweather.alertaec.info.OfficialInfoFreshness
import org.breezyweather.alertaec.info.OfficialInfoSelectionReadState
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EmergencyCenterScreen(
    onBack: () -> Unit,
    emergencyCenterViewModel: EmergencyCenterViewModel = viewModel(),
) {
    val uiState by emergencyCenterViewModel.uiState.collectAsState()
    var hasCompletedInitialRead by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) hasCompletedInitialRead = true
    }
    val context = LocalContext.current
    val sources = listOf(
        "INAMHI - Alertas meteorologicas" to
            "https://servicios.inamhi.gob.ec/alertas/",
        "ECU 911 - Informacion oficial" to
            "https://www.ecu911.gob.ec/",
        "Gestion de Riesgos del Ecuador" to
            "https://www.gestionderiesgos.gob.ec/",
        "Instituto Geofisico - Sismos y volcanes" to
            "https://www.igepn.edu.ec/",
        "Municipio de Quito" to
            "https://www.quito.gob.ec/"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Centro de Emergencias", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Avisos oficiales disponibles en la cache local. " +
                "Consulta la fuente y verifica las fechas de cada aviso."
        )

        if (uiState.isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(
                    if (!hasCompletedInitialRead) {
                        "Leyendo cache local..."
                    } else {
                        "Recargando cache local..."
                    }
                )
            }
        }

        when (uiState.readState) {
            OfficialInfoSelectionReadState.EMPTY -> {
                if (!uiState.isLoading) {
                    Text("No hay avisos disponibles en la cache local.")
                }
            }

            OfficialInfoSelectionReadState.READ_ERROR -> {
                if (!uiState.isLoading) {
                    Text("No se pudo leer la cache local de avisos.")
                }
            }

            OfficialInfoSelectionReadState.LOADED -> {
                if (uiState.current.isNotEmpty()) {
                    Text("Avisos vigentes", style = MaterialTheme.typography.titleLarge)
                    uiState.current.forEach { cachedItem ->
                        EmergencyInfoCard(cachedItem, context)
                    }
                }
                if (uiState.history.isNotEmpty()) {
                    Text("Historial", style = MaterialTheme.typography.titleLarge)
                    uiState.history.forEach { cachedItem ->
                        EmergencyInfoCard(cachedItem, context)
                    }
                }
            }
        }

        sources.forEach { (title, url) ->
            OutlinedButton(
                onClick = {
                    if (url.isValidHttpUrl()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title)
            }
        }

        OutlinedButton(
            onClick = emergencyCenterViewModel::reload,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text("Recargar cache local")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al pronostico")
        }
    }
}

@Composable
private fun EmergencyInfoCard(item: OfficialInfoCachedItem, context: android.content.Context) {
    val sourceUrl = item.item.sourceUrl?.takeIf { it.isValidHttpUrl() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.item.title, style = MaterialTheme.typography.titleMedium)
            if (item.item.summary.isNotBlank()) {
                Text(item.item.summary)
            }
            Text("Fuente: ${item.item.sourceId}")
            Text("Fechas: ${item.availableDates()}")
            Text("Vigencia: ${item.freshness.label()}")
            Text("Verificacion: ${item.item.verification.label()}")
            sourceUrl?.let { url ->
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                ) {
                    Text("Abrir fuente")
                }
            }
        }
    }
}

private val emergencyDateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-EC"))
    .withZone(ZoneId.of("America/Guayaquil"))

private fun OfficialInfoCachedItem.availableDates(): String = listOfNotNull(
    item.publishedAtMillis?.let { "publicado ${it.formatEmergencyDate()}" },
    item.updatedAtMillis?.let { "actualizado ${it.formatEmergencyDate()}" },
    "recibido ${item.fetchedAtMillis.formatEmergencyDate()}",
    item.expiresAtMillis?.let { "vence ${it.formatEmergencyDate()}" }
).joinToString("; ")

private fun Long.formatEmergencyDate(): String = emergencyDateFormatter.format(Instant.ofEpochMilli(this))

private fun OfficialInfoFreshness.label(): String = when (this) {
    OfficialInfoFreshness.ACTIVE -> "Vigente"
    OfficialInfoFreshness.EXPIRED -> "Caducado"
    OfficialInfoFreshness.UNKNOWN -> "No determinada"
    OfficialInfoFreshness.ARCHIVED -> "Archivado"
}

private fun Enum<*>.label(): String = when (name) {
    "OFFICIAL" -> "Oficial"
    "SOURCE_VERIFIED" -> "Fuente verificada"
    "UNVERIFIED" -> "No verificada"
    else -> name
}

private fun String.isValidHttpUrl(): Boolean = runCatching {
    val uri = URI(this)
    (uri.scheme.equals("http", ignoreCase = true) ||
        uri.scheme.equals("https", ignoreCase = true)) &&
        !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null
}.getOrDefault(false)
