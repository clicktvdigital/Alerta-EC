package org.breezyweather.alertaec

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Base
import breezyweather.domain.weather.model.Hourly
import breezyweather.domain.weather.model.Weather
import breezyweather.domain.weather.reference.WeatherCode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.TimeZone

class AlertaEcWeatherFreshnessTest {
    private val now = 1_800_000_000_000L

    @Test
    fun recentQueryAndDailyUpdateCannotMakeOldHourlyDataFresh() {
        val oldAcquisition = now - 2 * 3_600_000L
        val location = location(oldAcquisition)
        val event = AlertaEcWeatherAdapter.fromLocation(location, now).single()
        event.updatedAtMillis shouldBe oldAcquisition
        AlertaEcNotificationPolicy.shouldNotify(event, now) shouldBe false
    }

    @Test
    fun legacyHourlyDataNeverInheritsTheBaseTimestamps() {
        val event = AlertaEcWeatherAdapter.fromLocation(location(null), now).single()
        event.updatedAtMillis shouldBe null
        AlertaEcNotificationPolicy.shouldNotify(event, now) shouldBe false
    }

    @Test
    fun aNewHourlyAcquisitionCanNotifyWithoutARecentDailyUpdate() {
        val location = location(now)
        val weather = location.weather!!
        val dailyOld = location.copy(weather = weather.copy(base = Base(forecastUpdateTime = Date(1))))
        val event = AlertaEcWeatherAdapter.fromLocation(dailyOld, now).single()
        event.updatedAtMillis shouldBe now
        AlertaEcNotificationPolicy.shouldNotify(event, now) shouldBe true
    }

    @Test
    fun officialAlertsRemainAvailableAndDoNotEnterTheOrdinaryNotifier() {
        val alert = Alert(alertId = "synthetic-official", color = 0, headline = "Alerta de prueba")
        val location = Location(weather = Weather(alertList = listOf(alert)))
        val event = AlertaEcWeatherAdapter.fromLocation(location, now).single()
        event.state shouldBe AlertaEcEventState.OFFICIAL_EVENT
        location.weather!!.alertList shouldBe listOf(alert)
        AlertaEcNotificationPolicy.shouldNotify(event, now) shouldBe false
    }

    @Test
    fun expiredBoundaryDoesNotMaskTheCurrentHour() {
        val location = location(now)
        val weather = location.weather!!
        val current = weather.hourlyForecast.single().copy(date = Date(now))
        val expired = current.copy(date = Date(now - 3_600_000L))
        val events = AlertaEcWeatherAdapter.fromLocation(
            location.copy(weather = weather.copy(hourlyForecast = listOf(expired, current))),
            now
        )
        events.single().startTimeMillis shouldBe now
        AlertaEcNotificationPolicy.shouldNotify(events.single(), now) shouldBe true
    }

    private fun location(acquiredAt: Long?) = Location(
        timeZone = TimeZone.getTimeZone("America/Guayaquil"),
        weather = Weather(
            base = Base(refreshTime = Date(now), forecastUpdateTime = Date(now)),
            hourlyForecast = listOf(
                Hourly(
                    date = Date(now + 60_000L),
                    weatherCode = WeatherCode.RAIN,
                    acquiredAt = acquiredAt?.let { Date(it) }
                )
            )
        )
    )
}
