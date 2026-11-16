package org.breezyweather.ui.support

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.breezyweather.R
import org.breezyweather.ui.about.AboutActivity
import org.breezyweather.ui.map.MapActivity

/**
 * OK on the Home cards with their own buttons (Compose). Compose buttons cannot take the TV D-pad
 * inside a RecyclerView card, so OK opens a native list of the card's actions instead. Each row runs
 * its action directly (Intent or dialog). D-pad navigates the list natively.
 */
object TvCardActions {
    const val TAG_MAP = "tv_card_map"
    const val TAG_DONATIONS = "tv_card_donations"

    /** True when the card has actions and the dialog was shown. */
    fun show(context: Context, tag: String): Boolean {
        val actions: List<Pair<String, () -> Unit>> = when (tag) {
            TAG_MAP -> listOf(
                "Abrir mapa meteorológico" to { context.startActivity(Intent(context, MapActivity::class.java)) },
                "Escribir reseña" to { showFeedback(context, false) },
                "Redactar reporte y adjuntar evidencia" to { showFeedback(context, true) },
            )
            TAG_DONATIONS -> listOf(
                "Ver formas para colaborar" to { context.startActivity(Intent(context, AboutActivity::class.java)) },
            )
            else -> return false
        }
        MaterialAlertDialogBuilder(context)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .setNegativeButton(R.string.action_close, null)
            .show()
        return true
    }
}
