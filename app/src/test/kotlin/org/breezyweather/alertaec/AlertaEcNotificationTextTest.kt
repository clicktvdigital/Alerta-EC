package org.breezyweather.alertaec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.TimeZone

class AlertaEcNotificationTextTest {
    private val zone = TimeZone.getTimeZone("America/Guayaquil")
    private val now = 1_800_000_000_000L

    @Test
    fun hourlyPeriodStatesBlockAndNotExactOnset() {
        val period = AlertaEcNotificationText.hourlyPeriod(now, now, zone)
        period shouldBe "Bloque horario desde 03:00 (no indica el minuto exacto)"
    }

    @Test
    fun hourlyValuesShowMissingAndRejectInvalidValues() {
        AlertaEcNotificationText.probabilityPercent(75.0) shouldBe "Prob. 75%"
        AlertaEcNotificationText.accumulationMm(1.25) shouldBe "Acum. 1.3 mm"
        AlertaEcNotificationText.probabilityPercent(null) shouldBe "Prob. —"
        AlertaEcNotificationText.probabilityPercent(Double.NaN) shouldBe "Prob. —"
        AlertaEcNotificationText.probabilityPercent(101.0) shouldBe "Prob. —"
        AlertaEcNotificationText.accumulationMm(null) shouldBe "Acum. —"
        AlertaEcNotificationText.accumulationMm(-1.0) shouldBe "Acum. —"
    }

    @Test
    fun pastStartIsNeverPresentedAsFuture() {
        AlertaEcNotificationText.interval(now - 60_000, now, zone).startsWith("Desde ") shouldBe true
        AlertaEcNotificationText.interval(now, now, zone).startsWith("Desde ") shouldBe true
        AlertaEcNotificationText.interval(now - 3_600_000, now, zone)
            .startsWith("Intervalo pasado ") shouldBe true
        AlertaEcNotificationText.interval(now + 60_000, now, zone).startsWith("Prev. ") shouldBe true
    }
}
