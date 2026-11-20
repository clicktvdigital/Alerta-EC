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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.breezyweather.ui.support.isTelevisionDevice
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Alert
import org.breezyweather.R
import org.breezyweather.common.activities.BreezyActivity
import org.breezyweather.common.utils.helpers.IntentHelper
import org.breezyweather.domain.weather.model.getFormattedDates
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.compose.themeRipple
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import java.util.Date

class AlertViewHolder(parent: ViewGroup) : AbstractMainCardViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.container_main_alert, parent, false)
) {

    @SuppressLint("DefaultLocale")
    override fun onBindView(
        activity: BreezyActivity,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
    ) {
        super.onBindView(activity, location, provider, listAnimationEnabled, itemAnimationEnabled)

        // Don’t show if alertList only contains alerts in the past, or the user dismissed every current one
        val dismissed = DismissedAlerts(activity)
        val hasCurrent = location.weather?.currentAlertList.orEmpty().any { isToday(it) && !dismissed.contains(it) }
        val hasUpcoming = location.weather?.currentAlertList.isNullOrEmpty() &&
            location.weather?.alertList?.any { it.endDate == null || it.endDate!!.time > Date().time } == true
        if (hasCurrent || hasUpcoming) {
            itemView.visibility = View.VISIBLE
            itemView.setOnClickListener {
                if (activity.isTelevisionDevice()) {
                    // TV: OK offers the detail or dismissing the warnings shown (they come back if they change).
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                        .setItems(arrayOf("Ver detalle de las alertas", "Descartar estas alertas")) { _, which ->
                            if (which == 0) {
                                IntentHelper.startAlertActivity(activity, location.formattedId)
                            } else {
                                dismissed.addAll(location.weather?.currentAlertList.orEmpty())
                                dismissedVersion.intValue++
                                itemView.visibility = View.GONE
                            }
                        }
                        .setNegativeButton(R.string.action_close, null)
                        .show()
                } else {
                    IntentHelper.startAlertActivity(activity, location.formattedId)
                }
            }
            itemView.findViewById<ComposeView>(R.id.container_main_first_card_alert_list).setContent {
                BreezyWeatherTheme(!ThemeManager.isLightTheme(activity, location)) {
                    if (location.weather!!.currentAlertList.isEmpty()) {
                        UpcomingAlerts(location.formattedId)
                    } else {
                        AlertList(location)
                    }
                }
            }
        } else {
            itemView.visibility = View.GONE
        }
    }

    @Composable
    fun UpcomingAlerts(
        formattedId: String,
        modifier: Modifier = Modifier,
    ) {
        val activity = LocalActivity.current
        ListItem(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = themeRipple(),
                    onClick = {
                        activity?.let {
                            IntentHelper.startAlertActivity(it, formattedId)
                        }
                    }
                ),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = {
                Text(
                    stringResource(R.string.alerts_to_follow),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            leadingContent = {
                Icon(
                    painterResource(R.drawable.ic_warning),
                    contentDescription = stringResource(R.string.alerts_to_follow),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }

    @Composable
    fun AlertList(
        location: Location,
        modifier: Modifier = Modifier,
    ) {
        val activity = LocalActivity.current
        val context = LocalContext.current
        // Most severe first (red → orange → yellow), then by start: the source order varies between
        // refreshes, so the same INAMHI warnings used to swap places on the TV home.
        // Only today's warnings that the user has not dismissed (X on touch, OK → «Descartar» on TV).
        val version = dismissedVersion.intValue
        val dismissed = remember(version) { DismissedAlerts(context) }
        val sortedAlerts = location.weather!!.currentAlertList
            .filter { isToday(it) && !dismissed.contains(it) }
            .sortedWith(compareByDescending<Alert> { it.severity.id }.thenBy { it.startDate })
        Column {
            sortedAlerts.forEach { currentAlert ->
                ListItem(
                    modifier = modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = themeRipple(),
                            onClick = {
                                activity?.let {
                                    IntentHelper.startAlertActivity(it, location.formattedId, currentAlert.alertId)
                                }
                            }
                        ),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    // Touch: X dismisses this warning on the device until it ends (TV uses OK → «Descartar»).
                    trailingContent = {
                        androidx.compose.material3.IconButton(onClick = {
                            dismissed.add(currentAlert)
                            dismissedVersion.intValue++
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Descartar alerta")
                        }
                    },
                    headlineContent = {
                        Text(
                            currentAlert.headline?.ifEmpty {
                                stringResource(R.string.alert)
                            } ?: stringResource(R.string.alert),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingContent = currentAlert.startDate?.let {
                        {
                            Text(
                                currentAlert.getFormattedDates(location, context),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clearAndSetSemantics {
                                        contentDescription = currentAlert.getFormattedDates(
                                            location,
                                            context,
                                            full = true
                                        )
                                    }
                            )
                        }
                    },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.material_icon_size))
                                .aspectRatio(ratio = 1F)
                                .clip(shape = MaterialShapes.Triangle.toShape())
                                .background(Color(currentAlert.color))
                        ) {}
                    }
                )
            }
        }
    }

    /** Bumped when the user dismisses warnings, so the Compose list recomposes without them. */
    private val dismissedVersion = androidx.compose.runtime.mutableIntStateOf(0)

    /** True when the warning's period touches today (no warnings from previous days). */
    private fun isToday(alert: Alert): Boolean {
        val start = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 24L * 60 * 60 * 1000
        return (alert.startDate?.time ?: Long.MIN_VALUE) < end && (alert.endDate?.time ?: Long.MAX_VALUE) >= start
    }

    /** Warnings dismissed on this device, kept until each one ends (an updated warning gets a new id). */
    private class DismissedAlerts(context: android.content.Context) {
        private val prefs = context.getSharedPreferences("alerta_ec_alertas_descartadas", android.content.Context.MODE_PRIVATE)
        private fun key(alert: Alert) = alert.alertId

        fun contains(alert: Alert): Boolean = prefs.contains(key(alert))

        fun addAll(alerts: List<Alert>) {
            val now = System.currentTimeMillis()
            prefs.edit().apply {
                // Forget dismissals of warnings that already ended.
                prefs.all.forEach { (k, v) -> if ((v as? Long ?: 0L) in 1 until now) remove(k) }
                alerts.forEach { putLong(key(it), it.endDate?.time ?: (now + 24L * 60 * 60 * 1000)) }
            }.apply()
        }

        fun add(alert: Alert) = addAll(listOf(alert))
    }
}
