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

package org.breezyweather.remoteviews

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_MIN
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Weather
import breezyweather.domain.weather.reference.AlertSeverity
import org.breezyweather.alertaec.AlertaEcNotificationAppearance
import org.breezyweather.alertaec.AlertaEcRainEpisodeEvaluator
import org.breezyweather.alertaec.AlertaEcWeatherAdapter
import org.breezyweather.R
import org.breezyweather.common.extensions.buildNotificationChannel
import org.breezyweather.common.extensions.buildNotificationChannelGroup
import org.breezyweather.common.extensions.notificationBuilder
import org.breezyweather.common.extensions.cancelNotification
import org.breezyweather.common.extensions.notify
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.domain.settings.ConfigStore
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.domain.weather.model.getMinutelyDescription
import org.breezyweather.domain.weather.model.getMinutelyTitle
import org.breezyweather.domain.weather.model.hasMinutelyPrecipitation
import java.security.MessageDigest
import java.text.DateFormat
import java.nio.charset.StandardCharsets

object Notifications {

    // We only have one group as we don’t have many channels
    // We name it “Breezy Weather” as LeakCanary also has its own group
    private const val GROUP_BREEZY_WEATHER = "group_breezy_weather"

    private const val CHANNEL_ALERT = "alert"
    private const val ID_ALERT_MIN = 1000
    private const val ID_ALERT_MAX = 1999
    private const val ID_ALERT_GROUP = 2000
    private const val ID_PRECIPITATION = 3000

    const val CHANNEL_FORECAST = "forecast"
    const val ID_TODAY_FORECAST = 2
    const val ID_TOMORROW_FORECAST = 3
    const val ID_UPDATING_TODAY_FORECAST = 7
    const val ID_UPDATING_TOMORROW_FORECAST = 8

    const val CHANNEL_WIDGET = "widget"
    const val ID_WIDGET = 1
    const val ID_UPDATING_WIDGET = 6

    const val CHANNEL_BACKGROUND = "background"
    const val CHANNEL_ACTIVE_LOCATION = "active_location"
    const val ID_RUNNING_IN_BACKGROUND = 5
    const val ID_UPDATING_AWAKE = 9
    const val ID_WEATHER_PROGRESS = -101
    const val ID_WEATHER_ERROR = -102

    const val CHANNEL_APP_UPDATE = "app_apk_update_channel"
    const val ID_APP_UPDATER = 11
    const val ID_APP_UPDATE_PROMPT = 12

    /**
     * Notification channel used for crash log file sharing.
     */
    const val CHANNEL_CRASH_LOGS = "crash_logs"
    const val ID_CRASH_LOGS = -201

    private const val ALERT_GROUP_KEY = "breezy_weather_alert_notification_group"
    private const val PREFERENCE_NOTIFICATION = "NOTIFICATION_PREFERENCE"
    private const val KEY_NOTIFICATION_ID = "NOTIFICATION_ID"
    // private const val PREFERENCE_SHORT_TERM_PRECIPITATION_ALERT = "SHORT_TERM_PRECIPITATION_ALERT_PREFERENCE"
    // private const val KEY_PRECIPITATION_LOCATION_KEY = "PRECIPITATION_LOCATION_KEY"
    // private const val KEY_PRECIPITATION_DATE = "PRECIPITATION_DATE"

    /**
     * Initialize channels so that the user can disable them, even if didn’t receive a notification yet
     */
    fun createChannels(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(
                buildNotificationChannelGroup(GROUP_BREEZY_WEATHER) {
                    setName(context.getString(R.string.brand_name))
                }
            )
        )


        // Keep existing installations on their original channel: no preference bypass.
        if (notificationManager.getNotificationChannel(CHANNEL_ALERT) == null) {
            notificationManager.createNotificationChannelsCompat(
                listOf(
                    Triple(
                        AlertaEcNotificationAppearance.PRECIPITATION,
                        R.string.notification_channel_precipitation,
                        longArrayOf(0, 180)
                    ),
                    Triple(
                        AlertaEcNotificationAppearance.SEVERE,
                        R.string.notification_channel_severe,
                        longArrayOf(0, 350, 180, 350)
                    ),
                    Triple(
                        AlertaEcNotificationAppearance.EXTREME,
                        R.string.notification_channel_extreme,
                        longArrayOf(0, 500, 200, 500, 200, 500)
                    )
                ).map { (id, name, pattern) ->
                    val importance = if (id == AlertaEcNotificationAppearance.PRECIPITATION) {
                        IMPORTANCE_DEFAULT
                    } else {
                        IMPORTANCE_HIGH
                    }
                    buildNotificationChannel(id, importance) {
                        setName(context.getString(name))
                        setGroup(GROUP_BREEZY_WEATHER)
                        setVibrationEnabled(true)
                        setVibrationPattern(pattern)
                    }
                }
            )
        }


