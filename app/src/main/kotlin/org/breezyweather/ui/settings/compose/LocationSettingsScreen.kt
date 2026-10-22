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

import android.Manifest
import android.app.Activity
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.breezyweather.background.location.ActiveLocationController
import org.breezyweather.background.location.DetailedLocationAddress
import org.breezyweather.background.location.DetailedLocationProvider
import org.breezyweather.background.location.LocationHumanFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.collections.immutable.ImmutableList
import org.breezyweather.BreezyWeather
import org.breezyweather.BuildConfig
import org.breezyweather.R
import org.breezyweather.common.extensions.openApplicationDetailsSettings
import org.breezyweather.common.extensions.plus
import org.breezyweather.common.preference.EditTextPreference
import org.breezyweather.common.preference.ListPreference
import org.breezyweather.common.source.ConfigurableSource
import org.breezyweather.common.source.LocationSource
import org.breezyweather.common.source.NonFreeNetSource
import org.breezyweather.common.source.RemovedSource
import org.breezyweather.common.source.getName
import org.breezyweather.common.utils.helpers.SnackbarHelper
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.common.composables.AlertDialogLink
import org.breezyweather.ui.common.widgets.Material3ExpressiveCardListItem
import org.breezyweather.ui.common.widgets.Material3Scaffold
import org.breezyweather.ui.common.widgets.generateCollapsedScrollBehavior
import org.breezyweather.ui.common.widgets.insets.FitStatusBarTopAppBar
import org.breezyweather.ui.settings.preference.bottomInsetItem
import org.breezyweather.ui.settings.preference.clickablePreferenceItem
import org.breezyweather.ui.settings.preference.composables.EditTextPreferenceViewWithCard
import org.breezyweather.ui.settings.preference.composables.ListPreferenceView
import org.breezyweather.ui.settings.preference.composables.ListPreferenceViewWithCard
import org.breezyweather.ui.settings.preference.composables.PreferenceScreen
import org.breezyweather.ui.settings.preference.composables.PreferenceViewWithCard
import org.breezyweather.ui.settings.preference.composables.SectionFooter
import org.breezyweather.ui.settings.preference.composables.SectionHeader
import org.breezyweather.ui.settings.preference.editTextPreferenceItem
import org.breezyweather.ui.settings.preference.largeSeparatorItem
import org.breezyweather.ui.settings.preference.listPreferenceItem
import org.breezyweather.ui.settings.preference.sectionFooterItem
import org.breezyweather.ui.settings.preference.sectionHeaderItem
import org.breezyweather.ui.settings.preference.smallSeparatorItem

