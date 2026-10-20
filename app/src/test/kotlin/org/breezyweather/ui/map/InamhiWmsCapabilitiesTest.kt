package org.breezyweather.ui.map

import java.time.Instant
import java.util.concurrent.CancellationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Documentos XML reales, analizados con el mismo SAX que producción. */
class InamhiWmsCapabilitiesTest {
    private val time = "2026-10-18T12:00:00Z"
    private val rain = InamhiLayers.PRECIPITATION_3H
    private val temperature = InamhiLayers.TEMPERATURE_3H

    private fun layer(name: String, dimension: String = "") =
        "<Layer><Name>$name</Name>$dimension</Layer>"
    private fun dimension(value: String) = "<Dimension name=\"time\">$value</Dimension>"
    private fun parse(body: String) = InamhiWmsTimes.parseCapabilities(
        "<WMS_Capabilities xmlns=\"http://www.opengis.net/wms\"><Capability>$body</Capability></WMS_Capabilities>"
            .byteInputStream()
    )

    @Test
    fun inheritsTimeWithoutLeakingSiblingOrStyleName() {
        val result = parse("<Layer>${dimension(time)}" +
            "<Layer><Name>$rain</Name><Style><Name>$temperature</Name></Style></Layer>" +
            layer(InamhiLayers.PRECIPITATION_24H, dimension("2026-10-18T15:00:00Z")) +
            layer(temperature) + "</Layer>")
        assertEquals(listOf(Instant.parse(time)), result.times[rain])
        assertEquals(listOf(Instant.parse(time)), result.times[temperature])
        assertEquals(listOf(Instant.parse("2026-10-18T15:00:00Z")),
            result.times[InamhiLayers.PRECIPITATION_24H])
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun malformedDimensionOnlyFailsItsLayer() {
        val result = parse(layer(rain, dimension("current")) + layer(temperature, dimension(time)))
        assertEquals(setOf(rain), result.errors)
        assertFalse(result.times.containsKey(rain))
        assertEquals(listOf(Instant.parse(time)), result.times[temperature])
    }

    @Test
    fun duplicateIsAnErrorForThatLayerEvenWhenThirdCopyIsValid() {
        val result = parse(layer(rain) + layer(rain) + layer(rain, dimension(time)) +
            layer(temperature, dimension(time)))
        assertEquals(setOf(rain), result.errors)
        assertFalse(result.times.containsKey(rain))
        assertTrue(result.times.containsKey(temperature))
    }

    @Test
    fun acceptsLegacyExtentCdataAndMissingDates() {
        val result = InamhiWmsTimes.parseCapabilities((
            "<WMT_MS_Capabilities><Layer><Extent name=\"time\"><![CDATA[$time]]></Extent>" +
                "<Name>$rain</Name></Layer>${layer(temperature)}</WMT_MS_Capabilities>"
            ).byteInputStream())
        assertEquals(listOf(Instant.parse(time)), result.times[rain])
        assertEquals(emptyList<Instant>(), result.times[temperature])
    }

    @Test
    fun rejectsMalformedXmlServiceErrorsDtdAndEntities() {
        listOf(
            "<WMS_Capabilities><Layer>",
            "<ServiceExceptionReport><ServiceException>error</ServiceException></ServiceExceptionReport>",
            "<!DOCTYPE WMS_Capabilities [<!ENTITY x 'value'>]><WMS_Capabilities>&x;</WMS_Capabilities>",
            "<!DOCTYPE WMS_Capabilities SYSTEM 'file:///must-not-be-read'><WMS_Capabilities/>"
        ).forEach { xml ->
            assertThrows(Exception::class.java) { InamhiWmsTimes.parseCapabilities(xml.byteInputStream()) }
        }
    }

    @Test
    fun parserPropagatesCancellation() {
        assertThrows(CancellationException::class.java) {
            InamhiWmsTimes.parseCapabilities("<WMS_Capabilities/>".byteInputStream()) {
                throw CancellationException()
            }
        }
    }

    @Test
    fun byteLimitAppliesWithoutContentLength() {
        assertEquals(InamhiWmsTimes.MAX_BYTES, InamhiWmsTimes.readLimited(
            ByteArray(InamhiWmsTimes.MAX_BYTES).inputStream()).size)
        assertThrows(IllegalArgumentException::class.java) {
            InamhiWmsTimes.readLimited(ByteArray(InamhiWmsTimes.MAX_BYTES + 1).inputStream())
        }
        assertThrows(CancellationException::class.java) {
            InamhiWmsTimes.readLimited(byteArrayOf(1).inputStream()) { throw CancellationException() }
        }
    }
}
