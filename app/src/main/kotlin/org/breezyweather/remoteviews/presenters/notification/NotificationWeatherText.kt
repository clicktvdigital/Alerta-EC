package org.breezyweather.remoteviews.presenters.notification

import kotlin.math.roundToInt

internal object NotificationWeatherText {
    fun nonBlank(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    // Whole numbers for temperatures and wind; accepts '.' or ',' decimals. Do not use for rain.
    fun wholeNumbers(text: String): String =
        Regex("\\d+[.,]\\d+").replace(text) { match ->
            match.value.replace(',', '.').toDouble().roundToInt().toString()
        }

    // Zero is valid at night; missing, negative and non-finite indices are not values.
    fun validUv(index: Double?): Boolean = index != null && index.isFinite() && index >= 0.0
}
