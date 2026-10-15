package org.breezyweather.ui.support

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.breezyweather.R

private const val PAYPHONE_URL = "https://ppls.me/izAAuu8fptGHF6uZBV9L4Q"

@Composable
internal fun SupportSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showOpenError by rememberSaveable { mutableStateOf(false) }
    val methods = remember {
        listOf(
            R.string.alertaec_support_paypal to "https://www.paypal.com/ncp/payment/NKS3NQA6Q2Q5N",
            R.string.alertaec_support_patreon to "https://www.patreon.com/checkout/clicktvdigital?rid=29823674&vanity=11078284",
            R.string.alertaec_support_github to "https://github.com/sponsors/clicktvdigital"
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.alertaec_support_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.alertaec_support_description),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = { openPayPhone(context) { showOpenError = true } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.alertaec_support_payphone_button))
        }
        methods.forEach { (labelId, url) ->
            TextButton(
                onClick = {
                    try {
                        uriHandler.openUri(url)
                    } catch (_: ActivityNotFoundException) {
                        showOpenError = true
                    } catch (_: SecurityException) {
                        showOpenError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(labelId),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    if (showOpenError) {
        AlertDialog(
            onDismissRequest = { showOpenError = false },
            title = { Text(stringResource(R.string.alertaec_support_open_error_title)) },
            text = { Text(stringResource(R.string.alertaec_support_open_error_message)) },
            confirmButton = {
                TextButton(onClick = { showOpenError = false }) {
                    Text(stringResource(R.string.alertaec_support_close))
                }
            }
        )
    }
}

private fun openPayPhone(context: Context, onError: () -> Unit) {
    val uri = Uri.parse(PAYPHONE_URL)
    if (uri.scheme != "https" || uri.host != "ppls.me") {
        onError()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.alertaec_support_open_with))
        )
    } catch (_: ActivityNotFoundException) {
        onError()
    } catch (_: SecurityException) {
        onError()
    }
}
