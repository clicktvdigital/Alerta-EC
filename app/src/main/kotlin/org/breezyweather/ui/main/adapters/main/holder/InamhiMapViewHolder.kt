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
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🗺️ Mapa meteorológico de Ecuador 🇪🇨",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Explora el mapa de Ecuador y consulta las capas INAMHI de precipitación y temperatura.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Fuente: INAMHI · Datos sujetos a disponibilidad y cobertura.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(context, MapActivity::class.java)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abrir mapa meteorológico")
                        }
                    }
                }
            }
        }
    }
}
