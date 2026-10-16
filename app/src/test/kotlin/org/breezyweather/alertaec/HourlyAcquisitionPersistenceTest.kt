package org.breezyweather.alertaec

import breezyweather.data.weather.WeatherMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Date

class HourlyAcquisitionPersistenceTest {
    @Test
    fun databaseMapperKeepsNullForLegacyRowsAndPreservesTheStoredTimestamp() {
        for (stored in listOf(null, 1_800_000_000_000L)) {
            val hour = WeatherMapper.mapHourly(
                date = 1_800_003_600_000L,
                daylight = true,
                weatherText = null,
                weatherCode = null,
                temperature = null,
                sourceFeelsLikeTemperature = null,
                apparentTemperature = null,
                windChillTemperature = null,
                humidex = null,
                totalPrecipitation = null,
                thunderstormPrecipitation = null,
                rainPrecipitation = null,
                snowPrecipitation = null,
                icePrecipitation = null,
                totalPrecipitationProbability = null,
                thunderstormPrecipitationProbability = null,
                rainPrecipitationProbability = null,
                snowPrecipitationProbability = null,
                icePrecipitationProbability = null,
                windDegree = null,
                windSpeed = null,
                windGusts = null,
                pm25 = null,
                pm10 = null,
                so2 = null,
                no2 = null,
                o3 = null,
                co = null,
                uvIndex = null,
                relativeHumidity = null,
                dewPoint = null,
                pressure = null,
                cloudCover = null,
                visibility = null,
                acquiredAt = stored
            )
            hour.acquiredAt shouldBe stored?.let { Date(it) }
            hour.toHourlyWrapper().toHourly().acquiredAt?.time shouldBe stored
        }
    }
}
