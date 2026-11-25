package org.breezyweather.ui.map

import android.content.Context

internal object InamhiMapPreferences {
    private const val NAME = "alerta_ec_inamhi_map"
    private const val KEY_RAIN_VISIBLE = "rain_visible"
    private const val KEY_RAIN_24H_VISIBLE = "rain_24h_visible"
    private const val KEY_TEMPERATURE_VISIBLE = "temperature_visible"
    private const val KEY_TEMPERATURE_1H_VISIBLE = "temperature_1h_visible"
    private const val KEY_WIND_VISIBLE = "wind_3h_visible"

    data class Selection(
        val rain3h: Boolean = true,
        val rain24h: Boolean = true,
        val temperature: Boolean = true,
        val temperature1h: Boolean = false,
        val wind3h: Boolean = false,
    ) {
        fun enabled(layer: String): Boolean = when (layer) {
            InamhiLayers.PRECIPITATION_3H -> rain3h
            InamhiLayers.PRECIPITATION_24H -> rain24h
            InamhiLayers.TEMPERATURE_3H -> temperature
            InamhiLayers.TEMPERATURE_1H -> temperature1h
            InamhiLayers.WIND_3H -> wind3h
            else -> throw IllegalArgumentException("Unsupported INAMHI layer: $layer")
        }
    }

    fun read(context: Context): Selection {
        val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val legacy = context.getSharedPreferences("alerta_ec_map_layers", Context.MODE_PRIVATE)
        if (legacy.contains("inamhi_rain_3h_enabled")) {
            // La elección del almacén actual tiene prioridad sobre la antigua.
            if (!prefs.contains(KEY_RAIN_VISIBLE)) {
                setRainVisible(context, legacy.getBoolean("inamhi_rain_3h_enabled", true))
            }
            legacy.edit().remove("inamhi_rain_3h_enabled").apply()
        }
        val prefs2 = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return Selection(
            isRainVisible(context),
            isRain24hVisible(context),
            isTemperatureVisible(context),
            // Extra layers start off so the map is not covered by five overlays at once.
            prefs2.getBoolean(KEY_TEMPERATURE_1H_VISIBLE, false),
            prefs2.getBoolean(KEY_WIND_VISIBLE, false),
        )
    }

    fun setEnabled(context: Context, layer: String, enabled: Boolean) {
        when (layer) {
            InamhiLayers.PRECIPITATION_3H -> setRainVisible(context, enabled)
            InamhiLayers.PRECIPITATION_24H -> setRain24hVisible(context, enabled)
            InamhiLayers.TEMPERATURE_3H -> setTemperatureVisible(context, enabled)
            InamhiLayers.TEMPERATURE_1H, InamhiLayers.WIND_3H -> context
                .getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(
                    if (layer == InamhiLayers.WIND_3H) KEY_WIND_VISIBLE else KEY_TEMPERATURE_1H_VISIBLE,
                    enabled
                ).apply()
            else -> throw IllegalArgumentException("Unsupported INAMHI layer: $layer")
        }
    }

    fun isRainVisible(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_RAIN_VISIBLE, true)

    fun setRainVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RAIN_VISIBLE, visible)
            .apply()
    }

    fun isRain24hVisible(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_RAIN_24H_VISIBLE, true)

    fun setRain24hVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RAIN_24H_VISIBLE, visible).apply()
    }

    fun isTemperatureVisible(context: Context): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_TEMPERATURE_VISIBLE, true)

    fun setTemperatureVisible(context: Context, visible: Boolean) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TEMPERATURE_VISIBLE, visible).apply()
    }
}
