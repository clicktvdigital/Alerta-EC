/*
 * ALERTA EC: acceso voluntario a la sección de aportes.
 * Basado en Breezy Weather (LGPL-3.0); se conservan sus atribuciones.
 */
package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import org.breezyweather.ui.support.TvCardActions
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import org.breezyweather.ui.common.widgets.tvFocusIndicator
import org.breezyweather.ui.support.SupportSection
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.resource.providers.ResourceProvider

/** Invitación discreta bajo el primer pronóstico, nunca por encima de alertas. */
class DonationsViewHolder(
    private val composeView: ComposeView,
) : AbstractMainViewHolder(composeView) {

    /** Held here (not in remember) so OK on TV can open/close the options in place, without another window. */
    private val expanded = mutableStateOf(false)

    init {
        composeView.tag = TvCardActions.TAG_DONATIONS
    }

    /** OK on the card (TV remote): toggles «Ver formas de colaborar» like pressing its blue button. */
    fun toggleFromRemote() {
        expanded.value = !expanded.value
    }

    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
    ) {
        super.onBindView(context, location, provider, listAnimationEnabled, itemAnimationEnabled)
        composeView.setContent {
            BreezyWeatherTheme(!ThemeManager.isLightTheme(context, location)) {
                DonationsPanel()
            }
        }
    }

    @Composable
    private fun DonationsPanel() {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "❤️ Apoya ALERTA EC 🇪🇨",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Una aplicación gratuita para todos. Tu apoyo voluntario ayuda a seguir mejorándola.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "☕ Café: $1   ·   🍲 Encebollado: $3   ·   💚 $5   ·   🦅 $10",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = { expanded.value = !expanded.value },
                    modifier = Modifier.fillMaxWidth().tvFocusIndicator(),
                ) {
                    Text(if (expanded.value) "Ocultar opciones de apoyo ▲" else "Ver formas de colaborar ▼")
                }
                if (expanded.value) {
                    Text(
                        text = "Valores sugeridos, no cobros automáticos. Comprueba el importe y el destinatario al pagar.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // Reutiliza los QR y enlaces existentes; no duplica la lógica de pago.
                    SupportSection()
                }
            }
        }
    }
}
