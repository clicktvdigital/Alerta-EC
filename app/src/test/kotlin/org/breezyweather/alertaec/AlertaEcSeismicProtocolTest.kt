package org.breezyweather.alertaec

import org.breezyweather.alertaec.AlertaEcSeismicProtocol.Phase
import org.breezyweather.alertaec.AlertaEcSeismicProtocol.SeismicReport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlertaEcSeismicProtocolTest {
    @Test
    fun distantSmallQuakeIsNotFelt() {
        val report = SeismicReport(magnitude = 3.0, distanceKm = 300.0, depthKm = 10.0)
        assertEquals(0, AlertaEcSeismicProtocol.feltLevel(report))
        // Only the official-information line remains.
        assertEquals(1, AlertaEcSeismicProtocol.advise(report).size)
    }

    @Test
    fun strongNearbyQuakeGivesDuringAndAfterSteps() {
        val report = SeismicReport(magnitude = 6.8, distanceKm = 20.0, depthKm = 15.0)
        assertEquals(3, AlertaEcSeismicProtocol.feltLevel(report))
        val advice = AlertaEcSeismicProtocol.advise(report)
        assertTrue(advice.any { it.phase == Phase.DURING && it.text.startsWith("Agáchate, cúbrete y sujétate") })
        assertTrue(advice.any { it.phase == Phase.AFTER && it.text.contains("réplicas") })
        assertTrue(advice.any { it.text.contains("estructuras con grietas") })
    }

    @Test
    fun deepQuakesAreFeltLessThanShallowOnes() {
        val shallow = SeismicReport(5.5, distanceKm = 60.0, depthKm = 10.0)
        val deep = SeismicReport(5.5, distanceKm = 60.0, depthKm = 200.0)
        assertTrue(AlertaEcSeismicProtocol.feltLevel(shallow) > AlertaEcSeismicProtocol.feltLevel(deep))
    }

    @Test
    fun tsunamiStepOnlyForStrongOffshoreQuakeOnTheCoastAndFirst() {
        val coast = SeismicReport(7.2, distanceKm = 80.0, depthKm = 20.0, offshore = true, userOnCoast = true)
        assertTrue(AlertaEcSeismicProtocol.advise(coast).first().text.contains("zonas altas"))
        val inland = coast.copy(userOnCoast = false)
        assertFalse(AlertaEcSeismicProtocol.advise(inland).any { it.text.contains("zonas altas") })
    }

    @Test
    fun preparationIsAlwaysAvailable() {
        assertTrue(AlertaEcSeismicProtocol.preparation.all { it.phase == Phase.BEFORE })
        assertTrue(AlertaEcSeismicProtocol.preparation.any { it.text.contains("mochila de emergencia") })
    }
}
