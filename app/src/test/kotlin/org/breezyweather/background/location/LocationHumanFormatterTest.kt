package org.breezyweather.background.location

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LocationHumanFormatterTest {
    @Test
    fun `formats decimal and DMS coordinates with correct hemispheres`() {
        LocationHumanFormatter.decimalCoordinates(-0.5, -78.25) shouldBe "-0.500000, -78.250000"
        LocationHumanFormatter.dmsCoordinates(-0.5, -78.25) shouldBe
            "0° 30′ 00.00″ S · 78° 15′ 00.00″ O"
    }

    @Test
    fun `unknown accuracy and altitude are explicitly unavailable`() {
        LocationHumanFormatter.accuracy(null) shouldBe "No disponible"
        LocationHumanFormatter.altitude(null) shouldBe "No disponible"
        LocationHumanFormatter.accuracy(12f) shouldBe "±12 m"
        LocationHumanFormatter.altitude(1000.0) shouldBe "1000 m (altitud GPS, referencia WGS84)"
    }
}
