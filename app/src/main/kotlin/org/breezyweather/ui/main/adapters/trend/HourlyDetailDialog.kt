package org.breezyweather.ui.main.adapters.trend

import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.getContentDescription
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.breezyweather.R
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.common.extensions.formatPercent
import org.breezyweather.common.extensions.getHour
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.unit.formatting.UnitWidth

/**
 * Detail panel for one forecast hour, opened with OK/Enter on Android TV.
 * Shows every value the hour has (temperature, feels-like, precipitation, humidity, wind,
 * pressure and air quality). Values the source did not send are left out, not shown as zero.
 */
object HourlyDetailDialog {

    fun show(activity: BreezyActivity, location: Location, index: Int) {
        if (!activity.isActivityResumed) return
        val hourly = location.weather?.nextHourlyForecast?.getOrNull(index) ?: return
        val temperatureUnit = SettingsManager.getInstance(activity).getTemperatureUnit(activity)

        val lines = buildList {
            hourly.temperature?.temperature?.let {
                add(activity.getString(R.string.temperature) to it.formatMeasure(activity, temperatureUnit, unitWidth = UnitWidth.LONG))
            }
            hourly.temperature?.feelsLikeTemperature?.let {
                add(activity.getString(R.string.tag_feels_like) to it.formatMeasure(activity, temperatureUnit, unitWidth = UnitWidth.LONG))
            }
            hourly.precipitation?.total?.let {
                add(activity.getString(R.string.precipitation) to it.formatMeasure(activity, unitWidth = UnitWidth.LONG))
            }
            hourly.relativeHumidity?.let {
                add(activity.getString(R.string.humidity) to it.formatPercent(activity, UnitWidth.NARROW))
            }
            hourly.wind?.takeIf { it.isValid }?.let {
                add(activity.getString(R.string.wind) to it.getContentDescription(activity, withGusts = true))
            }
            hourly.pressure?.let {
                add(activity.getString(R.string.pressure) to it.formatMeasure(activity, unitWidth = UnitWidth.LONG))
            }
            hourly.airQuality?.let {
                add(activity.getString(R.string.air_quality) to it.getName(activity))
            }
        }

        val message = if (lines.isEmpty()) {
            activity.getString(R.string.alertaec_hourly_unknown)
        } else {
            lines.joinToString("\n") { (label, value) -> "$label: $value" }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(hourly.date.getHour(location, activity))
            .setMessage(message)
            .setPositiveButton(R.string.action_close, null)
            .show()
    }
}
