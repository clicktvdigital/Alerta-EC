package org.breezyweather.alertaec.info

import android.content.Context

/** Creates a cache only in app-private Android storage. */
object OfficialInfoCacheFactory {
    fun create(
        context: Context,
        limits: OfficialInfoCacheLimits = OfficialInfoCacheLimits()
    ): OfficialInfoCache = OfficialInfoCache(context.noBackupFilesDir, limits)
}
