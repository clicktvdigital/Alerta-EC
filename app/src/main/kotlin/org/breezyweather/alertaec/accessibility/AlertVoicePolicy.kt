package org.breezyweather.alertaec.accessibility

/** No backlog: an alert suppressed by Android or initial setup is never replayed later. */
internal object AlertVoicePolicy {
    fun mayReadAutomatic(hasPreviousWeather: Boolean, active: Boolean, end: Long?, now: Long): Boolean =
        hasPreviousWeather && active && end != null && end > now

    fun speechRate(value: Float): Float = if (value.isFinite()) value.coerceIn(0.5f, 1.5f) else 1f

    fun summary(phenomenon: String): String =
        "Alerta meteorológica: $phenomenon. Consulta los detalles y las indicaciones de la fuente en la aplicación."
}
