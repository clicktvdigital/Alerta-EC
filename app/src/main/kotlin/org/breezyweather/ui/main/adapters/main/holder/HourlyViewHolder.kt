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

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import breezyweather.domain.location.model.Location
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonGroup
import org.breezyweather.R
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.alertaec.AlertaEcRecommendations
import org.breezyweather.alertaec.AlertaEcWeatherAdapter
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.common.extensions.formatPercent
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.extensions.getThemeColor
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.unit.formatting.UnitWidth
import org.breezyweather.ui.main.adapters.main.HourlyForecastHighlights
import org.breezyweather.ui.main.adapters.main.HourlyHighlightState
import org.breezyweather.ui.common.adapters.ButtonAdapter
import org.breezyweather.ui.common.widgets.trend.TrendLayoutManager
import org.breezyweather.ui.common.widgets.trend.TrendRecyclerView
import org.breezyweather.ui.main.adapters.trend.HourlyTrendAdapter
import org.breezyweather.ui.main.widgets.TrendRecyclerViewScrollBar
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import org.breezyweather.ui.theme.weatherView.WeatherViewController
import java.text.DateFormat

class HourlyViewHolder(parent: ViewGroup) : AbstractMainCardViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.container_main_hourly_trend_card, parent, false)
) {
    private val subtitle: TextView = itemView.findViewById(R.id.hourly_block_subtitle)
    private val highlights: TextView = itemView.findViewById(R.id.hourly_block_highlights)
    private val buttonGroup: MaterialButtonGroup = itemView.findViewById(R.id.hourly_block_button_group)
    private val trendRecyclerView: TrendRecyclerView = itemView.findViewById(R.id.hourly_block_trendRecyclerView)
    private val scrollBar: TrendRecyclerViewScrollBar = TrendRecyclerViewScrollBar()

    init {
        trendRecyclerView.setHasFixedSize(true)
        trendRecyclerView.addItemDecoration(scrollBar)
    }

    override fun onBindView(
        activity: BreezyActivity,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
        selectedTab: String?,
        setSelectedTab: (String?) -> Unit,
    ) {
        super.onBindView(activity, location, provider, listAnimationEnabled, itemAnimationEnabled)

        val weather = location.weather ?: return

        if (weather.current?.hourlyForecast.isNullOrEmpty()) {
            subtitle.visibility = View.GONE
        } else {
            subtitle.visibility = View.VISIBLE
            subtitle.text = weather.current?.hourlyForecast
        }

        val settings = SettingsManager.getInstance(context)
        val temperatureUnit = settings.getTemperatureUnit(context)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, context.currentLocale).apply {
            timeZone = location.timeZone
        }
        val acquiredFormat = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            context.currentLocale
        ).apply { timeZone = location.timeZone }
        val highlightLines = HourlyForecastHighlights.describe(
            forecasts = weather.nextHourlyForecast,
            nowMillis = System.currentTimeMillis(),
            timeZone = location.timeZone
        ).map { highlight ->
            val forecast = highlight.forecast
            if (forecast == null || highlight.state == HourlyHighlightState.MISSING) {
                return@map context.getString(R.string.alertaec_hourly_missing, highlight.offsetHours)
            }
            val details = buildList {
                forecast.temperature?.temperature?.let {
                    add(it.formatMeasure(context, temperatureUnit, unitWidth = UnitWidth.NARROW))
                }
                (forecast.weatherText?.takeIf { it.isNotBlank() }
                    ?: forecast.weatherCode?.let { WeatherViewController.getWeatherText(it) })
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::add)
                forecast.precipitationProbability?.total?.let {
                    add(context.getString(R.string.alertaec_hourly_rain, it.formatPercent(context, UnitWidth.NARROW)))
                }
                forecast.wind?.speed?.let {
                    add(context.getString(R.string.alertaec_hourly_wind, it.formatMeasure(context, unitWidth = UnitWidth.NARROW)))
                }
            }
            val acquiredAt = forecast.acquiredAt
            val acquisitionText = when {
                acquiredAt == null -> context.getString(R.string.alertaec_hourly_unknown)
                highlight.state == HourlyHighlightState.RECENT -> context.getString(
                    R.string.alertaec_hourly_recent,
                    acquiredFormat.format(acquiredAt)
                )
                highlight.state == HourlyHighlightState.STALE -> context.getString(
                    R.string.alertaec_hourly_stale,
                    acquiredFormat.format(acquiredAt)
                )
                else -> context.getString(R.string.alertaec_hourly_unknown)
            }
            SpannableStringBuilder().apply {
                val heading = context.getString(
                    R.string.alertaec_hourly_highlight,
                    highlight.offsetHours,
                    timeFormat.format(forecast.date),
                    ""
                ).trim().trimEnd('·').trimEnd()
                append(heading)
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(
                    ForegroundColorSpan(context.getThemeColor(androidx.appcompat.R.attr.colorPrimary)),
                    0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (details.isNotEmpty()) append("\n").append(details.joinToString(" · "))
                append("\n")
                val acquisitionStart = length
                append(acquisitionText)
                setSpan(
                    ForegroundColorSpan(context.getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)),
                    acquisitionStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        val visibleRecommendation = AlertaEcWeatherAdapter.recommendationEventForDisplay(location)
            ?.let(AlertaEcRecommendations::forEvent)
        highlights.text = SpannableStringBuilder().apply {
            visibleRecommendation?.let {
                append(context.getString(R.string.alertaec_preventive_advice, it))
                if (highlightLines.isNotEmpty()) append("\n\n")
            }
            highlightLines.forEachIndexed { index, line ->
                if (index > 0) append("\n\n")
                append(line)
            }
        }
        highlights.visibility = View.VISIBLE

        // Keep missing-interval text available even when there is no series to chart.
        if (weather.nextHourlyForecast.isEmpty()) {
            trendRecyclerView.adapter = null
            trendRecyclerView.visibility = View.GONE
            buttonGroup.visibility = View.GONE
            return
        }
        trendRecyclerView.visibility = View.VISIBLE

        val trendAdapter = HourlyTrendAdapter(activity, trendRecyclerView).apply {
            bindData(location)
        }
        val buttonList: MutableList<ButtonAdapter.Button> = trendAdapter.adapters.map {
            object : ButtonAdapter.Button {
                override val name = it.getDisplayName(activity)
            }
        }.toMutableList()
        selectedTab?.let { tab ->
            buttonList.indexOfFirst { it.name == tab }.let {
                if (it >= 0) {
                    trendAdapter.selectedIndex = it
                } else {
                    setSelectedTab(null) // Reset
                }
            }
        }

        if (buttonList.size < 2) {
            buttonGroup.visibility = View.GONE
        } else {
            buttonGroup.visibility = View.VISIBLE
            // Dirty trick to get the button group to actually redraw with the correct styles AND the overflow menu
            while (
                buttonGroup.children
                    .filter { it is MaterialButton && it.tag != MaterialButtonGroup.OVERFLOW_BUTTON_TAG }
                    .count() != 0
            ) {
                buttonGroup.children
                    .filter { it is MaterialButton && it.tag != MaterialButtonGroup.OVERFLOW_BUTTON_TAG }
                    .forEach {
                        buttonGroup.removeView(it)
                    }
            }
            buttonGroup.children
                .filter { it is MaterialButton && it.tag == MaterialButtonGroup.OVERFLOW_BUTTON_TAG }
                .forEach {
                    it.contentDescription = context.getString(R.string.action_more)
                }
            buttonList.forEachIndexed { index, button ->
                buttonGroup.addView(
                    MaterialButton(
                        context,
                        null,
                        com.google.android.material.R.attr.materialButtonStyle
                    ).apply {
                        text = button.name
                        isCheckable = true
                        isChecked = index == trendAdapter.selectedIndex
                        setOnClickListener {
                            trendAdapter.selectedIndex = index
                            setSelectedTab(button.name)
                            buttonGroup.children
                                .filter { it is MaterialButton && it.tag != MaterialButtonGroup.OVERFLOW_BUTTON_TAG }
                                .forEach { button ->
                                    (button as MaterialButton).isChecked = false
                                }
                            isChecked = true
                        }
                    }
                )
            }
        }
        trendRecyclerView.layoutManager = TrendLayoutManager(context)
        trendRecyclerView.setLineColor(
            context.getThemeColor(com.google.android.material.R.attr.colorOutline)
        )
        trendRecyclerView.setTextColor(
            ContextCompat.getColor(
                context,
                if (ThemeManager.isLightTheme(context, location)) R.color.colorTextGrey else R.color.colorTextGrey2nd
            )
        )
        trendRecyclerView.adapter = trendAdapter
        trendRecyclerView.setKeyLineVisibility(
            SettingsManager.getInstance(context).isTrendHorizontalLinesEnabled
        )

        scrollBar.resetColor(activity)
    }
}
