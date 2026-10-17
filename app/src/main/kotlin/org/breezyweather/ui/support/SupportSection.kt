package org.breezyweather.ui.support

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.breezyweather.R

// ALERTA_EC_APOYOS_ACCESIBLES_2026_10_16_V2
// Regresar desde un banco NO demuestra que un pago haya sido realizado.
private data class Apoyo(
    val nombre: String,
    val explicacion: String,
    val qr: Int,
    val marca: Int?,
    val url: String
)

private val apoyos = listOf(
    Apoyo(
        "PayPhone", "Pago voluntario con PayPhone",
        R.drawable.alertaec_qr_payphone, null,
        "https://ppls.me/izAAuu8fptGHF6uZBV9L4Q"
    ),
    Apoyo(
        "Banco Pichincha", "Cuenta de Banco Pichincha · cobro por Deuna",
        R.drawable.alertaec_qr_pichincha, R.drawable.alertaec_marca_pichincha,
        "https://pagar.deuna.app/H92p/person?id=v1e_WT373U4AKPZ46YWW"
    ),
    Apoyo(
        "Deuna", "Cuenta Deuna independiente de Banco Pichincha",
        R.drawable.alertaec_qr_deuna, R.drawable.alertaec_marca_deuna,
        "https://pagar.deuna.app/H92p/person/payment?id=858a7565a7fa5e9bc2302b7836e1fdf2d8635300"
    ),
    Apoyo(
        "Binance Pay", "Aporte mediante Binance Pay",
        R.drawable.alertaec_qr_binance, R.drawable.alertaec_marca_binance,
        "https://app.binance.com/uni-qr/RMqefYgh"
    )
)

private val otrosApoyos = listOf(
    "PayPal" to "https://www.paypal.com/ncp/payment/NKS3NQA6Q2Q5N",
    "Patreon" to "https://www.patreon.com/clicktvdigital",
    "GitHub Sponsors" to "https://github.com/sponsors/clicktvdigital"
)

