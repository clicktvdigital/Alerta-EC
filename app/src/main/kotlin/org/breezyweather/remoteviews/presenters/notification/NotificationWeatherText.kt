package org.breezyweather.remoteviews.presenters.notification

internal object NotificationWeatherText {
    fun nonBlank(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    // Zero is valid at night; missing, negative and non-finite indices are not values.
    fun validUv(index: Double?): Boolean = index != null && index.isFinite() && index >= 0.0
}
