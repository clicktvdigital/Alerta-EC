package org.breezyweather.common.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.breezyweather.alertaec.info.OfficialInfoCache
import org.breezyweather.alertaec.info.OfficialInfoCacheFactory
import org.breezyweather.alertaec.info.OfficialInfoSelector
import org.breezyweather.ui.emergency.EmergencyCenterClock
import org.breezyweather.ui.emergency.EmergencyCenterReadDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OfficialInfoModule {
    @Provides
    @Singleton
    fun provideOfficialInfoCache(application: Application): OfficialInfoCache =
        OfficialInfoCacheFactory.create(application)

    @Provides
    @Singleton
    fun provideOfficialInfoSelector(cache: OfficialInfoCache): OfficialInfoSelector =
        OfficialInfoSelector(cache)

    @Provides
    @Singleton
    @EmergencyCenterReadDispatcher
    fun provideEmergencyCenterReadDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideEmergencyCenterClock(): EmergencyCenterClock =
        EmergencyCenterClock { System.currentTimeMillis() }
}
