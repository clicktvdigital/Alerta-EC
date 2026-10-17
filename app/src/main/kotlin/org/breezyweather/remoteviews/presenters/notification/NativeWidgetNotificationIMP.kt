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

package org.breezyweather.remoteviews.presenters.notification

import org.breezyweather.common.options.appearance.CalendarHelper
import org.breezyweather.common.extensions.getFormattedMediumDayAndMonthInAdditionalCalendar
import java.util.Date
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import breezyweather.domain.location.model.Location
import org.breezyweather.common.extensions.notificationBuilder
import org.breezyweather.common.extensions.notify
import org.breezyweather.common.extensions.toBitmap
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.remoteviews.Notifications
import org.breezyweather.remoteviews.presenters.AbstractRemoteViewsPresenter
import org.breezyweather.ui.theme.resource.ResourceHelper
import org.breezyweather.ui.theme.resource.ResourcesProviderFactory

object NativeWidgetNotificationIMP : AbstractRemoteViewsPresenter() {
    fun buildNotificationAndSendIt(
        context: Context,
        location: Location,
        daytime: Boolean,
        tempIcon: Boolean,
        persistent: Boolean,
    ) {
        val current = location.weather?.current ?: return
        val provider = ResourcesProviderFactory.newInstance
        val settings = SettingsManager.getInstance(context)
        val temperatureUnit = settings.getTemperatureUnit(context)

        val tempFeelsLikeOrAir = if (settings.isWidgetNotificationUsingFeelsLike) {
            current.temperature?.feelsLikeTemperature ?: current.temperature?.temperature
        } else {
            current.temperature?.temperature
        }
        val temperature = if (tempIcon) tempFeelsLikeOrAir else null

        val summary = NotificationForecastSummary(context, location)

        val notification = context.notificationBuilder(Notifications.CHANNEL_WIDGET).apply {
            priority = NotificationCompat.PRIORITY_MAX
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            if (temperature != null) {
                setSmallIcon(
                    IconCompat.createWithBitmap(
                        ResourceHelper.createTempBitmap(context, temperature, temperatureUnit)
                    )
                )
            } else {
                setSmallIcon(
                    ResourceHelper.getDefaultMinimalXmlIconId(current.weatherCode, daytime)
                )
            }
            current.weatherCode?.let { weatherCode ->
                setLargeIcon(
                    ResourceHelper.getWidgetNotificationIcon(
                        provider,
                        weatherCode,
                        daytime,
                        minimal = false,
                        darkText = false
                    ).toBitmap()
                )
            }
            if (CalendarHelper.getAlternateCalendarSetting(context) != null) {
                setSubText(summary.interval + " · " + Date().getFormattedMediumDayAndMonthInAdditionalCalendar(location, context))
            } else {
                setSubText(summary.interval)
            }
            setContentTitle("${summary.phenomenon} · ${summary.place}")
            setContentText(summary.compact)
            setStyle(NotificationCompat.BigTextStyle().bigText(summary.details))
            setOngoing(persistent)
            setOnlyAlertOnce(true)
            setContentIntent(getWeatherPendingIntent(context, location, Notifications.ID_WIDGET))
        }.build()

        context.notify(Notifications.ID_WIDGET, notification)
    }
}
