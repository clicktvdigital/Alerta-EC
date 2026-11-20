/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.ui.main.adapters.main.holder

import kotlinx.coroutines.launch
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.reference.WeatherCode
import org.breezyweather.BreezyWeather
import org.breezyweather.R
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.domain.weather.model.getTemperatureRangeSummary
import org.breezyweather.ui.common.widgets.AnimatableIconView
import org.breezyweather.ui.common.widgets.NumberAnimTextView
import org.breezyweather.ui.main.widgets.TextRelativeClock
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import org.breezyweather.unit.formatting.UnitWidth
import org.breezyweather.unit.formatting.format
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class HeaderViewHolder(parent: ViewGroup) : AbstractMainViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.container_main_header, parent, false)
) {
    private val timezoneText: TextView = itemView.findViewById(R.id.container_main_header_timezone)
    private val refreshTimeText: TextRelativeClock = itemView.findViewById(R.id.refreshTimeText)
    private val mTemperatureContainer: RelativeLayout = itemView.findViewById(R.id.container_main_header_temperature)
    private val mTemperature: NumberAnimTextView = itemView.findViewById(R.id.container_main_header_temperature_value)
    private val mTemperatureUnitView: TextView = itemView.findViewById(R.id.container_main_header_temperature_unit)
    private val mWeatherText: TextView = itemView.findViewById(R.id.container_main_header_weather_condition_description)
    private val mSkyIcon: AnimatableIconView = itemView.findViewById(R.id.container_main_header_sky_icon)
    private val mFeelsLike: TextView = itemView.findViewById(R.id.container_main_header_feels_like)
    private val mTemperatureRange: TextView = itemView.findViewById(R.id.container_main_header_temperature_range)
    private var mTemperatureFrom = 0
    private var mTemperatureTo = 0

    @SuppressLint("SetTextI18n")
    override fun onBindView(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
    ) {
        super.onBindView(context, location, provider, listAnimationEnabled, itemAnimationEnabled)

        if (BreezyWeather.instance.debugMode) {
            timezoneText.visibility = View.VISIBLE
            timezoneText.text = arrayOf(if (location.countryCode == "EC") "Ecuador" else location.countryCode, when (location.timeZone.id) { "America/Guayaquil" -> "Hora continental (Quito) · UTC−5"; "Pacific/Galapagos" -> "Hora de Galápagos · UTC−6"; else -> location.timeZone.id }).joinToString(
                context.getString(R.string.dot_separator)
            )
        }

        location.weather?.base?.refreshTime?.let {
            refreshTimeText.visibility = View.VISIBLE
            refreshTimeText.setDate(it)
        } ?: run {
            refreshTimeText.visibility = View.GONE
        }

        val temperatureUnit = SettingsManager.getInstance(context).getTemperatureUnit(context)
        location.weather?.current?.let { current ->
            bindSkyIcon(context, location, provider, current.weatherCode, current.weatherText, itemAnimationEnabled)
            if (!current.weatherText.isNullOrEmpty()) {
                mWeatherText.visibility = View.VISIBLE
                mWeatherText.text = current.weatherText + precipitationModelNote(location, current.weatherCode)
                // Quito: replace the model's «is it raining now» with the nearest EPMAPS rain gauge, when available.
                val modelText = current.weatherText.orEmpty()
                val code = current.weatherCode
                gaugeJob?.cancel()
                gaugeJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    val reading = org.breezyweather.alertaec.observation.QuitoRainGauges
                        .lastHour(location.latitude, location.longitude) ?: return@launch
                    mWeatherText.text = gaugeText(modelText, code, reading)
                }
            } else {
                mWeatherText.visibility = View.GONE
            }
            current.temperature?.temperature?.let {
                mTemperatureContainer.visibility = View.VISIBLE
                mTemperatureContainer.contentDescription = it.formatMeasure(
                    context,
                    temperatureUnit,
                    unitWidth = UnitWidth.LONG
                )
                mTemperatureFrom = mTemperatureTo
                mTemperatureTo = it.toDouble(temperatureUnit).roundToInt()
                mTemperature.isAnimEnabled = itemAnimationEnabled
                mTemperature.duration = (abs(mTemperatureTo - mTemperatureFrom) * 20.0)
                    .coerceAtLeast(1000.0) // at least 1 second
                    .coerceAtMost(2000.0) // no longer than 2 seconds
                    .milliseconds
                mTemperatureUnitView.text = temperatureUnit.getNominativeUnit(context)
            } ?: run {
                mTemperatureContainer.visibility = View.GONE
            }
            current.temperature?.feelsLikeTemperature?.let { feelsLike ->
                if (current.temperature!!.temperature?.toDouble(temperatureUnit)?.roundToInt() !=
                    feelsLike.toDouble(temperatureUnit).roundToInt()
                ) {
                    mFeelsLike.visibility = View.VISIBLE
                    mFeelsLike.text = context.getString(
                        R.string.temperature_feels_like_with_unit,
                        feelsLike.formatMeasure(
                            context,
                            temperatureUnit,
                            valueWidth = UnitWidth.NARROW,
                            unitWidth = UnitWidth.NARROW
                        )
                    )
                    mFeelsLike.contentDescription = context.getString(R.string.temperature_feels_like) +
                        context.getString(R.string.colon_separator) +
                        feelsLike.formatMeasure(
                            context,
                            temperatureUnit,
                            unitWidth = UnitWidth.LONG
                        )
                } else {
                    mFeelsLike.visibility = View.GONE
                }
            } ?: run {
                mFeelsLike.visibility = View.GONE
            }
        }

        location.weather!!.getTemperatureRangeSummary(context, location, temperatureUnit)?.let {
            mTemperatureRange.visibility = View.VISIBLE
            mTemperatureRange.text = it.first
            mTemperatureRange.contentDescription = it.second
        } ?: run {
            mTemperatureRange.visibility = View.GONE
        }
    }

    override fun getEnterAnimator(pendingAnimatorList: List<Animator>): Animator {
        val a: Animator = ObjectAnimator.ofFloat(itemView, "alpha", 0f, 1f)
        a.duration = 300
        a.startDelay = 100
        a.interpolator = FastOutSlowInInterpolator()
        return a
    }

    @SuppressLint("DefaultLocale")
    override fun onEnterScreen() {
        super.onEnterScreen()
        mTemperature.setNumberString(
            mTemperatureFrom.format(decimals = 0, locale = context.currentLocale),
            mTemperatureTo.format(decimals = 0, locale = context.currentLocale)
        )
    }

    /**
     * Big sky icon for the current moment: day/night variant of the current condition (sun, sun with
     * clouds, moon, rain or storm clouds), so the time of day and the weather read at a glance on a TV.
     */
    private fun bindSkyIcon(
        context: Context,
        location: Location,
        provider: ResourceProvider,
        code: WeatherCode?,
        weatherText: String?,
        animate: Boolean,
    ) {
        if (code == null) {
            mSkyIcon.visibility = View.GONE
            return
        }
        val daylight = location.isDaylight
        mSkyIcon.visibility = View.VISIBLE
        mSkyIcon.setAnimatableIcon(
            provider.getWeatherIcons(code, daylight),
            provider.getWeatherAnimators(code, daylight)
        )
        mSkyIcon.contentDescription = listOfNotNull(
            context.getString(if (daylight) R.string.daytime else R.string.nighttime),
            weatherText?.takeIf { it.isNotEmpty() }
        ).joinToString(context.getString(R.string.dot_separator))
        if (animate) mSkyIcon.post { mSkyIcon.startAnimators() }
    }

    /**
     * «Llovizna ligera» comes from the forecast model for the whole grid cell, not from an observation at the point.
     * When the minute-by-minute forecast shows no precipitation right now (or there is none), say so instead of
     * presenting rain as confirmed at the user's place.
     */
    private fun precipitationModelNote(
        location: breezyweather.domain.location.model.Location,
        code: breezyweather.domain.weather.reference.WeatherCode?,
    ): String {
        val wet = code in setOf(
            breezyweather.domain.weather.reference.WeatherCode.RAIN,
            breezyweather.domain.weather.reference.WeatherCode.SLEET,
            breezyweather.domain.weather.reference.WeatherCode.SNOW,
            breezyweather.domain.weather.reference.WeatherCode.HAIL,
            breezyweather.domain.weather.reference.WeatherCode.THUNDERSTORM,
        )
        if (!wet) return ""
        val now = System.currentTimeMillis()
        val minute = location.weather?.minutelyForecast?.firstOrNull { now >= it.date.time && now < it.endingDate.time }
        val raining = (minute?.precipitationIntensity?.inMillimeters ?: 0.0) > 0.0
        return if (raining) "" else " · según el modelo"
    }

    private var gaugeJob: kotlinx.coroutines.Job? = null

    /**
     * Measured rain wins over the model for «now»: «Sin lluvia medida · P03 a 1,5 km (el modelo prevé chubascos
     * ligeros)» or «Lluvia medida: 0,8 mm en 2 h · P03 a 1,5 km».
     */
    private fun gaugeText(
        modelText: String,
        code: breezyweather.domain.weather.reference.WeatherCode?,
        reading: org.breezyweather.alertaec.observation.QuitoRainGauges.Reading,
    ): String {
        val where = "${reading.station.code} a " + String.format(java.util.Locale.forLanguageTag("es-EC"), "%.1f km", reading.distanceKm)
        val wet = code in setOf(
            breezyweather.domain.weather.reference.WeatherCode.RAIN,
            breezyweather.domain.weather.reference.WeatherCode.SLEET,
            breezyweather.domain.weather.reference.WeatherCode.HAIL,
            breezyweather.domain.weather.reference.WeatherCode.THUNDERSTORM,
        )
        return when {
            reading.lastHourMm > 0.0 -> "Lluvia medida: " +
                String.format(java.util.Locale.forLanguageTag("es-EC"), "%.1f", reading.lastHourMm) + " mm en ${org.breezyweather.alertaec.observation.QuitoRainGauges.WINDOW_HOURS} h · $where"
            wet -> "Sin lluvia medida · $where (el modelo prevé ${modelText.lowercase()})"
            else -> "$modelText · sin lluvia medida ($where)"
        }
    }
}
