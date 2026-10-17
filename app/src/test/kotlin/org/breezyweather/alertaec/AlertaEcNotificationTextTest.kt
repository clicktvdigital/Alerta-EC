package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.TimeZone

class AlertaEcNotificationTextTest {
    private val zone = TimeZone.getTimeZone("America/Guayaquil")
    private val now = 1_800_000_000_000L

    @Test
    fun pastStartIsNeverPresentedAsFuture() {
        AlertaEcNotificationText.interval(now - 60_000, now, zone).startsWith("Desde ") shouldBe true
        AlertaEcNotificationText.interval(now, now, zone).startsWith("Desde ") shouldBe true
        AlertaEcNotificationText.interval(now - 3_600_000, now, zone)
            .startsWith("Intervalo pasado ") shouldBe true
        AlertaEcNotificationText.interval(now + 60_000, now, zone).startsWith("Prev. ") shouldBe true
    }
}
