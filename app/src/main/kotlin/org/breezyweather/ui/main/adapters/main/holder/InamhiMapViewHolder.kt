package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
import org.breezyweather.ui.map.MapActivity
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.resource.providers.ResourceProvider

class InamhiMapViewHolder(
    private val composeView: ComposeView
) : AbstractMainViewHolder(composeView) {

    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean
    ) {
        super.onBindView(
            context, location, provider,
            listAnimationEnabled, itemAnimationEnabled
        )

        composeView.setContent {
            BreezyWeatherTheme(!ThemeManager.isLightTheme(context, location)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🗺️ Meteorología de Ecuador",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lluvias · Temperatura · Capas INAMHI",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Fuentes y cobertura indicadas dentro del visor. " +
                            "El pronóstico no equivale a observación en tiempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { context.startActivity(Intent(context, MapActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Explorar mapa meteorológico")
                    }
                }
            }
        }
    }
}
