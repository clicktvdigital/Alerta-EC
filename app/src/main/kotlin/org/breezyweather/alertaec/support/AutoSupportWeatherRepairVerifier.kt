package org.breezyweather.alertaec.support

/**
 * Compara dos informes meteorologicos, obtenidos antes y despues
 * de un intento de actualizacion. No realiza acciones sobre el sistema.
 *
 * Un estado CORRECTO posterior por si solo no demuestra reparacion:
 * debe existir evidencia de una adquisicion posterior.
 */
internal object AutoSupportWeatherRepairVerifier {

    private val weatherIds = setOf(
        "weather_current",
        "weather_daily",
        "weather_hourly",
    )

    fun compare(
        before: List<AutoSupportCheck>,
        after: List<AutoSupportCheck>,
        repairStartedAtMillis: Long,
    ): List<AutoSupportCheck> {
        val previousById = before.associateBy { it.id }
        val afterIds = after.map { it.id }.toSet()
        val missingWeatherBefore = before.any {
            it.id == "weather_data" && it.result == AutoSupportResult.PENDIENTE
        }
        val verificationTimeMillis = System.currentTimeMillis()
        val newlyAcquiredWeather = after.any { check ->
            check.id in weatherIds &&
                check.acquiredAtMillis != null &&
                check.acquiredAtMillis >= repairStartedAtMillis &&
                check.acquiredAtMillis <= verificationTimeMillis &&
                check.result == AutoSupportResult.CORRECTO
        }
        val completeWeatherRecovery = weatherIds.all { weatherId ->
            after.any { check ->
                check.id == weatherId &&
                    check.result == AutoSupportResult.CORRECTO &&
                    check.acquiredAtMillis != null &&
                    check.acquiredAtMillis >= repairStartedAtMillis &&
                    check.acquiredAtMillis <= verificationTimeMillis
            }
        }
        val missingPreviousChecks = before
            .filter { previous ->
                previous.id !in afterIds &&
                    !(previous.id == "weather_data" && completeWeatherRecovery)
            }
            .map { previous ->
                previous.copy(
                    result = AutoSupportResult.PENDIENTE,
                    detail = previous.detail +
                        " No se pudo confirmar nuevamente esta comprobacion despues del intento de actualizacion.",
                )
            }

        val partialWeatherRecovery = missingWeatherBefore &&
            newlyAcquiredWeather &&
            weatherIds.any { weatherId ->
                after.none { check ->
                    check.id == weatherId &&
                        check.result == AutoSupportResult.CORRECTO &&
                        check.acquiredAtMillis != null &&
                        check.acquiredAtMillis >= repairStartedAtMillis &&
                        check.acquiredAtMillis <= verificationTimeMillis
                }
            }

        val verifiedChecks = after.map { current ->
            val previous = previousById[current.id]
            val previousTime = previous?.acquiredAtMillis
            val currentTime = current.acquiredAtMillis
            val acquiredAfterRepairStarted =
                currentTime != null &&
                    currentTime >= repairStartedAtMillis &&
                    currentTime <= verificationTimeMillis
            val newerThanBefore =
                currentTime != null &&
                    (previousTime == null || currentTime > previousTime)

            if (
                current.id in weatherIds &&
                (previous?.result == AutoSupportResult.PENDIENTE ||
                    (previous == null && missingWeatherBefore)) &&
                current.result == AutoSupportResult.CORRECTO &&
                acquiredAfterRepairStarted &&
                newerThanBefore
            ) {
                current.copy(
                    result = AutoSupportResult.REPARADO,
                    detail = current.detail +
                        " Se verifico una adquisicion posterior al inicio del intento de reparacion.",
                )
            } else if (
                missingWeatherBefore &&
                current.id in weatherIds &&
                current.result == AutoSupportResult.CORRECTO &&
                !acquiredAfterRepairStarted
            ) {
                current.copy(
                    result = AutoSupportResult.PENDIENTE,
                    detail = current.detail +
                        " No se verifico una adquisicion nueva de esta seccion durante la recuperacion.",
                )
            } else {
                current
            }
        } + missingPreviousChecks

        return if (partialWeatherRecovery) {
            verifiedChecks + AutoSupportCheck(
                id = "weather_partial_recovery",
                description = "Recuperacion parcial del pronostico",
                result = AutoSupportResult.PENDIENTE,
                detail = "Se recibieron algunos datos meteorologicos nuevos, pero otras secciones siguen pendientes. No se confirma una recuperacion completa.",
            )
        } else {
            verifiedChecks
        }
    }
}
