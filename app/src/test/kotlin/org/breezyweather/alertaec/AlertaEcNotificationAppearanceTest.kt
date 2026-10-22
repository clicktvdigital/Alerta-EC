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
    fun soundsRequireSupportedPhenomenonAndSeverity() {
        AlertaEcNotificationAppearance.officialSoundChannel(AlertaEcPhenomenon.FLOOD, AlertSeverity.SEVERE) shouldBe AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE
        AlertaEcNotificationAppearance.officialSoundChannel(AlertaEcPhenomenon.RAIN, AlertSeverity.EXTREME) shouldBe AlertaEcNotificationAppearance.SOUND_CRITICAL
        AlertaEcNotificationAppearance.officialSoundChannel(AlertaEcPhenomenon.RAIN, AlertSeverity.MODERATE) shouldBe null
        AlertaEcNotificationAppearance.eventSoundChannel(AlertaEcPhenomenon.RAIN, AlertaEcSeverity.INFORMATION) shouldBe AlertaEcNotificationAppearance.SOUND_RAIN
        AlertaEcNotificationAppearance.soundResource("unknown") shouldBe null
    }

    @Test
    fun soundOffDoesNotSilenceEnabledVibration() {
        org.breezyweather.remoteviews.Notifications.shouldSetSilent(false, true) shouldBe false
        org.breezyweather.remoteviews.Notifications.shouldSetSilent(false, false) shouldBe true
        org.breezyweather.remoteviews.Notifications.shouldSetSilent(true, true, quietUpdate = true) shouldBe true
    }

    @Test
    fun selectedEmergencyChannelIsIndependentFromForecast() {
        org.breezyweather.remoteviews.Notifications.selectChannel(true, true, AlertaEcNotificationAppearance.SOUND_CRITICAL, "forecast") shouldBe AlertaEcNotificationAppearance.SOUND_CRITICAL
        org.breezyweather.remoteviews.Notifications.selectChannel(false, true, null, "forecast") shouldBe AlertaEcNotificationAppearance.VIBRATION_FORECAST
        org.breezyweather.remoteviews.Notifications.selectChannel(false, false, null, "forecast") shouldBe "forecast"
        org.breezyweather.remoteviews.Notifications.selectChannel(true, false, AlertaEcNotificationAppearance.SOUND_RAIN, "forecast") shouldBe AlertaEcNotificationAppearance.SOUND_RAIN
        org.breezyweather.remoteviews.Notifications.selectChannel(true, true, null, "forecast") shouldBe AlertaEcNotificationAppearance.VIBRATION_FORECAST

        org.breezyweather.remoteviews.Notifications.shouldPostToChannel(true, true, 0, false) shouldBe false
        org.breezyweather.remoteviews.Notifications.shouldPostToChannel(true, true, 4, false) shouldBe true
        org.breezyweather.remoteviews.Notifications.shouldPostToChannel(true, true, 4, true) shouldBe false
        org.breezyweather.remoteviews.Notifications.shouldPostToChannel(false, true, 4, false) shouldBe false
    }


    @Test
    fun allPreferenceCombinationsPreserveTheRequestedAlertBehavior() {
        val sound = AlertaEcNotificationAppearance.SOUND_CRITICAL
        val fallback = AlertaEcNotificationAppearance.EXTREME

        org.breezyweather.remoteviews.Notifications.selectChannel(true, true, sound, fallback) shouldBe sound
        org.breezyweather.remoteviews.Notifications.selectChannel(true, false, sound, fallback) shouldBe AlertaEcNotificationAppearance.SOUND_CRITICAL_NO_VIBRATION
        org.breezyweather.remoteviews.Notifications.selectChannel(false, true, sound, fallback) shouldBe AlertaEcNotificationAppearance.VIBRATION_EXTREME
        org.breezyweather.remoteviews.Notifications.selectChannel(false, false, sound, fallback) shouldBe AlertaEcNotificationAppearance.SILENT_EXTREME
    }

    @Test
    fun silentEmergencyStillHonorsTheLogicalExtremeChannelBlock() {
        val selected = org.breezyweather.remoteviews.Notifications.selectChannel(
            soundEnabled = false,
            vibrationEnabled = false,
            soundChannel = AlertaEcNotificationAppearance.SOUND_CRITICAL,
            fallbackChannel = AlertaEcNotificationAppearance.EXTREME,
        )

        selected shouldBe AlertaEcNotificationAppearance.SILENT_EXTREME
        org.breezyweather.remoteviews.Notifications.logicalCategory(selected) shouldBe AlertaEcNotificationAppearance.EXTREME
        org.breezyweather.remoteviews.Notifications.shouldPostToChannel(true, true, 4, channelBlocked = true) shouldBe false
    }

    @Test
    fun genericStormDoesNotInventLightning() {
        AlertaEcWeatherAdapter.phenomenonFromText("Tormenta tropical") shouldBe AlertaEcPhenomenon.OTHER
        AlertaEcWeatherAdapter.phenomenonFromText("Tormenta eléctrica con lluvia") shouldBe AlertaEcPhenomenon.THUNDERSTORM
        AlertaEcWeatherAdapter.phenomenonFromText("Strong wind warning") shouldBe AlertaEcPhenomenon.STRONG_WIND
        AlertaEcWeatherAdapter.phenomenonFromText("Rain advisory") shouldBe AlertaEcPhenomenon.RAIN
    }
}