@Composable
fun LocationSettingsScreen(
    context: Activity,
    onNavigateBack: () -> Unit,
    locationSources: ImmutableList<LocationSource>,
    detailedLocationProvider: DetailedLocationProvider,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = generateCollapsedScrollBehavior()
    val accessCoarseLocationPermissionState =
        rememberPermissionState(permission = Manifest.permission.ACCESS_COARSE_LOCATION)
    val accessFineLocationPermissionState =
        rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val accessBackgroundLocationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Only save the permission state on supported Android versions to
        // prevent a crash on older Android versions.
        rememberPermissionState(permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        null
    }

    Material3Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FitStatusBarTopAppBar(
                title = stringResource(R.string.settings_location),
                onBackPressed = onNavigateBack,
                actions = { AboutActivityIconButton(context) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddings ->
        PreferenceScreen(
            paddingValues = paddings.plus(PaddingValues(horizontal = dimensionResource(R.dimen.normal_margin)))
        ) {
            if (BuildConfig.FLAVOR == "freenet") {
                clickablePreferenceItem(R.string.settings_weather_source_freenet_disclaimer) { id ->
                    val dialogLinkOpenState = remember { mutableStateOf(false) }

                    Material3ExpressiveCardListItem(
                        surface = MaterialTheme.colorScheme.secondaryContainer,
                        onSurface = MaterialTheme.colorScheme.onSecondaryContainer,
                        isFirst = true,
                        isLast = true,
                        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.small_margin))
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                top = dimensionResource(R.dimen.normal_margin),
                                start = dimensionResource(R.dimen.normal_margin),
                                end = dimensionResource(R.dimen.normal_margin)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.settings_weather_source_freenet_disclaimer),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (BuildConfig.INSTALL_INSTRUCTIONS_LINK.startsWith("https://") &&
                                (
                                    !BuildConfig.INSTALL_INSTRUCTIONS_LINK.contains("breezy", ignoreCase = true) ||
                                        BreezyWeather.instance.isSignedByBreezy ||
                                        BreezyWeather.instance.debugMode
                                    )
                            ) {
                                TextButton(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    onClick = {
                                        dialogLinkOpenState.value = true
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_learn_more)
                                    )
                                }
                            }
                        }
                    }
                    if (dialogLinkOpenState.value) {
                        AlertDialogLink(
                            onClose = { dialogLinkOpenState.value = false },
                            linkToOpen = BuildConfig.INSTALL_INSTRUCTIONS_LINK
                        )
                    }
                }
                largeSeparatorItem()
            }

            sectionHeaderItem(R.string.settings_location_section_general)
            listPreferenceItem(R.string.settings_location_service) { id ->
                ListPreferenceViewWithCard(
                    title = context.getString(id),
                    selectedKey = SettingsManager.getInstance(context).locationSource,
                    valueArray = locationSources.map { it.id }.toTypedArray(),
                    nameArray = locationSources.map { it.getName(context) }.toTypedArray(),
                    enableArray = locationSources.map {
                        it !is RemovedSource &&
                            (it !is ConfigurableSource || it.isConfigured) &&
                            (BuildConfig.FLAVOR != "freenet" || it !is NonFreeNetSource)
                    }.toTypedArray(),
                    summary = { _, value -> locationSources.firstOrNull { it.id == value }?.name },
                    isFirst = true,
                    isLast = true,
                    onValueChanged = { sourceId ->
                        SettingsManager.getInstance(context).locationSource = sourceId
                    }
                )
            }
            sectionFooterItem(R.string.settings_location_section_general)
            largeSeparatorItem()

            sectionHeaderItem(R.string.location_service_native)
            clickablePreferenceItem(R.string.settings_location_access_switch_title) { id ->
                PreferenceViewWithCard(
                    titleId = id,
                    summaryId = if (accessCoarseLocationPermissionState.status == PermissionStatus.Granted) {
                        R.string.settings_location_access_switch_summaryOn
                    } else {
                        R.string.settings_location_access_switch_summaryOff
                    },
                    isFirst = true,
                    isLast = accessBackgroundLocationPermissionState == null &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    onClick = {
                        if (accessCoarseLocationPermissionState.status != PermissionStatus.Granted) {
                            if (
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            ) {
                                accessCoarseLocationPermissionState.launchPermissionRequest()
                            } else {
                                context.openApplicationDetailsSettings()
                            }
                        } else {
                            SnackbarHelper.showSnackbar(
                                context.getString(R.string.settings_location_access_permission_already_granted)
                            )
                        }
                    }
                )
            }
            accessBackgroundLocationPermissionState?.let {
                smallSeparatorItem()
                clickablePreferenceItem(R.string.settings_location_access_background_title) { id ->
                    PreferenceViewWithCard(
                        titleId = id,
                        summaryId = if (it.status == PermissionStatus.Granted) {
                            R.string.settings_location_access_background_summaryOn
                        } else {
                            R.string.settings_location_access_background_summaryOff
                        },
                        enabled = accessCoarseLocationPermissionState.status == PermissionStatus.Granted,
                        isLast = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                        onClick = {
                            if (it.status != PermissionStatus.Granted) {
                                if (
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        context,
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                ) {
                                    it.launchPermissionRequest()
                                } else {
                                    context.openApplicationDetailsSettings()
                                }
                            } else {
                                SnackbarHelper.showSnackbar(
                                    context.getString(R.string.settings_location_access_permission_already_granted)
                                )
                            }
                        }
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smallSeparatorItem()
                clickablePreferenceItem(R.string.settings_location_access_precise_title) { id ->
                    PreferenceViewWithCard(
                        titleId = id,
                        summaryId = if (accessFineLocationPermissionState.status == PermissionStatus.Granted) {
                            R.string.settings_location_access_precise_summaryOn
                        } else {
                            R.string.settings_location_access_precise_summaryOff
                        },
                        enabled = accessCoarseLocationPermissionState.status == PermissionStatus.Granted,
                        isLast = true,
                        onClick = {
                            if (accessFineLocationPermissionState.status != PermissionStatus.Granted) {
                                if (
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                ) {
                                    accessFineLocationPermissionState.launchPermissionRequest()
                                } else {
                                    context.openApplicationDetailsSettings()
                                }
                            } else {
                                SnackbarHelper.showSnackbar(
                                    context.getString(R.string.settings_location_access_permission_already_granted)
                                )
                            }
                        }
                    )
                }
            }
            sectionFooterItem(R.string.location_service_native)
            item(key = "active_location_evidence") {
                ActiveLocationEvidence(context, detailedLocationProvider)
            }
            largeSeparatorItem()

            // TODO: Duplicate code from weather sources
            locationSources.filterIsInstance<ConfigurableSource>().forEach { preferenceSource ->
                largeSeparatorItem()
                item(key = "header_${preferenceSource.id}") {
                    SectionHeader(title = preferenceSource.name)
                }
                preferenceSource.getPreferences(context).forEachIndexed { index, preference ->
                    when (preference) {
                        is ListPreference -> {
                            listPreferenceItem(preference.titleId) { id ->
                                ListPreferenceView(
                                    titleId = id,
                                    selectedKey = preference.selectedKey,
                                    valueArrayId = preference.valueArrayId,
                                    nameArrayId = preference.nameArrayId,
                                    card = true,
                                    isFirst = index == 0,
                                    isLast = index == preferenceSource.getPreferences(context).lastIndex,
                                    onValueChanged = preference.onValueChanged
                                )
                            }
                        }

                        is EditTextPreference -> {
                            editTextPreferenceItem(preference.titleId) { id ->
                                EditTextPreferenceViewWithCard(
                                    titleId = id,
                                    summary = preference.summary,
                                    content = preference.content,
                                    placeholder = preference.placeholder,
                                    regex = preference.regex,
                                    regexError = preference.regexError,
                                    keyboardType = preference.keyboardType,
                                    isFirst = index == 0,
                                    isLast = index == preferenceSource.getPreferences(context).lastIndex,
                                    onValueChanged = preference.onValueChanged
                                )
                            }
                        }
                    }
                    if (index != preferenceSource.getPreferences(context).lastIndex) {
                        smallSeparatorItem()
                    }
                }
                item(key = "footer_${preferenceSource.id}") {
                    SectionFooter()
                }
            }

            bottomInsetItem()
        }
    }
}

