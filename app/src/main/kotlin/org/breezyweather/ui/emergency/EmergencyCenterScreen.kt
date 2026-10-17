package org.breezyweather.ui.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EmergencyCenterScreen(onBack: () -> Unit) {
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
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Centro de Emergencias", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Informacion oficial del Ecuador. " +
                "Consulta las fuentes originales y verifica la fecha de cada aviso."
        )
        Text(
            "Los enlaces no representan alertas activas. " +
                "Noticias, mapas y camaras en vivo se integraran despues.",
            style = MaterialTheme.typography.bodySmall
        )

        sources.forEach { (title, url) ->
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title)
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al pronostico")
        }
    }
}
