package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    @Composable
    private fun RainNowCard(context: Context, location: Location, forecastLine: String) {
        val ecuadorLines by produceState(initialValue = emptyList<String>(), location.latitude, location.longitude) {
            if (!InamhiPointForecast.covers(location.latitude, location.longitude)) return@produceState
            val es = Locale.forLanguageTag("es-EC")
            val gauge = runCatching { QuitoRainGauges.lastHour(location.latitude, location.longitude) }.getOrNull()
            val inamhi = runCatching { InamhiPointForecast.at(location.latitude, location.longitude) }.getOrNull()
            value = listOfNotNull(
                gauge?.let {
                    "EPMAPS " + it.station.code + ": " +
                        if (it.lastHourMm > 0.0) String.format(es, "%.1f mm medidos", it.lastHourMm) else "sin lluvia medida"
                },
                inamhi?.precipitation3hMm?.let { String.format(es, "INAMHI WRF: %.1f mm en 3 h", it) },
            )
        }
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
                Text(
                    text = forecastLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ecuadorLines.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { context.startActivity(Intent(context, MapActivity::class.java)) }) {
                        Text("🗺️ Abrir mapa")
                    }
                    OutlinedButton(onClick = { showSupportBlock() }) {
                        Text("🤝 Colaborar")
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
