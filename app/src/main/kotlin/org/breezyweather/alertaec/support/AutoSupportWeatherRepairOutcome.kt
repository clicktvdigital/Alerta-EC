package org.breezyweather.alertaec.support

/**
 * Traduce el seguimiento de WorkManager a decisiones de autosoporte.
 * Nunca considera la finalizacion del worker como reparacion demostrada.
 */
internal object AutoSupportWeatherRepairOutcome {

    internal data class Assessment(
        val shouldRecheckWeather: Boolean,
        val message: String,
    )

    fun assess(
        outcome: AutoSupportWeatherWorkTracker.Outcome,
    ): Assessment = when (outcome) {
        AutoSupportWeatherWorkTracker.Outcome.TERMINADO ->
            Assessment(
                shouldRecheckWeather = true,
                message = "La actualizacion termino. Es necesario comprobar si se recibieron datos nuevos.",
            )

        AutoSupportWeatherWorkTracker.Outcome.FALLIDO ->
            Assessment(
                shouldRecheckWeather = true,
                message = "El trabajo informo un fallo. Se comprobara si hubo datos parciales.",
            )

        AutoSupportWeatherWorkTracker.Outcome.CANCELADO ->
            Assessment(
                shouldRecheckWeather = false,
                message = "La actualizacion fue cancelada. Reparacion no confirmada.",
            )

        AutoSupportWeatherWorkTracker.Outcome.NO_ENCONTRADO ->
            Assessment(
                shouldRecheckWeather = false,
                message = "No se encontro el trabajo solicitado. Reparacion no confirmada.",
            )

        AutoSupportWeatherWorkTracker.Outcome.TIEMPO_AGOTADO ->
            Assessment(
                shouldRecheckWeather = false,
                message = "La actualizacion sigue sin resultado confirmado. Puede continuar en segundo plano.",
            )

        AutoSupportWeatherWorkTracker.Outcome.ERROR ->
            Assessment(
                shouldRecheckWeather = false,
                message = "No fue posible consultar el resultado del trabajo. Reparacion no confirmada.",
            )
    }
}
