package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import breezyweather.domain.location.model.Location
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import org.breezyweather.alertaec.observation.InamhiPointForecast
import org.breezyweather.alertaec.observation.QuitoRainGauges
import org.breezyweather.ui.main.adapters.main.MainAdapter
import org.breezyweather.ui.map.MapActivity
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import org.breezyweather.ui.common.widgets.NeonButton
import org.breezyweather.alertaec.community.AlertaEcCommunity
import org.breezyweather.alertaec.AlertaEcRainRisk
import androidx.compose.runtime.setValue

/**
 * Light "rain right now" card at the top of the home (nowcasting): the next hour from the forecast
 * (MET Norway probability, Open-Meteo minute-by-minute), then EPMAPS measured rain and INAMHI WRF when they
 * answer. Opens the weather map and jumps to the support block; both buttons are D-pad focusable on Android TV.
 * Network reads run on IO inside the observation helpers, never on the main thread.
 */
class RainNowViewHolder(
    private val composeView: ComposeView,
) : AbstractMainViewHolder(composeView) {

    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
    ) {
        super.onBindView(context, location, provider, listAnimationEnabled, itemAnimationEnabled)
        val forecastLine = forecastLine(location)
        composeView.setContent {
            BreezyWeatherTheme(!ThemeManager.isLightTheme(context, location)) {
                RainNowCard(context, location, forecastLine)
            }
        }
    }

    private fun forecastLine(location: Location): String {
        val weather = location.weather ?: return "Sin datos de pronóstico todavía"
        val now = Date()
        val inHour = Date(now.time + 60 * 60 * 1000L)
        val minutes = weather.minutelyForecast.filter { it.date >= now && it.date < inHour }
        val firstWet = minutes.firstOrNull { (it.precipitationIntensity?.inMillimeters ?: 0.0) > 0.0 }
        val nextHour = weather.hourlyForecast.firstOrNull { it.date >= now }
        val probability = nextHour?.precipitationProbability?.total?.inPercent
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt()
        val minuteText = when {
            minutes.isEmpty() -> null
            firstWet == null -> "sin lluvia en los próximos 60 min"
            firstWet.date.time - now.time < 5 * 60 * 1000L -> "lloviendo ahora o en minutos"
            else -> "lluvia en ~${((firstWet.date.time - now.time) / 60_000L).toInt()} min"
        }
        return listOfNotNull(
            minuteText?.replaceFirstChar { it.uppercase() },
            probability?.let { "Probabilidad próxima hora: $it %" },
        ).joinToString(" · ").ifEmpty { "Sin datos de lluvia para la próxima hora" }
    }

    /** Forecast part of the risk index: next 3 h probability (max) and amount (sum), first wet minute. */
    private fun forecastInputs(location: Location): AlertaEcRainRisk.Inputs {
        val weather = location.weather ?: return AlertaEcRainRisk.Inputs()
        val now = Date()
        val in3h = Date(now.time + 3 * 60 * 60 * 1000L)
        val hours = weather.hourlyForecast.filter { it.date >= Date(now.time - 60 * 60 * 1000L) && it.date < in3h }
        val probability = hours.mapNotNull {
            it.precipitationProbability?.total?.inPercent?.takeIf { p -> p.isFinite() && p in 0.0..100.0 }
        }.maxOrNull()?.roundToInt()
        val amount = hours.mapNotNull {
            it.precipitation?.total?.inMillimeters?.takeIf { mm -> mm.isFinite() && mm >= 0.0 }
        }.takeIf { it.isNotEmpty() }?.sum()
        val firstWet = weather.minutelyForecast
            .filter { it.date >= now && it.date < Date(now.time + 60 * 60 * 1000L) }
            .firstOrNull { (it.precipitationIntensity?.inMillimeters ?: 0.0) > 0.0 }
        return AlertaEcRainRisk.Inputs(
            probabilityPercent = probability,
            forecastMm3h = amount,
            rainingWithinMinutes = firstWet?.let { ((it.date.time - now.time) / 60_000L).toInt().coerceAtLeast(0) },
        )
    }

    private data class EcuadorData(
        val lines: List<String> = emptyList(),
        val measuredMm: Double? = null,
        val inamhiMm: Double? = null,
        val reports: Int = 0,
    )

    @Composable
    private fun RainNowCard(context: Context, location: Location, forecastLine: String) {
        val baseInputs = androidx.compose.runtime.remember(location) { forecastInputs(location) }
        val ecuador by produceState(initialValue = EcuadorData(), location.latitude, location.longitude) {
            if (!InamhiPointForecast.covers(location.latitude, location.longitude)) return@produceState
            val es = Locale.forLanguageTag("es-EC")
            val gauge = runCatching { QuitoRainGauges.lastHour(location.latitude, location.longitude) }.getOrNull()
            val inamhi = runCatching { InamhiPointForecast.at(location.latitude, location.longitude) }.getOrNull()
            // Community reports of rain / flooding nearby in the last 3 h (only when Supabase is configured).
            val reports = if (AlertaEcCommunity.isConfigured) {
                (AlertaEcCommunity.recentReports(location.latitude, location.longitude, hours = 3)
                    as? AlertaEcCommunity.Result.Ok)?.value
                    ?.count { it.category != "Error de aplicación" } ?: 0
            } else {
                0
            }
            value = EcuadorData(
                lines = listOfNotNull(
                    gauge?.let {
                        "EPMAPS " + it.station.code + ": " +
                            if (it.lastHourMm > 0.0) String.format(es, "%.1f mm medidos", it.lastHourMm) else "sin lluvia medida"
                    },
                    inamhi?.precipitation3hMm?.let { String.format(es, "INAMHI WRF: %.1f mm en 3 h", it) },
                ),
                measuredMm = gauge?.lastHourMm,
                inamhiMm = inamhi?.precipitation3hMm,
                reports = reports,
            )
        }
        val ecuadorLines = ecuador.lines
        val risk = AlertaEcRainRisk.evaluate(
            baseInputs.copy(
                measuredLastHourMm = ecuador.measuredMm,
                inamhi3hMm = ecuador.inamhiMm,
                communityReports = ecuador.reports,
            )
        )
        var showWhy by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "🌧️ Lluvia ahora",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Risk index 0-100 with its level; "¿Por qué?" shows every source that moved it.
                Text(
                    text = "${risk.level.emoji} ${risk.level.label} · índice ${risk.score}/100",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = risk.level.advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = forecastLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                androidx.compose.material3.TextButton(onClick = { showWhy = !showWhy }) {
                    Text(if (showWhy) "Ocultar el porqué ▲" else "¿Por qué este nivel? ▼")
                }
                if (showWhy) {
                    risk.reasons.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        AlertaEcRainRisk.Level.entries.joinToString("\n") { "${it.emoji} ${it.label}: ${it.advice}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Guía orientativa de ALERTA EC; no reemplaza los avisos de la SNGR ni del INAMHI.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ecuadorLines.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Phone: one button per line, full width and centred, so no word is cut.
                // Android TV / tablets: side by side. Both keep the neon focus and no purple fill.
                androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val openMap: @Composable () -> Unit = {
                        Text(
                            "🗺️ Abrir mapa",
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    val support: @Composable () -> Unit = {
                        Text(
                            "🤝 Colaborar",
                            maxLines = 1,
                            softWrap = false,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    val mapAction = { context.startActivity(Intent(context, MapActivity::class.java)) }
                    if (maxWidth < 420.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            NeonButton(onClick = mapAction, modifier = Modifier.fillMaxWidth()) { openMap() }
                            NeonButton(onClick = { showSupportBlock() }, modifier = Modifier.fillMaxWidth()) { support() }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            NeonButton(onClick = mapAction, modifier = Modifier.weight(1f)) { openMap() }
                            NeonButton(onClick = { showSupportBlock() }, modifier = Modifier.weight(1f)) { support() }
                        }
                    }
                }
            }
        }
    }

    /** Scrolls the home list to the support block and gives it focus (Android TV remote). */
    private fun showSupportBlock() {
        val list = itemView.parent as? RecyclerView ?: return
        val position = (list.adapter as? MainAdapter)?.supportPosition()?.takeIf { it >= 0 } ?: return
        list.smoothScrollToPosition(position)
        list.postDelayed({ list.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }, 600L)
    }
}
