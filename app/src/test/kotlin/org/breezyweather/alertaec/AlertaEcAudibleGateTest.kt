package org.breezyweather.alertaec

import breezyweather.domain.weather.reference.WeatherCode
import io.kotest.matchers.shouldBe
import org.breezyweather.background.updater.interactor.GetApplicationRelease
import org.junit.jupiter.api.Test

class AlertaEcAudibleGateTest {
    @Test
    fun onlyOneSoundPerUpdateCycle() {
        val now = 10_000_000L
        AlertaEcAudibleGate.canClaim(0L, now, priority = false) shouldBe true
        AlertaEcAudibleGate.canClaim(now - 5_000L, now, priority = false) shouldBe false
        AlertaEcAudibleGate.canClaim(now - AlertaEcAudibleGate.CYCLE_WINDOW_MILLIS, now, priority = false) shouldBe true
        // Extreme official alerts always sound.
        AlertaEcAudibleGate.canClaim(now - 5_000L, now, priority = true) shouldBe true
    }

    @Test
    fun realConditionMapsToItsSound() {
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.FOG) shouldBe "FOG"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.HAZE) shouldBe "FOG"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.RAIN) shouldBe "RAIN"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.THUNDERSTORM) shouldBe "THUNDERSTORM"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.WIND) shouldBe "STRONG_WIND"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.CLEAR) shouldBe "CLEAR"
        AlertaEcCurrentConditionVoice.conditionOf(WeatherCode.CLOUDY) shouldBe "NONE"
    }

    @Test
    fun updateCheckUsesVersionCodeAndNames() {
        GetApplicationRelease.isNewer("0.1.23", 125, "0.1.24", 126) shouldBe true
        GetApplicationRelease.isNewer("0.1.24", 126, "0.1.24", 126) shouldBe false
        GetApplicationRelease.isNewer("0.1.25", 127, "0.1.24", 126) shouldBe false
        // GitHub fallback (no published code): names only.
        GetApplicationRelease.isNewer("0.1.24", 126, "v0.1.22", null) shouldBe false
        GetApplicationRelease.isNewer("0.1.24-r5", 126, "v0.1.25", null) shouldBe true
        GetApplicationRelease.isNewer("0.1.24", 126, "v0.1.24", null) shouldBe false
        GetApplicationRelease.isNewer("0.1.9", 109, "0.1.10", null) shouldBe true
    }
}
