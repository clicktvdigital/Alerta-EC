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

package org.breezyweather.ui.settings.compose

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import org.breezyweather.R
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.extensions.plus
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.common.widgets.Material3Scaffold
import org.breezyweather.ui.common.widgets.generateCollapsedScrollBehavior
import org.breezyweather.ui.common.widgets.insets.FitStatusBarTopAppBar
import org.breezyweather.ui.settings.preference.composables.ListPreferenceViewWithCard
import org.breezyweather.ui.settings.preference.composables.PreferenceScreen
import org.breezyweather.ui.settings.preference.listPreferenceItem
import org.breezyweather.ui.settings.preference.smallSeparatorItem
import org.breezyweather.unit.distance.DistanceUnit
import org.breezyweather.unit.formatting.UnitWidth
import org.breezyweather.unit.precipitation.PrecipitationUnit
import org.breezyweather.unit.pressure.PressureUnit
import org.breezyweather.unit.speed.SpeedUnit
import org.breezyweather.unit.temperature.TemperatureUnit

@Composable
fun UnitSettingsScreen(
    context: Context,
    onNavigateBack: () -> Unit,
    updateWidgetIfNecessary: (Context) -> Unit,
    updateNotificationIfNecessary: (Context) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = generateCollapsedScrollBehavior()
    val useMeasureFormat = SettingsManager.getInstance(context).useMeasureFormat

    Material3Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FitStatusBarTopAppBar(
                title = stringResource(R.string.settings_units),
                onBackPressed = onNavigateBack,
                actions = { AboutActivityIconButton(context) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddings ->
        PreferenceScreen(
            paddingValues = paddings.plus(PaddingValues(horizontal = dimensionResource(R.dimen.normal_margin)))
        ) {
            listPreferenceItem(R.string.settings_units_temperature) { id ->
                val allowedTemperatureUnits = TemperatureUnit.entries.filter { it != TemperatureUnit.DECI_CELSIUS }
                val valueArray = arrayOf("auto") + allowedTemperatureUnits.map { it.id }
                val nameArray = arrayOf(
                    stringResource(
                        R.string.parenthesis,
                        "Predeterminado Ecuador",
                        SettingsManager.getInstance(context).getTemperatureUnit(context)
                            .getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                    )
                ) + allowedTemperatureUnits.map {
                    it.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                }
                ListPreferenceViewWithCard(
                    title = stringResource(id),
                    summary = { _, value ->
                        (valueArray.indexOfFirst { it == value }.let { if (it == -1) nameArray[0] else nameArray[it] }) + "\n" + "Grados Celsius (°C): 0 °C congela el agua; Quito suele estar entre 10 y 22 °C, la Costa entre 24 y 32 °C."
                    },
                    selectedKey = SettingsManager.getInstance(context).temperatureUnit?.id ?: "auto",
                    valueArray = valueArray,
                    nameArray = nameArray,
                    isFirst = true,
                    onValueChanged = { temperatureUnitId ->
                        SettingsManager.getInstance(context).temperatureUnit = if (temperatureUnitId != "auto") {
                            TemperatureUnit.getUnit(temperatureUnitId)
                        } else {
                            null
                        }

                        // Widgets and notification-widget may use units, update them
                        updateWidgetIfNecessary(context)
                        updateNotificationIfNecessary(context)
                    }
                )
            }
            smallSeparatorItem()
            listPreferenceItem(R.string.settings_units_precipitation) { id ->
                val allowedPrecipitationUnits = PrecipitationUnit.entries.filter { it != PrecipitationUnit.MICROMETER }
                val valueArray = arrayOf("auto") + allowedPrecipitationUnits.map { it.id }
                val defaultUnit = SettingsManager.getInstance(context).getPrecipitationUnit(context)
                val snowfallUnit = SettingsManager.getInstance(context).getSnowfallUnit(context)
                val nameArray = arrayOf(
                    stringResource(
                        R.string.parenthesis,
                        "Predeterminado Ecuador",
                        if (defaultUnit != snowfallUnit) {
                            defaultUnit.getDisplayName(
                                context,
                                context.currentLocale,
                                UnitWidth.LONG,
                                useMeasureFormat
                            ) +
                                "/" +
                                snowfallUnit.getDisplayName(
                                    context,
                                    context.currentLocale,
                                    UnitWidth.LONG,
                                    useMeasureFormat
                                )
                        } else {
                            defaultUnit.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                        }
                    )
                ) + allowedPrecipitationUnits.map {
                    it.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                }
                ListPreferenceViewWithCard(
                    title = stringResource(id),
                    summary = { _, value ->
                        (valueArray.indexOfFirst { it == value }.let { if (it == -1) nameArray[0] else nameArray[it] }) + "\n" + "Milímetros (mm): 1 mm es 1 litro de agua por metro cuadrado. 2 mm/h es lluvia ligera; más de 10 mm/h, fuerte."
                    },
                    selectedKey = SettingsManager.getInstance(context).precipitationUnit?.id ?: "auto",
                    valueArray = valueArray,
                    nameArray = nameArray,
                    onValueChanged = { precipitationUnitId ->
                        SettingsManager.getInstance(context).precipitationUnit = if (precipitationUnitId != "auto") {
                            PrecipitationUnit.getUnit(precipitationUnitId)
                        } else {
                            null
                        }

                        // Widgets and notification-widget may use units, update them
                        updateWidgetIfNecessary(context)
                        updateNotificationIfNecessary(context)
                    }
                )
            }
            smallSeparatorItem()
            listPreferenceItem(R.string.settings_units_speed) { id ->
                val allowedSpeedUnits = SpeedUnit.entries.filter { it != SpeedUnit.CENTIMETER_PER_SECOND }
                val valueArray = arrayOf("auto") + allowedSpeedUnits.map { it.id }
                val nameArray = arrayOf(
                    stringResource(
                        R.string.parenthesis,
                        "Predeterminado Ecuador",
                        SettingsManager.getInstance(context).getSpeedUnit(context)
                            .getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                    )
                ) + allowedSpeedUnits.map {
                    it.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                }
                ListPreferenceViewWithCard(
                    title = stringResource(id),
                    summary = { _, value ->
                        (valueArray.indexOfFirst { it == value }.let { if (it == -1) nameArray[0] else nameArray[it] }) + "\n" + "Kilómetros por hora (km/h): 20 km/h mueve ramas; desde 50 km/h es viento fuerte, cuida techos y árboles."
                    },
                    selectedKey = SettingsManager.getInstance(context).speedUnit?.id ?: "auto",
                    valueArray = valueArray,
                    nameArray = nameArray,
                    onValueChanged = { speedUnitId ->
                        SettingsManager.getInstance(context).speedUnit = if (speedUnitId != "auto") {
                            SpeedUnit.getUnit(speedUnitId)
                        } else {
                            null
                        }

                        // Widgets and notification-widget may use units, update them
                        updateWidgetIfNecessary(context)
                        updateNotificationIfNecessary(context)
                    }
                )
            }
            smallSeparatorItem()
            listPreferenceItem(R.string.settings_units_distance) { id ->
                val allowedDistanceUnits = DistanceUnit.entries
                val valueArray = arrayOf("auto") + allowedDistanceUnits.map { it.id }
                val nameArray = arrayOf(
                    stringResource(
                        R.string.parenthesis,
                        "Predeterminado Ecuador",
                        SettingsManager.getInstance(context).getDistanceUnit(context)
                            .getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                    )
                ) + allowedDistanceUnits.map {
                    it.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                }
                ListPreferenceViewWithCard(
                    title = stringResource(id),
                    summary = { _, value ->
                        (valueArray.indexOfFirst { it == value }.let { if (it == -1) nameArray[0] else nameArray[it] }) + "\n" + "Kilómetros (km): la visibilidad con neblina puede bajar de 1 km; con cielo despejado supera los 10 km."
                    },
                    selectedKey = SettingsManager.getInstance(context).distanceUnit?.id ?: "auto",
                    valueArray = valueArray,
                    nameArray = nameArray,
                    onValueChanged = { distanceUnitId ->
                        SettingsManager.getInstance(context).distanceUnit = if (distanceUnitId != "auto") {
                            DistanceUnit.getUnit(distanceUnitId)
                        } else {
                            null
                        }

                        // Widgets and notification-widget may use units, update them
                        updateWidgetIfNecessary(context)
                        updateNotificationIfNecessary(context)
                    }
                )
            }
            smallSeparatorItem()
            listPreferenceItem(R.string.settings_units_pressure) { id ->
                val allowedPressureUnits = PressureUnit.entries.filter { it != PressureUnit.PASCAL }
                val valueArray = arrayOf("auto") + allowedPressureUnits.map { it.id }
                val nameArray = arrayOf(
                    stringResource(
                        R.string.parenthesis,
                        "Predeterminado Ecuador",
                        SettingsManager.getInstance(context).getPressureUnit(context)
                            .getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                    )
                ) + allowedPressureUnits.map {
                    it.getDisplayName(context, context.currentLocale, UnitWidth.LONG, useMeasureFormat)
                }
                ListPreferenceViewWithCard(
                    title = stringResource(id),
                    summary = { _, value ->
                        (valueArray.indexOfFirst { it == value }.let { if (it == -1) nameArray[0] else nameArray[it] }) + "\n" + "Hectopascales (hPa): igual a milibares. Al nivel del mar ~1013 hPa = 1 atm; en Quito ~730 hPa (0,72 atm). Si baja rápido, puede llegar mal tiempo."
                    },
                    selectedKey = SettingsManager.getInstance(context).pressureUnit?.id ?: "auto",
                    valueArray = valueArray,
                    nameArray = nameArray,
                    isLast = true,
                    onValueChanged = { pressureUnitId ->
                        SettingsManager.getInstance(context).pressureUnit = if (pressureUnitId != "auto") {
                            PressureUnit.getUnit(pressureUnitId)
                        } else {
                            null
                        }

                        // Widgets and notification-widget may use units, update them
                        updateWidgetIfNecessary(context)
                        updateNotificationIfNecessary(context)
                    }
                )
            }
        }
    }
}
