package org.breezyweather.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.breezyweather.R

internal fun shareAlertaEc(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.alertaec_share_message))
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.alertaec_share_title)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.action_share_no_app, Toast.LENGTH_SHORT).show()
    }
}
