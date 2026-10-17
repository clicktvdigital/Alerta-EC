package org.breezyweather.alertaec

import breezyweather.domain.weather.reference.AlertSeverity
import io.kotest.matchers.shouldBe
import org.breezyweather.R
import org.junit.jupiter.api.Test

class AlertaEcNotificationAppearanceTest {
    @Test
    fun iconsMatchThePhenomenon() {
        AlertaEcNotificationAppearance.icon(AlertaEcPhenomenon.RAIN) shouldBe R.drawable.ic_notification_rain
        AlertaEcNotificationAppearance.icon(AlertaEcPhenomenon.HEAVY_RAIN) shouldBe R.drawable.ic_notification_rain
        AlertaEcNotificationAppearance.icon(AlertaEcPhenomenon.THUNDERSTORM) shouldBe R.drawable.ic_notification_thunderstorm
        AlertaEcNotificationAppearance.icon(AlertaEcPhenomenon.STRONG_WIND) shouldBe R.drawable.ic_notification_wind
        AlertaEcNotificationAppearance.icon(AlertaEcPhenomenon.OTHER) shouldBe R.drawable.ic_notification_weather
    }

    @Test
    fun onlyOfficialSevereAndExtremeAlertsUseIntrusiveChannels() {
        AlertaEcNotificationAppearance.officialChannel(AlertSeverity.SEVERE) shouldBe AlertaEcNotificationAppearance.SEVERE
        AlertaEcNotificationAppearance.officialChannel(AlertSeverity.EXTREME) shouldBe AlertaEcNotificationAppearance.EXTREME
        for (severity in listOf(AlertSeverity.UNKNOWN, AlertSeverity.MINOR, AlertSeverity.MODERATE)) {
            AlertaEcNotificationAppearance.officialChannel(severity) shouldBe "forecast"
        }
    }

    @Test
    fun genericStormDoesNotInventLightning() {
        AlertaEcWeatherAdapter.phenomenonFromText("Tormenta tropical") shouldBe AlertaEcPhenomenon.OTHER
        AlertaEcWeatherAdapter.phenomenonFromText("Tormenta eléctrica con lluvia") shouldBe AlertaEcPhenomenon.THUNDERSTORM
        AlertaEcWeatherAdapter.phenomenonFromText("Strong wind warning") shouldBe AlertaEcPhenomenon.STRONG_WIND
        AlertaEcWeatherAdapter.phenomenonFromText("Rain advisory") shouldBe AlertaEcPhenomenon.RAIN
    }
}
