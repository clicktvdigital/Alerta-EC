package org.breezyweather.ui.support

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.breezyweather.R
import org.breezyweather.ui.about.AboutActivity
import org.breezyweather.ui.map.MapActivity

/**
 * OK on the Home cards with their own buttons (Compose). Compose buttons cannot take the TV D-pad
 * inside a RecyclerView card: a card with one action runs it directly; with several, OK opens a native list. Each row runs
 * its action directly (Intent or dialog). D-pad navigates the list natively.
 */
object TvCardActions {
    const val TAG_MAP = "tv_card_map"
    const val TAG_REVIEW = "tv_card_review"
    const val TAG_REPORT = "tv_card_report"
    const val TAG_DONATIONS = "tv_card_donations"

    /** True when the card has actions and the dialog was shown. */
    fun show(context: Context, tag: String): Boolean {
        val actions: List<Pair<String, () -> Unit>> = when (tag) {
            TAG_MAP -> listOf(
                "Abrir mapa meteorológico" to { context.startActivity(Intent(context, MapActivity::class.java)) },
            )
            TAG_REVIEW -> listOf("Escribir reseña" to { showFeedback(context, false) })
            TAG_REPORT -> listOf("Redactar reporte y adjuntar evidencia" to { showFeedback(context, true) })
            TAG_DONATIONS -> listOf(
                "Ver formas para colaborar" to { context.startActivity(Intent(context, AboutActivity::class.java)) },
            )
            else -> return false
        }
        // One action: OK runs it directly, like pressing the card's button (no extra window).
        if (actions.size == 1) {
            actions.single().second()
            return true
        }
        MaterialAlertDialogBuilder(context)
            .setItems(actions.map { it.first }.toTypedArray()) { _, which -> actions[which].second() }
            .setNegativeButton(R.string.action_close, null)
            .show()
        return true
    }
}