        notificationManager.createNotificationChannelsCompat(
            listOf(
                buildNotificationChannel(AlertaEcNotificationAppearance.PRECIPITATION_IMMINENT, IMPORTANCE_DEFAULT) {
                    setName(context.getString(R.string.notification_channel_precipitation))
                    setVibrationEnabled(true)
                    setVibrationPattern(longArrayOf(0, 180))
                    setGroup(GROUP_BREEZY_WEATHER)
                },
                buildNotificationChannel(CHANNEL_ALERT, IMPORTANCE_HIGH) {
                    setName(context.getString(R.string.notification_channel_alerts))
                    setGroup(GROUP_BREEZY_WEATHER)
                },
                buildNotificationChannel(CHANNEL_FORECAST, IMPORTANCE_LOW) {
                    setName(context.getString(R.string.notification_channel_forecast))
                    setVibrationEnabled(false)
                    setSound(null, null)
                    setGroup(GROUP_BREEZY_WEATHER)
                },
                buildNotificationChannel(CHANNEL_WIDGET, IMPORTANCE_DEFAULT) {
                    setName(context.getString(R.string.notification_channel_widget))
                    setGroup(GROUP_BREEZY_WEATHER)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_ACTIVE_LOCATION, IMPORTANCE_LOW) {
                    setName(context.getString(R.string.notification_channel_active_location))
                    setGroup(GROUP_BREEZY_WEATHER)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_BACKGROUND, IMPORTANCE_MIN) {
                    setName(context.getString(R.string.notification_channel_background_services))
                    setGroup(GROUP_BREEZY_WEATHER)
                    setShowBadge(false)
                },
                buildNotificationChannel(CHANNEL_CRASH_LOGS, IMPORTANCE_HIGH) {
                    setName(context.getString(R.string.notification_channel_crash_logs))
                },
                buildNotificationChannel(CHANNEL_APP_UPDATE, IMPORTANCE_DEFAULT) {
                    setName(context.getString(R.string.notification_channel_app_updates))
                }
            ).filter { notificationManager.getNotificationChannel(it.id) == null }
        )

