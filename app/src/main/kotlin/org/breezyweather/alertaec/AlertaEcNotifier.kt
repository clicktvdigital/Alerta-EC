package org.breezyweather.alertaec

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import org.breezyweather.common.extensions.notificationBuilder
import org.breezyweather.common.extensions.notify
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.domain.settings.SettingsManager

object AlertaEcNotifier {

    private const val CHANNEL_ALERT = "alert"
    private const val ID_BASE = 4000
    private const val ID_RANGE = 1000

    fun checkAndNotify(context: Context, location: Location) {
        if (!SettingsManager.getInstance(context).isAlertPushEnabled) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        // OFFICIAL_EVENT ya se envía desde remoteviews.Notifications.
        val events = AlertaEcWeatherAdapter.fromLocation(location)
            .filter { AlertaEcNotificationPolicy.shouldNotify(it.state) }

        val newEvents = AlertaEcEventDeduplicator.filterNewEvents(context, events)
        newEvents.forEach { event ->
            sendNotification(context, location, event)
            AlertaEcEventDeduplicator.markNotified(context, event)
        }
    }

    private fun sendNotification(context: Context, location: Location, event: AlertaEcEvent) {
        val notificationId = ID_BASE + ((event.id.hashCode() and Int.MAX_VALUE) % ID_RANGE)
        val stateLabel = AlertaEcNotificationPolicy.stateLabel(event.state)

        val details = buildList {
            event.locationName?.let { add(it) }
            event.precipitationMmPerHour?.let { add(String.format("%.1f mm/h", it)) }
            event.precipitationProbabilityPercent?.let { add("$it% de probabilidad") }
            event.windSpeedKmh?.let { add(String.format("viento %.0f km/h", it)) }
            event.windGustKmh?.let { add(String.format("ráfagas %.0f km/h", it)) }
            event.sourceName?.let { add("Fuente: $it") }
        }.joinToString(" · ")

        val title = "$stateLabel · ${event.headline}"
        val content = details.ifBlank { event.description ?: event.phenomenon.spokenName }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            IntentHelper.buildMainActivityIntent(location),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = context.notificationBuilder(CHANNEL_ALERT).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_alert)
            setContentTitle(title)
            setContentText(content)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setContentIntent(pendingIntent)
        }.build()

        context.notify(notificationId, notification)
    }
}
