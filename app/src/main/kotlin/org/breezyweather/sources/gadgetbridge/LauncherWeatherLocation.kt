package org.breezyweather.sources.gadgetbridge

import android.content.Context
import breezyweather.domain.location.model.Location

internal object LauncherWeatherLocation {
    private const val PREFS = "alerta_ec_launcher_weather"
    private const val KEY = "preferred_location_id"

    fun preferredId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)

    fun select(context: Context, location: Location) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, location.formattedId)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
