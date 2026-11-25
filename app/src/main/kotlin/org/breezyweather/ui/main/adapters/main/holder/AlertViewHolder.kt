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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import breezyweather.domain.location.model.Location
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

    /** Set by MainAdapter: close the card (also by swiping it left or right). */
    var onDismiss: () -> Unit = {}

    /** Set by MainAdapter: move the card between the top and the bottom of the home screen. */
    var onTogglePosition: () -> Unit = {}
    var isAtBottom: Boolean = false

    @SuppressLint("DefaultLocale")
    override fun onBindView(
        activity: BreezyActivity,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
    ) {
        super.onBindView(activity, location, provider, listAnimationEnabled, itemAnimationEnabled)

        // Don’t show if alertList only contains alerts in the past
        if (location.weather?.alertList?.any { it.endDate == null || it.endDate!!.time > Date().time } == true) {
            itemView.visibility = View.VISIBLE
            itemView.setOnClickListener {
                IntentHelper.startAlertActivity(activity, location.formattedId)
            }
            itemView.findViewById<ComposeView>(R.id.container_main_first_card_alert_list).setContent {
                BreezyWeatherTheme(!ThemeManager.isLightTheme(activity, location)) {
                    Column {
                        AlertsHeader()
                        if (location.weather!!.currentAlertList.isEmpty()) {
                            UpcomingAlerts(location.formattedId)
                        } else {
                            AlertList(location)
                        }
                    }
                }
            }
        } else {
            itemView.visibility = View.GONE
        }
    }

    /** Title row with "move" and a close X; both reachable with the TV remote (D-pad + OK). */
    @Composable
    private fun AlertsHeader() {
        val neon = Color(0xFF00E5FF)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Alertas meteorológicas",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            val moveSource = remember { MutableInteractionSource() }
            val moveFocused = moveSource.collectIsFocusedAsState().value
            TextButton(
                onClick = { onTogglePosition() },
                interactionSource = moveSource,
                modifier = if (moveFocused) {
                    Modifier.border(2.dp, neon, RoundedCornerShape(50))
                } else {
                    Modifier
                }
            ) {
                Icon(
                    painterResource(if (isAtBottom) R.drawable.ic_arrow_upward_alt else R.drawable.ic_arrow_downward_alt),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isAtBottom) "Mover arriba" else "Mover abajo",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val closeSource = remember { MutableInteractionSource() }
            val closeFocused = closeSource.collectIsFocusedAsState().value
            IconButton(
                onClick = { onDismiss() },
                interactionSource = closeSource,
                modifier = if (closeFocused) Modifier.border(2.dp, neon, CircleShape) else Modifier
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "Cerrar alertas (también puedes deslizar la tarjeta)",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
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
        Column {
            location.weather!!.currentAlertList.forEach { currentAlert ->
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
}
