package org.breezyweather.sources

import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.model.Base
import breezyweather.domain.weather.model.Hourly
import breezyweather.domain.weather.model.Weather
import breezyweather.domain.weather.reference.WeatherCode
import breezyweather.domain.weather.wrappers.DailyWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.WeatherWrapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date

class HourlyAcquisitionTest {
    private val now = 1_800_000_000_000L
    private val oldTime = now - 2 * 3_600_000L

    @Test
    fun receiptStampsOnlyIncomingRecordsAndConversionsPreserveIt() {
        val response = WeatherWrapper(hourlyForecast = listOf(HourlyWrapper(Date(now))))
        val hour = response.withHourlyAcquisition(now).hourlyForecast!!.single().toHourly()
        hour.acquiredAt shouldBe Date(now)
        hour.toHourlyWrapper().toHourly().acquiredAt shouldBe Date(now)
        Hourly(Date(now)).toHourlyWrapper().toHourly().acquiredAt shouldBe null
    }

    @Test
    fun mergingNewAndCachedHoursPreservesTheirDifferentAcquisitionTimes() {
        val old = oldWeather()
        val response = WeatherWrapper(
            hourlyForecast = listOf(HourlyWrapper(Date(now + 3_600_000L), weatherCode = WeatherCode.RAIN))
        ).withHourlyAcquisition(now)
        val merged = merge(response, old).hourlyForecast!!
        merged.map { it.date.time } shouldBe listOf(now, now + 3_600_000L)
        merged.map { it.acquiredAt?.time } shouldBe listOf(oldTime, now)
        merged.map { it.toHourly().toHourlyWrapper().acquiredAt?.time } shouldBe listOf(oldTime, now)
    }

    @Test
    fun dailyOnlyResponseDoesNotRefreshHourlyCache() {
        val daily = WeatherWrapper(dailyForecast = listOf(DailyWrapper(Date(now)))).withHourlyAcquisition(now)
        val merged = merge(daily, oldWeather())
        merged.hourlyForecast!!.single().acquiredAt shouldBe Date(oldTime)
    }

    @Test
    fun failedOrEmptyHourlyResponsesDoNotInventCacheAcquisition() {
        val failed = WeatherWrapper(
            hourlyForecast = listOf(HourlyWrapper(Date(now))),
            failedFeatures = mapOf(SourceFeature.FORECAST to IllegalStateException("synthetic"))
        ).withHourlyAcquisition(now)
        failed.hourlyForecast!!.single().acquiredAt shouldBe null
        for (incoming in listOf(null, emptyList<HourlyWrapper>())) {
            val response = WeatherWrapper(hourlyForecast = incoming).withHourlyAcquisition(now)
            merge(response, oldWeather()).hourlyForecast!!.single().acquiredAt shouldBe Date(oldTime)
        }
    }

    @Test
    fun cacheFallbackAndLegacyRecordsKeepTheirOriginalOrUnknownTime() {
        val old = oldWeather()
        val cached = WeatherWrapper(hourlyForecast = old.toHourlyWrapperList(Date(now - 1)))
        merge(cached, old).hourlyForecast!!.single().acquiredAt shouldBe Date(oldTime)
        val legacy = old.copy(hourlyForecast = listOf(Hourly(Date(now))))
        merge(WeatherWrapper(), legacy).hourlyForecast!!.single().acquiredAt shouldBe null
    }

    private fun oldWeather() = Weather(
        base = Base(refreshTime = Date(now), forecastUpdateTime = Date(now)),
        hourlyForecast = listOf(Hourly(Date(now), acquiredAt = Date(oldTime)))
    )

    private fun merge(response: WeatherWrapper, old: Weather) = completeNewWeatherWithPreviousData(
        response, old, Date(now - 3_600_000L), airQualitySource = null, pollenSource = null
    )
}
