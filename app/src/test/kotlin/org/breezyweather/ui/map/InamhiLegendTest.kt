package org.breezyweather.ui.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InamhiLegendTest {
    // Shape of INAMHI's real GetLegendGraphic JSON (wrf_3h_precipitacion), first steps only.
    private val precipitation = """
        {"Legend": [{"layerName": "wrf_3h_precipitacion", "title": "Pronóstico WRF 3h - Precipitación",
        "rules": [{"symbolizers": [{"Raster": {"colormap": {"entries": [
          {"quantity": "5", "color": "#FFFF73"},
          {"quantity": "0", "color": "#FFFFFF"},
          {"quantity": "1", "color": "#FFFFBE"},
          {"quantity": "40", "color": "#216E9E"}
        ]}}}]}]}]}
    """.trimIndent()

    @Test
    fun parsesOfficialColourMapSortedByValue() {
        val steps = InamhiLegend.parse(precipitation)!!
        assertEquals(listOf(0.0, 1.0, 5.0, 40.0), steps.map { it.quantity })
        assertEquals(0xFF216E9E.toInt(), steps.last().color)
    }

    @Test
    fun malformedLegendGivesNull() {
        assertNull(InamhiLegend.parse("""{"Legend": []}"""))
        assertNull(InamhiLegend.parse("<html/>"))
    }

    @Test
    fun hexColoursAndContrast() {
        assertEquals(0xFFFFFF73.toInt(), InamhiLegend.parseHexColor("#FFFF73"))
        assertNull(InamhiLegend.parseHexColor("#FFF"))
        // Light yellow gets dark text, dark blue gets light text.
        assertTrue(InamhiLegend.luminance(InamhiLegend.parseHexColor("#FFFF73")!!) > 0.5)
        assertTrue(InamhiLegend.luminance(InamhiLegend.parseHexColor("#204B91")!!) < 0.5)
    }

    @Test
    fun unitsPerLayer() {
        assertEquals("mm", InamhiLegend.unit(InamhiLayers.PRECIPITATION_3H))
        assertEquals("°C", InamhiLegend.unit(InamhiLayers.TEMPERATURE_3H))
    }
}
