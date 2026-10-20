package org.breezyweather.ui.map

import java.net.URLEncoder
import java.time.Instant

internal object InamhiWmsTiles {
    fun url(layer: String, time: Instant): String {
        require(layer in setOf(
            InamhiLayers.PRECIPITATION_3H,
            InamhiLayers.PRECIPITATION_24H,
            InamhiLayers.TEMPERATURE_3H
        ))
        val params = listOf(
            "SERVICE=WMS",
            "VERSION=1.1.1",
            "REQUEST=GetMap",
            "LAYERS=" + URLEncoder.encode(layer, "UTF-8"),
            "STYLES=",
            "SRS=EPSG:3857",
            "BBOX={bbox-epsg-3857}",
            "WIDTH=256",
            "HEIGHT=256",
            "FORMAT=image/png",
            "TRANSPARENT=TRUE",
            "TIME=" + URLEncoder.encode(time.toString(), "UTF-8")
        )
        return InamhiLayers.WMS_URL + "?" + params.joinToString("&")
    }
}