@Composable
internal fun SupportSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var qrSeleccionado by rememberSaveable { mutableIntStateOf(-1) }
    var mensajeError by rememberSaveable { mutableStateOf(false) }
    var enlacePendiente by rememberSaveable { mutableStateOf(false) }
    var salioDePantalla by rememberSaveable { mutableStateOf(false) }
    var preguntarAlVolver by rememberSaveable { mutableStateOf(false) }
    var mostrarGracias by rememberSaveable { mutableStateOf(false) }
    var valorar by rememberSaveable { mutableStateOf(false) }
    var estrellas by rememberSaveable { mutableIntStateOf(0) }
    var montoSeleccionado by rememberSaveable { mutableIntStateOf(3) }

    // Pregunta opcional al regresar; no interpreta el retorno como un pago exitoso.
    DisposableEffect(lifecycleOwner, enlacePendiente, salioDePantalla) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && enlacePendiente) {
                salioDePantalla = true
            }
            if (event == Lifecycle.Event.ON_RESUME && enlacePendiente && salioDePantalla) {
                enlacePendiente = false
                salioDePantalla = false
                preguntarAlVolver = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun abrirPago(url: String) {
        if (abrirEnlaceSeguro(context, url)) {
            enlacePendiente = true
            salioDePantalla = false
        } else {
            mensajeError = true
        }
    }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ALERTA_EC_APOYOS_VISUALES_V017: portada cercana y sin prometer servicios de emergencia.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🇪🇨💚 Un pequeño gesto, una gran ayuda", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Invítame un café o un encebollado y ayúdame a seguir mejorando ALERTA EC para Ecuador. 🦅",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Gracias por apoyar este proyecto independiente de CLICK TV DIGITAL.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            "ALERTA EC es gratuita. Si deseas apoyar su desarrollo, elige una opción de aporte voluntario.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Al pagar, verifica el destinatario y el valor en tu aplicación bancaria. " +
                "Ninguna donación da prioridad en emergencias.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Importes orientativos: no se envían al enlace o QR de ningún proveedor.
        Text("☕ ¿Con cuánto te gustaría colaborar?", style = MaterialTheme.typography.titleMedium)
        val propuestas = listOf(
            Triple("☕", "Invítame un café", 1),
            Triple("🍲", "Un encebollado", 3),
            Triple("💚", "Apoya el proyecto", 5),
            Triple("🦅", "Impulsa ALERTA EC", 10),
        )
        propuestas.chunked(2).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                fila.forEach { (emoji, nombre, monto) ->
                    val seleccionado = montoSeleccionado == monto
                    Button(
                        onClick = { montoSeleccionado = monto },
                        modifier = Modifier.weight(1f).heightIn(min = 86.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (seleccionado) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (seleccionado) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                (if (seleccionado) "✓ " else "") + "$emoji $nombre",
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text("USD $monto", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        Text(
            "💛 Aporte sugerido: USD $montoSeleccionado. También puedes elegir otro importe " +
                "si la plataforma de pago lo permite. Ningún valor se cobra ni se transfiere desde ALERTA EC.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "Elige cómo deseas aportar. Verifica el destinatario y escribe o confirma el valor " +
                "antes de autorizar cualquier pago.",
            style = MaterialTheme.typography.bodyMedium,
        )

        apoyos.forEachIndexed { indice, apoyo ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (apoyo.marca != null) {
                            Image(
                                painter = painterResource(apoyo.marca),
                                contentDescription = "Marca de ${apoyo.nombre}",
                                modifier = Modifier.size(width = 120.dp, height = 46.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("💳 PayPhone", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(apoyo.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    }
                    Text(apoyo.explicacion, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { abrirPago(apoyo.url) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    ) { Text("💚 Continuar con ${apoyo.nombre}") }
                    OutlinedButton(
                        onClick = { qrSeleccionado = indice },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { Text("📷 Ver QR y copiar enlace") }
                }
            }
        }

        Text("Otras formas de apoyar", style = MaterialTheme.typography.titleMedium)
        otrosApoyos.forEach { (nombre, url) ->
            OutlinedButton(
                onClick = { abrirPago(url) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Abrir $nombre") }
        }
        Button(
            onClick = { compartirAplicacion(context) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
        ) { Text("Compartir ALERTA EC con familiares y amigos") }
        TextButton(
            onClick = { preguntarAlVolver = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Ya regresé de mi aplicación de pago") }
    }

    if (qrSeleccionado in apoyos.indices) {
        val seleccionado = apoyos[qrSeleccionado]
        AlertDialog(
            onDismissRequest = { qrSeleccionado = -1 },
            title = { Text("QR · ${seleccionado.nombre}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(seleccionado.qr),
                        contentDescription = "Código QR para donar con ${seleccionado.nombre}",
                        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 320.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text("En este mismo teléfono puedes abrir el enlace directamente. Comprueba el destinatario antes de pagar.")
                    TextButton(
                        onClick = {
                            copiarEnlace(context, seleccionado.url)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) { Text("Copiar enlace de cobro") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    qrSeleccionado = -1
                    abrirPago(seleccionado.url)
                }) { Text("Abrir pago") }
            },
            dismissButton = { TextButton(onClick = { qrSeleccionado = -1 }) { Text("Cerrar") } }
        )
    }

    if (preguntarAlVolver) {
        AlertDialog(
            onDismissRequest = { preguntarAlVolver = false },
            title = { Text("¿Pudiste completar tu aporte?") },
            text = { Text("Solo tu aplicación de pago puede confirmar la transacción. ALERTA EC no puede comprobarla automáticamente.") },
            confirmButton = {
                TextButton(onClick = {
                    preguntarAlVolver = false
                    mostrarGracias = true
                }) { Text("Sí, completé mi aporte") }
            },
            dismissButton = {
                TextButton(onClick = { preguntarAlVolver = false }) { Text("Aún no / Cancelar") }
            }
        )
    }

    if (mostrarGracias) {
        AlertDialog(
            onDismissRequest = { mostrarGracias = false },
            title = { Text("❤️ ¡Gracias por apoyar ALERTA EC!") },
            text = { Text("Gracias por tu aporte voluntario a este proyecto de CLICK TV DIGITAL. ¿Nos ayudas a compartirlo o contar tu experiencia?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarGracias = false
                    compartirAplicacion(context)
                }) { Text("Compartir aplicación") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarGracias = false
                    valorar = true
                }) { Text("Dejar mi opinión") }
            }
        )
    }

    if (valorar) {
        AlertDialog(
            onDismissRequest = { valorar = false },
            title = { Text("Tu opinión sobre ALERTA EC") },
            text = {
                Column {
                    Text("Elige libremente de 1 a 5 estrellas. Tu valoración no se publicará automáticamente.")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        (1..5).forEach { numero ->
                            TextButton(onClick = { estrellas = numero }) {
                                Text(if (numero <= estrellas) "★" else "☆", fontSize = 27.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = estrellas > 0,
                    onClick = {
                        valorar = false
                        enviarOpinion(context, estrellas)
                    }
                ) { Text("Enviar por correo") }
            },
            dismissButton = { TextButton(onClick = { valorar = false }) { Text("Ahora no") } }
        )
    }

    if (mensajeError) {
        AlertDialog(
            onDismissRequest = { mensajeError = false },
            title = { Text("No se pudo abrir el enlace") },
            text = { Text("Revisa tu conexión o intenta copiar el enlace de pago. No se realizó ningún cobro desde ALERTA EC.") },
            confirmButton = { TextButton(onClick = { mensajeError = false }) { Text("Entendido") } }
        )
    }
}

private fun abrirEnlaceSeguro(context: Context, url: String): Boolean {
    val uri = Uri.parse(url)
    val hosts = setOf("ppls.me", "pagar.deuna.app", "app.binance.com", "www.paypal.com", "www.patreon.com", "github.com")
    if (uri.scheme != "https" || uri.host !in hosts) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun copiarEnlace(context: Context, url: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Enlace de donación ALERTA EC", url))
}

private fun compartirAplicacion(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "🇪🇨 ALERTA EC — Aplicación meteorológica gratuita de CLICK TV DIGITAL. Descargas oficiales: https://github.com/clicktvdigital/Alerta-EC/releases")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartir ALERTA EC"))
    } catch (_: ActivityNotFoundException) {
        // Puede no haber aplicaciones instaladas para compartir.
    }
}

private fun enviarOpinion(context: Context, estrellas: Int) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:clicktvdigital@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "Mi opinión sobre ALERTA EC")
        putExtra(Intent.EXTRA_TEXT, "Mi valoración: $estrellas de 5 estrellas.\n\nMi opinión: ")
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No hay correo configurado; no se crea una reseña ficticia.
    }
}
