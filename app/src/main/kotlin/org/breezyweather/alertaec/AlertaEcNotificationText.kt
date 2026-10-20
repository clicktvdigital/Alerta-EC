package org.breezyweather.alertaec

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

internal object AlertaEcNotificationText {
    fun probabilityPercent(value: Double?): String = value
        ?.takeIf { it.isFinite() && it in 0.0..100.0 }
        ?.let { "Prob. ${it.roundToInt()}%" }
        ?: "Prob. —"

    fun accumulationMm(value: Double?): String = value
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.let { String.format("Acum. %.1f mm", it) }
        ?: "Acum. —"

    /** Hourly records identify a block, never the exact minute of onset. */
    fun hourlyPeriod(start: Long, now: Long, zone: TimeZone): String {
        val day = SimpleDateFormat("yyyyMMdd", Locale.ROOT).apply { timeZone = zone }
        val pattern = if (day.format(Date(start)) == day.format(Date(now))) "HH:mm" else "dd/MM HH:mm"
        val format = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = zone }
        return "Bloque horario desde ${format.format(Date(start))} (no indica el minuto exacto)"
    }

    fun interval(start: Long, now: Long, zone: TimeZone): String {
        val day = SimpleDateFormat("yyyyMMdd", Locale.ROOT).apply { timeZone = zone }
        val pattern = if (day.format(Date(start)) == day.format(Date(now))) "HH:mm" else "dd/MM HH:mm"
        val format = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = zone }
        val label = when {
            start > now -> "Prev."
            now - start < 3_600_000L -> "Desde"
            else -> "Intervalo pasado"
        }
        return "$label ${format.format(Date(start))}"
    }
}
