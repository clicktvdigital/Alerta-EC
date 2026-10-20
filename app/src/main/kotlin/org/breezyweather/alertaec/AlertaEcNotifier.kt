package org.breezyweather.alertaec

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import breezyweather.domain.location.model.Location
import org.breezyweather.R
import org.breezyweather.common.extensions.notificationBuilder
import org.breezyweather.common.extensions.notify
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.common.extensions.formatMeasure
import org.breezyweather.unit.formatting.UnitWidth
import org.breezyweather.unit.temperature.Temperature.Companion.celsius

object AlertaEcNotifier {

    private const val ID_BASE = 5000
    private const val ID_RANGE = 1000

    fun checkAndNotify(context: Context, location: Location, nowMillis: Long = System.currentTimeMillis(), fromQueueWorker: Boolean = false) {
        if (!SettingsManager.getInstance(context).isAlertPushEnabled) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = NotificationManagerCompat.from(context)
        val channel = org.breezyweather.remoteviews.Notifications.weatherChannel(context, "forecast")
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannel(channel)?.importance == 0) return

        // OFFICIAL_EVENT ya se envía desde remoteviews.Notifications.
        val events = AlertaEcWeatherAdapter.fromLocation(location, nowMillis)
            .filter { AlertaEcNotificationPolicy.shouldNotify(it, nowMillis) }

        AlertaEcNotificationQueueStore.saveLocation(context, location.formattedId)
        val newEvents = AlertaEcEventDeduplicator.filterNewEvents(context, events)
        val priority = AlertaEcNotificationQueuePolicy.priority(newEvents)
        priority.forEach { event ->
            sendNotification(context, location, event, nowMillis)
            AlertaEcEventDeduplicator.markNotified(context, event)
        }

        val informational = AlertaEcNotificationQueuePolicy.informational(newEvents, nowMillis)
        AlertaEcNotificationQueueStore.enqueue(context, informational.map { it.id })
        AlertaEcNotificationQueueStore.nextValid(context, events, nowMillis)
        if (AlertaEcNotificationQueuePolicy.canShowNext(
                AlertaEcNotificationQueueStore.lastShownAt(context), nowMillis
            )
        ) {
            AlertaEcNotificationQueueStore.nextValid(context, events, nowMillis)?.let { event ->
                sendNotification(context, location, event, nowMillis)
                AlertaEcEventDeduplicator.markNotified(context, event)
                AlertaEcNotificationQueueStore.remove(context, event.id)
                AlertaEcNotificationQueueStore.markShown(context, nowMillis)

            }
        }
        if (!fromQueueWorker && AlertaEcNotificationQueueStore.pending(context).isNotEmpty()) {
            val remainingMillis = (AlertaEcNotificationQueuePolicy.nextEligibleAt(
                AlertaEcNotificationQueueStore.lastShownAt(context)
            ) - nowMillis).coerceAtLeast(0L)
            AlertaEcNotificationQueueScheduler.schedule(
                context, (remainingMillis + 999L) / 1000L
            )
        }
    }

    private fun sendNotification(context: Context, location: Location, event: AlertaEcEvent, nowMillis: Long) {
        val notificationId = if (
            event.severity == AlertaEcSeverity.INFORMATION ||
            event.severity == AlertaEcSeverity.ADVISORY
        ) {
            ID_BASE
        } else {
            ID_BASE + 1 + ((event.id.hashCode() and Int.MAX_VALUE) % (ID_RANGE - 1))
        }
        val stateLabel = AlertaEcNotificationPolicy.stateLabel(event.state)
        val settings = SettingsManager.getInstance(context)
        val temperatureUnit = settings.getTemperatureUnit(context)

        val interval = event.startTimeMillis?.let {
            if (event.state == AlertaEcEventState.FORECAST) {
                AlertaEcNotificationText.hourlyPeriod(it, nowMillis, location.timeZone)
            } else {
                AlertaEcNotificationText.interval(it, nowMillis, location.timeZone)
            }
        } ?: "Hora no disponible"
        val temperatureText = event.temperatureCelsius?.takeIf { it.isFinite() }?.let {
            "Temp. ${it.celsius.formatMeasure(context, temperatureUnit, unitWidth = UnitWidth.NARROW)}"
        } ?: "Temp. —"
        val compact = listOf(
            temperatureText,
            AlertaEcNotificationText.probabilityPercent(event.precipitationProbabilityPercent?.toDouble()),
            AlertaEcNotificationText.accumulationMm(event.precipitationAccumulationMm)
        ).joinToString(" · ")
        val details = buildList {
            add("$stateLabel · ${event.headline}")
            add("$interval · $compact")
            add("Precipitación: acumulación del intervalo horario; no intensidad instantánea.")
            event.feelsLikeCelsius?.takeIf { it.isFinite() }?.let {
                add("Sensación térmica: ${it.celsius.formatMeasure(context, temperatureUnit, unitWidth = UnitWidth.NARROW)}")
            }
            event.windSpeedKmh?.let { add(String.format("viento %.0f km/h", it)) }
            event.windSpeedKmh?.let { add(String.format("%.1f m/s", it / 3.6)) }
            event.windGustKmh?.let { add(String.format("ráfagas %.0f km/h", it)) }
            event.windDirectionDegrees?.let { add("viento desde ${windDirection(it)} ($it°)") }
            event.humidityPercent?.let { add("humedad $it%") }
            event.pressureHpa?.let { add(String.format("presión %.0f hPa", it)) }
            event.visibilityKm?.let { add(String.format("visibilidad %.1f km", it)) }
            event.sourceName?.let { add("Fuente: $it") }
            event.updatedAtMillis?.let {
                val updatedTime = java.text.DateFormat.getDateTimeInstance(
                    java.text.DateFormat.SHORT,
                    java.text.DateFormat.SHORT
                ).apply {
                    timeZone = location.timeZone
                }.format(java.util.Date(it))
                add(context.getString(R.string.alertaec_hourly_acquired_at, updatedTime))
            }
        }.joinToString("\n")

        val title = "${event.headline} · ${event.locationName ?: "Ubicación no disponible"}"
        val advice = AlertaEcRecommendations.forEvent(event)
        val content = listOfNotNull(
            details.ifBlank { event.description ?: event.phenomenon.spokenName },
            advice.takeIf { event.state != AlertaEcEventState.OFFICIAL_EVENT }?.let { "Consejo preventivo: $it" }
        ).joinToString("\n")

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            IntentHelper.buildMainActivityIntent(location),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = context.notificationBuilder(
            org.breezyweather.remoteviews.Notifications.weatherChannel(context, "forecast")
        ).apply {
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            setSmallIcon(AlertaEcNotificationAppearance.icon(event.phenomenon))
            setContentTitle(title)
            setSubText(interval)
            setContentText(compact)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            if (event.severity == AlertaEcSeverity.INFORMATION || event.severity == AlertaEcSeverity.ADVISORY) {
                setTimeoutAfter(30_000L)
                val dismissIntent = android.content.Intent(
                    context,
                    org.breezyweather.background.receiver.NotificationReceiver::class.java
                ).apply {
                    action = org.breezyweather.background.receiver.NotificationReceiver.ACTION_ALERTA_EC_DISMISSED
                }
                setDeleteIntent(
                    PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        dismissIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }
            setContentIntent(pendingIntent)
            addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.alertaec_view_details),
                pendingIntent
            )
        }.build()

        context.notify(notificationId, notification)
    }

    private fun windDirection(degrees: Int): String = when ((((degrees % 360 + 360) % 360 + 22.5) / 45).toInt() % 8) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SO"
        6 -> "O"
        else -> "NO"
    }
}
