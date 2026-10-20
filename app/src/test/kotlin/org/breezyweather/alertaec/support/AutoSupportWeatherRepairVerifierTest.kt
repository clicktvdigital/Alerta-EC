package org.breezyweather.alertaec.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoSupportWeatherRepairVerifierTest {
    private val started = System.currentTimeMillis() - 60_000L

    private fun check(
        id: String,
        result: AutoSupportResult,
        acquiredAt: Long? = null,
    ) = AutoSupportCheck(
        id = id,
        description = id,
        result = result,
        detail = "Prueba",
        acquiredAtMillis = acquiredAt,
    )

    @Test
    fun completeRecoveryRequiresThreeNewAcquisitions() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf("weather_current", "weather_daily", "weather_hourly").map {
            check(it, AutoSupportResult.CORRECTO, started + 1_000L)
        }
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertEquals(3, result.count { it.result == AutoSupportResult.REPARADO })
        assertEquals(0, result.count { it.result == AutoSupportResult.PENDIENTE })
    }

    @Test
    fun partialRecoveryKeepsPendingSections() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, started + 1_000L),
            check("weather_daily", AutoSupportResult.CORRECTO, started - 1_000L),
            check("weather_hourly", AutoSupportResult.PENDIENTE),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertEquals(AutoSupportResult.REPARADO, result.first { it.id == "weather_current" }.result)
        assertEquals(AutoSupportResult.PENDIENTE, result.first { it.id == "weather_daily" }.result)
        assertEquals(AutoSupportResult.PENDIENTE, result.first { it.id == "weather_hourly" }.result)
        assertTrue(result.any { it.id == "weather_partial_recovery" && it.result == AutoSupportResult.PENDIENTE })
    }

    @Test
    fun partialRecoveryMustKeepGeneralMissingDataWarning() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, started + 1_000L),
            check("weather_daily", AutoSupportResult.PENDIENTE),
            check("weather_hourly", AutoSupportResult.PENDIENTE),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)

        assertEquals(
            AutoSupportResult.REPARADO,
            result.first { it.id == "weather_current" }.result,
        )
        assertTrue(result.any {
            it.id == "weather_data" && it.result == AutoSupportResult.PENDIENTE
        })
        assertTrue(result.any {
            it.id == "weather_partial_recovery" &&
                it.result == AutoSupportResult.PENDIENTE
        })
    }

    @Test
    fun oldDailyAcquisitionPreventsCompleteRecovery() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, started + 1_000L),
            check("weather_daily", AutoSupportResult.CORRECTO, started - 1_000L),
            check("weather_hourly", AutoSupportResult.CORRECTO, started + 1_000L),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)

        assertTrue(result.any {
            it.id == "weather_data" && it.result == AutoSupportResult.PENDIENTE
        })
        assertTrue(result.any {
            it.id == "weather_partial_recovery" &&
                it.result == AutoSupportResult.PENDIENTE
        })
        assertEquals(2, result.count { it.result == AutoSupportResult.REPARADO })
    }

    @Test
    fun completeRecoveryClearsGeneralMissingDataWarning() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, started + 1_000L),
            check("weather_daily", AutoSupportResult.CORRECTO, started + 1_000L),
            check("weather_hourly", AutoSupportResult.CORRECTO, started + 1_000L),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)

        assertEquals(3, result.count { it.result == AutoSupportResult.REPARADO })
        assertTrue(result.none { it.id == "weather_data" })
        assertTrue(result.none { it.id == "weather_partial_recovery" })
    }

    @Test
    fun oldDataCannotClearMissingDataWarning() {
        val before = listOf(check("weather_data", AutoSupportResult.PENDIENTE))
        val after = listOf(check("weather_current", AutoSupportResult.CORRECTO, started - 1_000L))
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertTrue(result.any { it.id == "weather_data" && it.result == AutoSupportResult.PENDIENTE })
        assertEquals(0, result.count { it.result == AutoSupportResult.REPARADO })
    }

    @Test
    fun missingPreviousCheckRemainsPending() {
        val before = listOf(
            check("weather_current", AutoSupportResult.PENDIENTE),
            check("weather_daily", AutoSupportResult.PENDIENTE),
        )
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, started + 1_000L),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertTrue(result.any {
            it.id == "weather_daily" && it.result == AutoSupportResult.PENDIENTE
        })
    }

    @Test
    fun unchangedAcquisitionCannotBeDeclaredRepaired() {
        val acquired = started + 1_000L
        val before = listOf(
            check("weather_current", AutoSupportResult.PENDIENTE, acquired),
        )
        val after = listOf(
            check("weather_current", AutoSupportResult.CORRECTO, acquired),
        )
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertEquals(0, result.count { it.result == AutoSupportResult.REPARADO })
    }

    @Test
    fun futureAcquisitionCannotCountAsRepair() {
        val before = listOf(check("weather_current", AutoSupportResult.PENDIENTE))
        val after = listOf(check("weather_current", AutoSupportResult.CORRECTO, System.currentTimeMillis() + 3_600_000L))
        val result = AutoSupportWeatherRepairVerifier.compare(before, after, started)
        assertEquals(0, result.count { it.result == AutoSupportResult.REPARADO })
    }
}
