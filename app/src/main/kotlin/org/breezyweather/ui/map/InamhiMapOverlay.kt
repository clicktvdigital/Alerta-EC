package org.breezyweather.ui.map

import java.time.Instant
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.rasterOpacity
import org.maplibre.android.style.layers.PropertyFactory.visibility
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet

/** Instancia exclusiva de un estilo; nunca comparte fechas entre mapas o vistas. */
internal class InamhiMapOverlay(private val style: Style) {
    private data class Entry(val source: String, val layer: String, val time: Instant)
    private val active = mutableMapOf<String, Entry>()
    private val owned = mutableMapOf<String, MutableList<Entry>>()
    private var sequence = 0L

    fun forecastTime(layer: String): Instant? = active[layer]?.time

    private fun remove(entry: Entry) {
        val overlay = style.getLayer(entry.layer)
        overlay?.setProperties(visibility(Property.NONE))
        if (overlay != null) check(style.removeLayer(entry.layer))
        if (style.getSource(entry.source) != null) check(style.removeSource(entry.source))
    }

    fun clear(layer: String) {
        require(layer in InamhiLayers.all)
        val entries = owned[layer] ?: return
        var failure: Exception? = null
        entries.toList().forEach { entry ->
            try {
                remove(entry)
                entries.remove(entry)
                if (active[layer] == entry) active.remove(layer)
            } catch (error: Exception) {
                failure = error
            }
        }
        failure?.let { throw it }
        active.remove(layer)
    }

    fun discardExpired(layer: String, now: Instant) {
        val entries = owned[layer] ?: return
        entries.toList().filter { it.time.isBefore(now) }.forEach { entry ->
            remove(entry)
            entries.remove(entry)
            if (active[layer] == entry) active.remove(layer)
        }
    }

    fun apply(layer: String, time: Instant?) {
        require(layer in InamhiLayers.all)
        if (time == null) {
            clear(layer)
            return
        }
        if (active[layer]?.time == time) return
        val prefix = "alerta-ec-${layer.substringAfter(':')}-${++sequence}"
        val entry = Entry("$prefix-source", "$prefix-layer", time)
        val entries = owned.getOrPut(layer) { mutableListOf() }
        entries.add(entry)
        try {
            val tiles = TileSet("2.2.0", InamhiWmsTiles.url(layer, time))
            style.addSource(RasterSource(entry.source, tiles, 256))
            val opacity = if (layer == InamhiLayers.PRECIPITATION_3H) 0.15f else 0.10f
            val overlay = RasterLayer(entry.layer, entry.source).withProperties(rasterOpacity(opacity))
            if (style.getLayer("waterway_line_label") != null) {
                style.addLayerBelow(overlay, "waterway_line_label")
            } else {
                style.addLayer(overlay)
            }
        } catch (error: Exception) {
            try {
                remove(entry)
                entries.remove(entry)
            } catch (cleanup: Exception) {
                error.addSuppressed(cleanup)
            }
            throw error
        }
        active[layer] = entry
        // La sustitución solo retira la anterior después de crear la nueva.
        entries.toList().filter { it != entry }.forEach { previous ->
            remove(previous)
            entries.remove(previous)
        }
    }
}
