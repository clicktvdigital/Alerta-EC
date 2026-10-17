package org.breezyweather.remoteviews.presenters.notification

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NotificationWeatherTextTest {
    @Test
    fun missingConditionsCannotLeaveAWaitingPlaceholder() {
        listOf(null, "", "  ", "\n").forEach {
            NotificationWeatherText.nonBlank(it) shouldBe null
        }
        NotificationWeatherText.nonBlank(" Cubierto ") shouldBe "Cubierto"
        NotificationWeatherText.nonBlank("Cloudy") shouldBe "Cloudy"
    }

    @Test
    fun uvZeroRemainsValidButMissingAndInvalidIndicesAreOmitted() {
        listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            NotificationWeatherText.validUv(it) shouldBe false
        }
        listOf(0.0, 0.5, 12.0).forEach {
            NotificationWeatherText.validUv(it) shouldBe true
        }
    }
}
