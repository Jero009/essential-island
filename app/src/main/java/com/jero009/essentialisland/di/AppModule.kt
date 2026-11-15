/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.di

import android.content.Context
import com.jero009.essentialisland.data.INotificationHistoryRepository
import com.jero009.essentialisland.data.INotificationRepository
import com.jero009.essentialisland.data.NotificationHistoryRepository
import com.jero009.essentialisland.data.SmartIslandNotificationRepository
import com.jero009.essentialisland.data.SmartIslandSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SmartIslandSettingsRepository = SmartIslandSettingsRepository(context)

    @Provides
    @Singleton
    fun provideNotificationRepository(): INotificationRepository =
        SmartIslandNotificationRepository()

    @Provides
    @Singleton
    fun provideNotificationHistoryRepository(
        @ApplicationContext context: Context
    ): INotificationHistoryRepository = NotificationHistoryRepository(context)
}