        notificationManager.createNotificationChannelsCompat(
            listOfNotNull(
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_RAIN, IMPORTANCE_DEFAULT, R.string.notification_channel_precipitation),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_STORM, IMPORTANCE_DEFAULT, R.string.notification_channel_precipitation),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE, IMPORTANCE_HIGH, R.string.notification_channel_severe),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_VOLCANIC, IMPORTANCE_HIGH, R.string.notification_channel_severe),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_CRITICAL, IMPORTANCE_HIGH, R.string.notification_channel_extreme),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_RAIN_NO_VIBRATION, IMPORTANCE_DEFAULT, R.string.notification_channel_precipitation, false),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_STORM_NO_VIBRATION, IMPORTANCE_DEFAULT, R.string.notification_channel_precipitation, false),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE_NO_VIBRATION, IMPORTANCE_HIGH, R.string.notification_channel_severe, false),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_VOLCANIC_NO_VIBRATION, IMPORTANCE_HIGH, R.string.notification_channel_severe, false),
                soundChannel(context, AlertaEcNotificationAppearance.SOUND_CRITICAL_NO_VIBRATION, IMPORTANCE_HIGH, R.string.notification_channel_extreme, false),
                vibrationChannel(context, AlertaEcNotificationAppearance.VIBRATION_FORECAST, IMPORTANCE_LOW, R.string.notification_channel_forecast),
                vibrationChannel(context, AlertaEcNotificationAppearance.VIBRATION_SEVERE, IMPORTANCE_HIGH, R.string.notification_channel_severe),
                vibrationChannel(context, AlertaEcNotificationAppearance.VIBRATION_EXTREME, IMPORTANCE_HIGH, R.string.notification_channel_extreme),
                silentChannel(context, AlertaEcNotificationAppearance.SILENT_SEVERE, IMPORTANCE_HIGH, R.string.notification_channel_severe),
                silentChannel(context, AlertaEcNotificationAppearance.SILENT_EXTREME, IMPORTANCE_HIGH, R.string.notification_channel_extreme),
            ).filter { notificationManager.getNotificationChannel(it.id) == null }
        )
    }

    private fun soundChannel(context: Context, id: String, importance: Int, name: Int, vibrationEnabled: Boolean = true): NotificationChannelCompat? {
        val resource = AlertaEcNotificationAppearance.soundResource(id) ?: return null
        return buildNotificationChannel(id, importance) {
            setName(context.getString(name))
            setGroup(GROUP_BREEZY_WEATHER)
            setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + resource),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setVibrationEnabled(vibrationEnabled)
            setVibrationPattern(longArrayOf(0, 180))
        }
    }

    private fun vibrationChannel(context: Context, id: String, importance: Int, name: Int): NotificationChannelCompat =
        buildNotificationChannel(id, importance) {
            setName(context.getString(name))
            setGroup(GROUP_BREEZY_WEATHER)
            setSound(null, null)
            setVibrationEnabled(true)
            setVibrationPattern(longArrayOf(0, 180))
        }

    private fun silentChannel(context: Context, id: String, importance: Int, name: Int): NotificationChannelCompat =
        buildNotificationChannel(id, importance) {
            setName(context.getString(name))
            setGroup(GROUP_BREEZY_WEATHER)
            setSound(null, null)
            setVibrationEnabled(false)
        }

    private fun categoryChannel(channelId: String): String? = when (channelId) {
        AlertaEcNotificationAppearance.SOUND_RAIN,
        AlertaEcNotificationAppearance.SOUND_STORM,
        AlertaEcNotificationAppearance.SOUND_RAIN_NO_VIBRATION,
        AlertaEcNotificationAppearance.SOUND_STORM_NO_VIBRATION,
        AlertaEcNotificationAppearance.VIBRATION_FORECAST -> CHANNEL_FORECAST
        AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE,
        AlertaEcNotificationAppearance.SOUND_VOLCANIC,
        AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE_NO_VIBRATION,
        AlertaEcNotificationAppearance.SOUND_VOLCANIC_NO_VIBRATION,
        AlertaEcNotificationAppearance.VIBRATION_SEVERE -> AlertaEcNotificationAppearance.SEVERE
        AlertaEcNotificationAppearance.SOUND_CRITICAL,
        AlertaEcNotificationAppearance.SOUND_CRITICAL_NO_VIBRATION,
        AlertaEcNotificationAppearance.VIBRATION_EXTREME,
        AlertaEcNotificationAppearance.SILENT_EXTREME -> AlertaEcNotificationAppearance.EXTREME
        AlertaEcNotificationAppearance.SILENT_SEVERE -> AlertaEcNotificationAppearance.SEVERE
        else -> null
    }

    internal fun weatherChannel(context: Context, requested: String): String =
        if (NotificationManagerCompat.from(context).getNotificationChannel(requested) != null) requested else CHANNEL_ALERT

    internal fun canPostToChannel(context: Context, channelId: String, logicalChannelId: String? = null): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannel(channelId) ?: return false
        val groupBlocked = channel.group?.let { manager.getNotificationChannelGroup(it)?.isBlocked == true } == true
        val categoryBlocked = if (channelId == AlertaEcNotificationAppearance.VIBRATION_FORECAST || channelId == AlertaEcNotificationAppearance.SOUND_RAIN) {
            listOf(CHANNEL_FORECAST, AlertaEcNotificationAppearance.PRECIPITATION).any { manager.getNotificationChannel(it)?.importance == 0 }
        } else {
            logicalChannelId?.let { manager.getNotificationChannel(it)?.importance == 0 } == true || categoryChannel(channelId)?.let { manager.getNotificationChannel(it)?.importance == 0 } == true
        }
        return shouldPostToChannel(true, true, channel.importance, groupBlocked || categoryBlocked)
    }

    internal fun shouldPostToChannel(
        notificationsEnabled: Boolean,
        postNotificationsGranted: Boolean,
        importance: Int,
        channelBlocked: Boolean,
    ): Boolean = notificationsEnabled && postNotificationsGranted && importance != 0 && !channelBlocked

    private fun noVibrationSoundChannel(channel: String): String = when (channel) {
        AlertaEcNotificationAppearance.SOUND_RAIN -> AlertaEcNotificationAppearance.SOUND_RAIN_NO_VIBRATION
        AlertaEcNotificationAppearance.SOUND_STORM -> AlertaEcNotificationAppearance.SOUND_STORM_NO_VIBRATION
        AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE -> AlertaEcNotificationAppearance.SOUND_FLOOD_LANDSLIDE_NO_VIBRATION
        AlertaEcNotificationAppearance.SOUND_VOLCANIC -> AlertaEcNotificationAppearance.SOUND_VOLCANIC_NO_VIBRATION
        AlertaEcNotificationAppearance.SOUND_CRITICAL -> AlertaEcNotificationAppearance.SOUND_CRITICAL_NO_VIBRATION
        else -> channel
    }

    private fun silentChannel(fallbackChannel: String): String = when (fallbackChannel) {
        AlertaEcNotificationAppearance.SEVERE -> AlertaEcNotificationAppearance.SILENT_SEVERE
        AlertaEcNotificationAppearance.EXTREME -> AlertaEcNotificationAppearance.SILENT_EXTREME
        else -> fallbackChannel
    }

    internal fun selectChannel(soundEnabled: Boolean, vibrationEnabled: Boolean, soundChannel: String?, fallbackChannel: String): String = when {
        soundEnabled && soundChannel != null -> if (vibrationEnabled) soundChannel else noVibrationSoundChannel(soundChannel)
        vibrationEnabled -> when (fallbackChannel) {
            AlertaEcNotificationAppearance.SEVERE -> AlertaEcNotificationAppearance.VIBRATION_SEVERE
            AlertaEcNotificationAppearance.EXTREME -> AlertaEcNotificationAppearance.VIBRATION_EXTREME
            else -> AlertaEcNotificationAppearance.VIBRATION_FORECAST
        }
        else -> silentChannel(fallbackChannel)
    }

    internal fun logicalCategory(channelId: String): String? = categoryChannel(channelId)

    internal fun shouldSetSilent(
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        intentionalSilence: Boolean = false,
        quietUpdate: Boolean = false,
    ): Boolean = intentionalSilence || quietUpdate || (!soundEnabled && !vibrationEnabled)
    private fun getNotificationBuilder(
        context: Context,
        @DrawableRes iconId: Int,
        title: String,
        subtitle: String,
        content: CharSequence?,
        intent: PendingIntent,
        channel: String = CHANNEL_ALERT,
    ): NotificationCompat.Builder {
        return context.notificationBuilder(channel).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(iconId)
            setContentTitle(title)
            setSubText(subtitle)
            setContentText(content)
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            setContentIntent(intent)
        }
    }

    fun checkAndSendAlert(
        context: Context,
        location: Location,
        oldResult: Weather?,
    ) {
        val weather = location.weather
        if (weather == null || !SettingsManager.getInstance(context).isAlertPushEnabled) return

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val config = ConfigStore(context, PREFERENCE_NOTIFICATION)
        val remembered = config.getStringSet("OFFICIAL_EVENTS", emptySet()).orEmpty().toMutableSet()
        val now = System.currentTimeMillis()
        val alerts = weather.alertList.distinctBy { it.alertId }.filter { alert ->
            alert.severity != AlertSeverity.MINOR &&
                (alert.endDate?.time?.let { it > now } ?: true) &&
                oldResult?.alertList?.none {
                    it.alertId == alert.alertId && it.severity == alert.severity && it.headline == alert.headline
                } != false
        }
        alerts.forEach { alert ->
            val key = "${location.formattedId}|${alert.alertId}|${alert.severity}|${alert.startDate?.time}"
            val phenomenon = AlertaEcWeatherAdapter.phenomenonFromText(alert.headline.orEmpty() + " " + alert.description.orEmpty())
            val soundChannel = if (SettingsManager.getInstance(context).isAlertSoundEnabled) AlertaEcNotificationAppearance.officialSoundChannel(phenomenon, alert.severity) else null
            val channel = weatherChannel(
                context,
                selectChannel(
                    SettingsManager.getInstance(context).isAlertSoundEnabled,
                    SettingsManager.getInstance(context).isAlertVibrationEnabled,
                    soundChannel,
                    AlertaEcNotificationAppearance.officialChannel(alert.severity)
                )
            )
            if (key !in remembered && canPostToChannel(context, channel, AlertaEcNotificationAppearance.officialChannel(alert.severity))) {
                sendAlertNotification(context, location, alert, alerts.size > 1)
                remembered.add(key)
            }
        }
        config.edit().putStringSet("OFFICIAL_EVENTS", remembered.toList().takeLast(200).toSet()).apply()
    }

    private fun sendAlertNotification(
        context: Context,
        location: Location,
        alert: Alert,
        inGroup: Boolean,
    ) {
        val notificationId = getAlertNotificationId(context)
        context.notify(
            notificationId,
            buildSingleAlertNotification(context, location, alert, inGroup, notificationId)
        )
        if (inGroup) {
            context.notify(
                ID_ALERT_GROUP,
                buildAlertGroupSummaryNotification(context, location, alert, notificationId)
            )
        }
    }

    @SuppressLint("InlinedApi")
    private fun buildSingleAlertNotification(
        context: Context,
        location: Location,
        alert: Alert,
        inGroup: Boolean,
        notificationId: Int,
    ): Notification {
        val time = alert.startDate?.let {
            // FIXME: Timezone
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT).format(it)
        }
        val description = alert.description?.replace("\n", "<br />")?.parseAsHtml() ?: ""
        val phenomenon = AlertaEcWeatherAdapter.phenomenonFromText(alert.headline.orEmpty() + " " + alert.description.orEmpty())
        val settings = SettingsManager.getInstance(context)
        val soundChannel = if (settings.isAlertSoundEnabled) {
            AlertaEcNotificationAppearance.officialSoundChannel(phenomenon, alert.severity)
        } else null
        val builder = getNotificationBuilder(
            context,
            AlertaEcNotificationAppearance.icon(AlertaEcWeatherAdapter.phenomenonFromText(alert.headline.orEmpty())),
            alert.headline?.ifEmpty {
                context.getString(R.string.alert)
            } ?: context.getString(R.string.alert),
            location.geographicName,
            description,
            PendingIntent.getActivity(
                context,
                notificationId,
                IntentHelper.buildMainActivityShowAlertsIntent(location, alert.alertId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ),
            weatherChannel(
                context,
                selectChannel(
                    settings.isAlertSoundEnabled,
                    settings.isAlertVibrationEnabled,
                    soundChannel,
                    AlertaEcNotificationAppearance.officialChannel(alert.severity)
                )
            )
        ).setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle(
                    alert.headline?.ifEmpty {
                        context.getString(R.string.alert)
                    } ?: context.getString(R.string.alert)
                )
                .setSummaryText(time)
                .bigText(description)
        )
        if (inGroup) {
            builder.setGroup(ALERT_GROUP_KEY)
            builder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
        }
        if (shouldSetSilent(settings.isAlertSoundEnabled, settings.isAlertVibrationEnabled)) builder.setSilent(true)
        return builder.build()
    }

    @SuppressLint("InlinedApi")
    private fun buildAlertGroupSummaryNotification(
        context: Context,
        location: Location,
        alert: Alert,
        notificationId: Int,
    ): Notification {
        return context.notificationBuilder(CHANNEL_ALERT).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_notification_weather)
            setSilent(true)
            setContentTitle(
                alert.headline?.ifEmpty {
                    context.getString(R.string.alert)
                } ?: context.getString(R.string.alert)
            )
            setGroup(ALERT_GROUP_KEY)
            color = getColor(context, location)
            setGroupSummary(true)
            setOnlyAlertOnce(true)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    IntentHelper.buildMainActivityShowAlertsIntent(location, alert.alertId),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }.build()
    }

    private fun getAlertNotificationId(context: Context): Int {
        val config = ConfigStore(context, PREFERENCE_NOTIFICATION)
        var id = config.getInt(KEY_NOTIFICATION_ID, ID_ALERT_MIN) + 1
        if (id > ID_ALERT_MAX) {
            id = ID_ALERT_MIN
        }
        config.edit()
            .putInt(KEY_NOTIFICATION_ID, id)
            .apply()
        return id
    }

    private fun precipitationLocationKey(location: Location): String =
        MessageDigest.getInstance("SHA-256")
            .digest(location.formattedId.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    // precipitation.
    fun checkAndSendPrecipitation(context: Context, location: Location) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.isPrecipitationPushEnabled) {
            context.cancelNotification(ID_PRECIPITATION)
            return
        }
        val weather = location.weather
        val samples = weather?.minutelyForecast
        if (samples.isNullOrEmpty()) {
            context.cancelNotification(ID_PRECIPITATION)
            return
        }
        if (weather == null || !weather.hasMinutelyPrecipitation) {
            context.cancelNotification(ID_PRECIPITATION)
            return
        }

        val config = ConfigStore(context, PREFERENCE_NOTIFICATION)
        val now = System.currentTimeMillis()
        val locationKey = precipitationLocationKey(location)
        val regularKey = "PRECIPITATION_LAST_${location.formattedId}"
        val last = config.getLong(regularKey, 0)
        val quietUpdate = last > 0 && now - last < 30 * 60 * 1000L
        val decision = AlertaEcRainEpisodeEvaluator.evaluate(samples, now)
        val stateKey = "PRECIPITATION_STATE_$locationKey"
        val validUntilKey = "PRECIPITATION_STATE_VALID_UNTIL_$locationKey"
        val episodeKeyName = "PRECIPITATION_EPISODE_$locationKey"
        val transitionKeyName = "PRECIPITATION_TRANSITION_$locationKey"
        val previousState = config.getString(stateKey, null)?.let { value ->
            runCatching { org.breezyweather.alertaec.AlertaEcRainEpisodeState.valueOf(value) }.getOrNull()
        }?.takeIf { config.getLong(validUntilKey, 0L) > now }
        val previousEpisodeKey = config.getString(episodeKeyName, null)
        val previousTransitionKey = config.getString(transitionKeyName, null)
        val episodeKey = AlertaEcRainEpisodeEvaluator.stableEpisodeKey(previousState, previousEpisodeKey, decision)
        val transitionKey = AlertaEcRainEpisodeEvaluator.stableNotificationTransitionKey(
            previousState, episodeKey, previousTransitionKey, decision
        )
        val audibleKey = "PRECIPITATION_AUDIBLE_$locationKey"
        val alreadyNotified = transitionKey != null && config.getString(audibleKey, null) == transitionKey
        val audible = settings.isPrecipitationAudibleEnabled && decision.canNotifyPreventively && !alreadyNotified
        val channel = if (audible) {
            weatherChannel(
                context,
                selectChannel(
                    settings.isAlertSoundEnabled,
                    settings.isAlertVibrationEnabled,
                    AlertaEcNotificationAppearance.SOUND_RAIN,
                    CHANNEL_FORECAST
                )
            )
        } else weatherChannel(context, AlertaEcNotificationAppearance.PRECIPITATION)
        context.notify(
            ID_PRECIPITATION,
            getNotificationBuilder(
                context,
                R.drawable.ic_notification_rain,
                weather.getMinutelyTitle(context),
                location.geographicName,
                weather.getMinutelyDescription(context, location),
                PendingIntent.getActivity(
                    context,
                    ID_PRECIPITATION,
                    IntentHelper.buildMainActivityIntent(location),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                ),
                channel
            ).apply {
                if (shouldSetSilent(
                        settings.isAlertSoundEnabled,
                        settings.isAlertVibrationEnabled,
                        intentionalSilence = !audible,
                        quietUpdate = quietUpdate
                    )
                ) {
                    setSilent(true)
                }
            }.build()
        )
        config.edit()
            .putString(stateKey, decision.state.name)
            .putLong(validUntilKey, decision.validUntilMillis ?: 0L)
            .putString(episodeKeyName, episodeKey)
            .putString(transitionKeyName, transitionKey)
            .apply()
        if (audible && transitionKey != null) config.edit().putString(audibleKey, transitionKey).apply()
        if (!quietUpdate && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            config.edit().putLong(regularKey, now).apply()
        }
    }

    @ColorInt
    private fun getColor(context: Context, location: Location): Int {
        return ContextCompat.getColor(
            context,
            if (location.isDaylight) R.color.lightPrimary_5 else R.color.darkPrimary_5
        )
    }
}
