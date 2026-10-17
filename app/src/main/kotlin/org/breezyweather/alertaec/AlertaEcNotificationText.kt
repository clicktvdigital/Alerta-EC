package org.breezyweather.alertaec

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object AlertaEcNotificationText {
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