@Composable
private fun ActiveLocationEvidence(
    context: Activity,
    detailedLocationProvider: DetailedLocationProvider,
) {
    var snapshot by remember { mutableStateOf(ActiveLocationController.getLatest(context)) }
    var address by remember { mutableStateOf<DetailedLocationAddress?>(null) }
    var addressFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = ActiveLocationController.getLatest(context)
            delay(1_000L)
        }
    }

    DisposableEffect(snapshot?.latitude, snapshot?.longitude) {
        address = null
        addressFailed = false
        val current = snapshot
        val disposable = if (current == null) {
            null
        } else {
            detailedLocationProvider.request(current.latitude, current.longitude)
                .subscribe(
                    { address = it },
                    { addressFailed = true },
                )
        }
        onDispose { disposable?.dispose() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.normal_margin))) {
            Text(stringResource(R.string.active_location_evidence_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            Text(stringResource(R.string.active_location_evidence_permissions, when {
                fine -> stringResource(R.string.active_location_evidence_permission_precise)
                coarse -> stringResource(R.string.active_location_evidence_permission_approximate)
                else -> stringResource(R.string.active_location_evidence_permission_missing)
            }))

            if (snapshot == null) {
                Text(stringResource(R.string.active_location_evidence_no_position))
            } else {
                val current = snapshot!!
                val ageSeconds = ((System.currentTimeMillis() - current.timestamp).coerceAtLeast(0L)) / 1_000L
                val stale = ageSeconds > 120L
                Text(stringResource(R.string.active_location_evidence_decimal, LocationHumanFormatter.decimalCoordinates(current.latitude, current.longitude)))
                Text(stringResource(R.string.active_location_evidence_dms, LocationHumanFormatter.dmsCoordinates(current.latitude, current.longitude)))
                Text(stringResource(R.string.active_location_evidence_horizontal, LocationHumanFormatter.accuracy(current.accuracy)))
                Text(stringResource(R.string.active_location_evidence_vertical, LocationHumanFormatter.accuracy(current.verticalAccuracyMeters)))
                Text(stringResource(R.string.active_location_evidence_altitude, LocationHumanFormatter.altitude(current.altitude)))
                Text(stringResource(R.string.active_location_evidence_measured_at, DateFormat.format("yyyy-MM-dd HH:mm:ss z", current.timestamp)))
                Text(stringResource(R.string.active_location_evidence_age, ageSeconds))
                if (stale) Text(stringResource(R.string.active_location_evidence_stale))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.active_location_evidence_address_title), style = MaterialTheme.typography.titleSmall)
                val lines = address?.completeAddressLines.orEmpty()
                if (lines.isEmpty()) {
                    Text(stringResource(if (addressFailed) R.string.active_location_evidence_address_unavailable else R.string.active_location_evidence_address_loading))
                } else {
                    lines.forEach { Text(it) }
                }
            }
        }
    }
}
