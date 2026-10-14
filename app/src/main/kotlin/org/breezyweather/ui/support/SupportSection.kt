package org.breezyweather.ui.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
internal fun SupportSection() {
    val uriHandler = LocalUriHandler.current
    val methods = listOf(
        "PayPhone · Aporte voluntario" to "https://ppls.me/izAAuu8fptGHF6uZBV9L4Q",
        "PayPal · Aporte voluntario" to "https://www.paypal.com/ncp/payment/NKS3NQA6Q2Q5N",
        "Patreon · Aporte mensual de USD 1" to "https://www.patreon.com/checkout/clicktvdigital?rid=29823674&vanity=11078284",
        "GitHub Sponsors" to "https://github.com/sponsors/clicktvdigital"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("💙 Apoya a ALERTA EC 🇪🇨", style = MaterialTheme.typography.titleMedium)
        Text(
            "Los aportes son voluntarios. Las alertas y funciones esenciales seguirán siendo gratuitas.",
            style = MaterialTheme.typography.bodyMedium
        )
        methods.forEach { (name, url) ->
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(url) }.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
