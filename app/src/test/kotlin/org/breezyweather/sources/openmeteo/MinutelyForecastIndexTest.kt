package org.breezyweather.sources.openmeteo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MinutelyForecastIndexTest {
    @Test
    fun `returns first interval at or after cutoff`() {
        findMinutelyForecastStartIndex(longArrayOf(100, 200, 300), 200) shouldBe 1
    }

    @Test
    fun `returns null when no interval can support a forecast`() {
        findMinutelyForecastStartIndex(longArrayOf(100, 200), 300) shouldBe null
        findMinutelyForecastStartIndex(longArrayOf(), 100) shouldBe null
    }
}
