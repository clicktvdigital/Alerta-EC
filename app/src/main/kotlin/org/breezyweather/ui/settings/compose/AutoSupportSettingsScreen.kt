package org.breezyweather.ui.settings.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import org.breezyweather.alertaec.support.AutoSupportCheck
import org.breezyweather.alertaec.support.AutoSupportEngine
import org.breezyweather.alertaec.support.AutoSupportGuidance
import org.breezyweather.alertaec.support.AutoSupportFaq
import org.breezyweather.alertaec.support.AutoSupportFeedback
import org.breezyweather.alertaec.support.AutoSupportReport
import org.breezyweather.alertaec.support.AutoSupportPhase
import org.breezyweather.alertaec.support.AutoSupportResult
import org.breezyweather.ui.common.widgets.Material3Scaffold
import org.breezyweather.ui.common.widgets.insets.FitStatusBarTopAppBar

@Composable
fun AutoSupportSettingsScreen(
    onNavigateBack: () -> Unit,
    locationRepository: breezyweather.data.location.LocationRepository,
    weatherRepository: breezyweather.data.weather.WeatherRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<AutoSupportEngine.Progress?>(null) }
    var running by remember { mutableStateOf(false) }
    var showFaq by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackDescription by remember { mutableStateOf("") }
    var includeDiagnostics by remember { mutableStateOf(false) }
    var showFeedbackPreview by remember { mutableStateOf(false) }

    Material3Scaffold(
        topBar = {
            FitStatusBarTopAppBar(
                title = "Centro de ayuda y autosoporte",
                onBackPressed = onNavigateBack,
            )
        }
    ) { paddings: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Diagnostico automatico de ALERTA EC",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Comprueba conexion, ubicacion, notificaciones y datos meteorologicos. Cuando es seguro, intenta actualizar el pronostico y verifica si llegaron datos nuevos.",
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !running,
                onClick = {
                    running = true
                    progress = null
                    scope.launch {
                        try {
                            AutoSupportEngine.run(
                                context = context,
                                locationRepository = locationRepository,
                                weatherRepository = weatherRepository,
                            ).collect { progress = it }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            progress = AutoSupportEngine.Progress(
                                phase = AutoSupportPhase.FINALIZADO,
                                report = AutoSupportReport(
                                    checks = listOf(
                                        AutoSupportCheck(
                                            id = "unexpected_support_error",
                                            description = "Ejecucion del autosoporte",
                                            result = AutoSupportResult.ERROR,
                                            detail = "El proceso se interrumpio por un error inesperado. No se confirma ninguna reparacion. Puede volver a intentarlo. Tipo: ${e.javaClass.simpleName}",
                                        )
                                    )
                                ),
                                message = "El autosoporte no pudo completarse.",
                            )
                        } finally {
                            running = false
                        }
                    }
                },
            ) {
                Text(if (running) "Ejecutando autosoporte..." else "Iniciar autosoporte")
            }
            if (running) {
                CircularProgressIndicator()
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showFaq = !showFaq },
            ) {
                Text(if (showFaq) "Ocultar preguntas frecuentes" else "Ver preguntas frecuentes")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showFeedback = !showFeedback },
            ) {
                Text(if (showFeedback) "Ocultar formulario" else "Otro problema o sugerencia")
            }
            if (showFeedback) {
                Text(
                    "Su mensaje permanece en este dispositivo hasta que decida compartirlo.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = feedbackDescription,
                    onValueChange = {
                        feedbackDescription = it.take(4000)
                        showFeedbackPreview = false
                    },
                    label = { Text("Describa el problema o sugerencia") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeDiagnostics,
                        onCheckedChange = {
                            includeDiagnostics = it
                            showFeedbackPreview = false
                        },
                    )
                    Text("Incluir resumen tecnico del diagnostico (opcional)")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = feedbackDescription.isNotBlank(),
                    onClick = { showFeedbackPreview = true },
                ) {
                    Text("Revisar informe antes de compartir")
                }
                if (showFeedbackPreview) {
                    Text("Vista previa del informe", style = MaterialTheme.typography.titleMedium)
                    Text(
                        AutoSupportFeedback.compose(
                            description = feedbackDescription,
                            report = progress?.report,
                            includeDiagnostics = includeDiagnostics,
                        )
                    )
                    Text("Esta vista previa no envia informacion.")
                }
            }
            if (showFaq) {
                Text(
                    "Ayuda disponible sin Internet",
                    style = MaterialTheme.typography.titleMedium,
                )
                AutoSupportFaq.entries.forEach { entry ->
                    Text(entry.question, style = MaterialTheme.typography.titleSmall)
                    Text(entry.answer, style = MaterialTheme.typography.bodyMedium)
                }
            }
            progress?.let { state ->
                val status = when (state.phase) {
                    AutoSupportPhase.INACTIVO -> "Sin iniciar"
                    AutoSupportPhase.DIAGNOSTICANDO -> "Diagnosticando problemas"
                    AutoSupportPhase.REPARANDO -> "Intentando reparar problemas"
                    AutoSupportPhase.FINALIZADO ->
                        if (state.report?.completedWithoutPendingIssues == true) {
                            "Finalizado correctamente: sin problemas pendientes"
                        } else {
                            "Finalizado con problemas pendientes"
                        }
                }
                val statusColor = when (state.phase) {
                    AutoSupportPhase.INACTIVO -> MaterialTheme.colorScheme.onSurfaceVariant
                    AutoSupportPhase.DIAGNOSTICANDO -> Color(0xFFC62828)
                    AutoSupportPhase.REPARANDO -> Color(0xFFB77900)
                    AutoSupportPhase.FINALIZADO ->
                        if (state.report?.completedWithoutPendingIssues == true) {
                            Color(0xFF2E7D32)
                        } else {
                            Color(0xFFB77900)
                        }
                }
                Text(
                    text = status,
                    color = statusColor,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (
                    state.phase == AutoSupportPhase.FINALIZADO &&
                    state.report?.completedWithoutPendingIssues == false
                ) {
                    Text(
                        "El proceso termino, pero quedan problemas pendientes.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(state.message)
                state.report?.let { report ->
                    Text("Comprobaciones: ${report.checks.size}. Pendientes: ${report.pendingCount}. Reparadas: ${report.repairedCount}.")
                    report.checks.forEach { check ->
                        val result = when (check.result) {
                            AutoSupportResult.CORRECTO -> "Correcto"
                            AutoSupportResult.REPARADO -> "Reparado"
                            AutoSupportResult.PENDIENTE -> "Pendiente"
                            AutoSupportResult.ERROR -> "Error"
                        }
                        Text("${check.description}: $result", style = MaterialTheme.typography.titleSmall)
                        Text(check.detail)
                        AutoSupportGuidance.recommendation(check)?.let { recommendation ->
                            Text(
                                text = "Qué puedes hacer: $recommendation",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
